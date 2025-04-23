package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import androidx.work.WorkManager
import com.boardgamegeek.extensions.PREFERENCES_KEY_SYNC_STATUSES
import com.boardgamegeek.extensions.mapStatusToEnum
import com.boardgamegeek.livedata.Event
import com.boardgamegeek.livedata.EventLiveData
import com.boardgamegeek.livedata.LiveSharedPreference
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.CollectionStatus
import com.boardgamegeek.model.Game
import com.boardgamegeek.model.PlayUploadResult
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import com.boardgamegeek.repository.GameCollectionRepository
import com.boardgamegeek.repository.PlayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionDetailsViewModel @Inject constructor(
    application: Application,
    private val itemRepository: GameCollectionRepository,
    private val gameCollectionRepository: GameCollectionRepository,
    private val playRepository: PlayRepository,
) : AndroidViewModel(application) {
    val syncCollectionStatuses: LiveData<Set<CollectionStatus>?> =
        LiveSharedPreference<Set<String>>(getApplication(), PREFERENCES_KEY_SYNC_STATUSES).map { set ->
            set?.map { it.mapStatusToEnum() }?.toSet()
        }

    private val allItems: LiveData<List<CollectionItem>> =
        liveData {
            try {
                emitSource(itemRepository.loadAllAsFlow().asLiveData())
            } catch (e: Exception) {
                _errorMessage.setMessage(e.localizedMessage.ifEmpty { "Error loading collection" })
            }
        }

    private val allGames: LiveData<List<CollectionItem>> = allItems.map { list ->
        list.groupBy { item -> item.gameId }.mapValues { entry ->
            entry.value.fold(entry.value[0]) { acc, i ->
                acc.copy(
                    own = acc.own || i.own,
                    previouslyOwned = acc.previouslyOwned || i.previouslyOwned,
                    preOrdered = acc.preOrdered || i.preOrdered,
                    forTrade = acc.forTrade || i.forTrade,
                    wantToPlay = acc.wantToPlay || i.wantToPlay,
                    wantToBuy = acc.wantToBuy || i.wantToBuy,
                    wantInTrade = acc.wantInTrade || i.wantInTrade,
                    wishList = acc.wishList || i.wishList,
                    wishListPriority = minOf(acc.wishListPriority, i.wishListPriority),
                    rating = maxOf(acc.rating, i.rating),
                )
            }
        }.values.toList()
    }

    private val baseItems = allItems.map {
        it.filter { item ->
            item.subtype in listOf(Game.Subtype.BOARDGAME, null)
        }
    }

    private val ownedAndUnplayedItems = baseItems.map { list ->
        list.filter { item ->
            item.own && item.numberOfPlays == 0
        }
    }

    private val _errorMessage = EventLiveData()
    val errorMessage: LiveData<Event<String>>
        get() = _errorMessage

    private val _loggedPlayResult = MutableLiveData<Event<PlayUploadResult>>()
    val loggedPlayResult: LiveData<Event<PlayUploadResult>>
        get() = _loggedPlayResult

    val isRefreshing = WorkManager.getInstance(getApplication()).getWorkInfosForUniqueWorkLiveData(WORK_NAME).map { list ->
        list.any { workInfo -> !workInfo.state.isFinished }
    }

    private val _playerCount: MutableLiveData<Int?> = MutableLiveData()
    private val _playerCountType: MutableLiveData<PlayerCountType> = MutableLiveData()
    val playerCountType: LiveData<PlayerCountType>
        get() = _playerCountType

    private val _wantToPlayItems = MediatorLiveData<List<CollectionItem>>()
    val wantToPlayItems: LiveData<List<CollectionItem>>
        get() = _wantToPlayItems

    private val _recentlyPlayedGames = MediatorLiveData<List<CollectionItem>>()
    val recentlyPlayedGames: LiveData<List<CollectionItem>>
        get() = _recentlyPlayedGames

    private val _friendlessShouldPlayGames = MediatorLiveData<List<CollectionItem>>()
    val friendlessShouldPlayGames: LiveData<List<CollectionItem>>
        get() = _friendlessShouldPlayGames

    private val _shelfOfOpportunityItems = MediatorLiveData<List<CollectionItem>>()
    val shelfOfOpportunityItems: LiveData<List<CollectionItem>>
        get() = _shelfOfOpportunityItems

    private val _shelfOfNewOpportunityItems = MediatorLiveData<List<CollectionItem>>()
    val shelfOfNewOpportunityItems: LiveData<List<CollectionItem>>
        get() = _shelfOfNewOpportunityItems

    init {
        _wantToPlayItems.addSource(allItems) {
            createWantToPlay(items = it)
        }
        _wantToPlayItems.addSource(_playerCount) {
            createWantToPlay(playerCount = it)
        }
        _wantToPlayItems.addSource(_playerCountType) {
            createWantToPlay(playerCountType = it ?: PlayerCountType.All)
        }

        _recentlyPlayedGames.addSource(allGames) {
            createRecentlyPlayed(games = it)
        }
        _recentlyPlayedGames.addSource(_playerCount) {
            createRecentlyPlayed(playerCount = it)
        }
        _recentlyPlayedGames.addSource(_playerCountType) {
            createRecentlyPlayed(playerCountType = it ?: PlayerCountType.All)
        }

        _friendlessShouldPlayGames.addSource(allGames) {
            createFriendlessShouldPlay(games = it)
        }
        _friendlessShouldPlayGames.addSource(_playerCount) {
            createFriendlessShouldPlay(playerCount = it)
        }
        _friendlessShouldPlayGames.addSource(_playerCountType) {
            createFriendlessShouldPlay(playerCountType = it ?: PlayerCountType.All)
        }

        _shelfOfOpportunityItems.addSource(ownedAndUnplayedItems) {
            createShelfOfOpportunity(items = it)
        }
        _shelfOfOpportunityItems.addSource(_playerCount) {
            createShelfOfOpportunity(playerCount = it)
        }
        _shelfOfOpportunityItems.addSource(_playerCountType) {
            createShelfOfOpportunity(playerCountType = it ?: PlayerCountType.All)
        }

        _shelfOfNewOpportunityItems.addSource(ownedAndUnplayedItems) {
            createShelfOfNewOpportunity(items = it)
        }
        _shelfOfNewOpportunityItems.addSource(_playerCount) {
            createShelfOfNewOpportunity(playerCount = it)
        }
        _shelfOfNewOpportunityItems.addSource(_playerCountType) {
            createShelfOfNewOpportunity(playerCountType = it ?: PlayerCountType.All)
        }

        _playerCountType.value = PlayerCountType.All
    }

    // BROWSE

    val recentlyViewedItems: LiveData<List<CollectionItem>> = allItems.switchMap {
        liveData {
            emit(it.filter { it.lastViewedDate > 0L }
                .sortedByDescending { it.lastViewedDate }
                .take(ITEM_LIMIT))
        }
    }

    val highlyRatedItems: LiveData<List<CollectionItem>> = baseItems.switchMap { list ->
        liveData {
            emit(
                list.sortedWith(compareByDescending<CollectionItem> { it.rating }.thenBy { it.geekRating })
                    .take(ITEM_LIMIT)
            )
        }
    }

    val friendlessFavoriteItems: LiveData<List<CollectionItem>> = allItems.switchMap { list ->
        liveData {
            emit(
                list.sortedByDescending { it.friendlessFave }
                    .take(ITEM_LIMIT)
            )
        }
    }

    val underratedItems: LiveData<List<CollectionItem>> = allItems.switchMap {
        liveData {
            emit(
                it.filter { it.rating > 0.0 }
                    .sortedByDescending { it.zScore }
                    .take(ITEM_LIMIT)
            )
        }
    }

    @Suppress("SpellCheckingInspection")
    val hawtItems: LiveData<List<CollectionItem>> = allItems.switchMap { list ->
        liveData {
            emit(list.filter { it.gameId != UNPUBLISHED_PROTOTYPE_ID }
                .filter { it.own }
                .sortedByDescending { it.hawt }
                .take(ITEM_LIMIT)
            )
        }
    }

    val whyOwnItems: LiveData<List<CollectionItem>> = allItems.switchMap { list ->
        liveData {
            emit(list.filter { it.gameId != UNPUBLISHED_PROTOTYPE_ID }
                .filter { it.own && it. lastPlayDate != null && it.lastPlayDate > 0 }
                .sortedByDescending { it.friendlessWhyOwn() }
                .take(ITEM_LIMIT)
            )
        }
    }


    // ACQUIRE

    data class CollectionAcquireStats(
        val incomingCount: Int,
        val incomingRate: Double,
        val preorderedCount: Int,
        val wishlistCount: Int,
        val wantToBuyCount: Int,
        val wantInTradeCount: Int,
    )

    val collectionAcquireStats: LiveData<CollectionAcquireStats> = allItems.map { items ->
        val incomingCount = items.filter { it.isIncoming }.sumOf { it.quantity }
        CollectionAcquireStats(
            incomingCount,
            incomingCount.toDouble() / items.filter { it.own }.sumOf { it.quantity },
            items.filter { it.preOrdered }.sumOf { it.quantity },
            items.filter { (it.wishList && it.wishListPriority in 1..4 ) }.sumOf { it.quantity },
            items.filter { it.wantToBuy }.sumOf { it.quantity },
            items.filter { it.wantInTrade }.sumOf { it.quantity },
        )
    }

    val preordered: LiveData<List<CollectionItem>> = allItems.switchMap {
        liveData {
            val (withDate, withoutDate) = it.filter { it.preOrdered }.partition { it.acquisitionDate > 0L }
            emit(
                (withoutDate.sortedByDescending { it.geekRating } + withDate.sortedBy { it.acquisitionDate })
                    .take(ITEM_LIMIT)
            )
        }
    }

    val wishlist: LiveData<List<CollectionItem>> = allItems.switchMap {
        liveData {
            emit(
                it.filter { it.wishList }
                    .sortedWith(compareBy<CollectionItem> { it.wishListPriority }.thenByDescending { it.geekRating })
                    .take(ITEM_LIMIT)
            )
        }
    }

    val wantToBuy: LiveData<List<CollectionItem>> = allItems.switchMap {
        liveData {
            emit(
                it.filter { it.wantToBuy }
                    .sortedByDescending { it.geekRating }
                    .take(ITEM_LIMIT)
            )
        }
    }

    val wantInTrade: LiveData<List<CollectionItem>> = allItems.switchMap {
        liveData {
            emit(
                it.filter { it.wantInTrade }
                    .sortedByDescending { it.geekRating }
                    .take(ITEM_LIMIT)
            )
        }
    }

    val favoriteUnownedItems: LiveData<List<CollectionItem>> = allGames.switchMap {
        liveData {
            emit(
                // TODO figure out how to hide un-buy-able games like Celebrities
                it.filter { !it.own && it.rating > 0.0 && !it.isIncoming }
                    .sortedByDescending { it.rating }
                    .take(ITEM_LIMIT)
            )
        }
    }

    val playedButUnownedItems: LiveData<List<CollectionItem>> = allGames.switchMap { list ->
        liveData {
            emit(
                list.filter { !it.own && it.numberOfPlays > 0 && !it.isIncoming }
                    .sortedByDescending { it.numberOfPlays }
                    .take(ITEM_LIMIT)
            )
        }
    }

    @Suppress("SpellCheckingInspection")
    val hawtUnownedItems: LiveData<List<CollectionItem>> = allGames.switchMap { list ->
        liveData {
            emit(list.filter { it.gameId != UNPUBLISHED_PROTOTYPE_ID && !it.own && !it.isIncoming }
                .sortedByDescending { it.hawt }
                .take(ITEM_LIMIT)
            )
        }
    }

    // PLAY

    fun filterPlayerCount(playerCount: Int?) {
        _playerCount.postValue(playerCount)
    }

    fun filterPlayerCountType(playerCountType: PlayerCountType) {
        _playerCountType.postValue(playerCountType)
    }

    enum class PlayerCountType {
        All,
        Supports,
        GoodWith,
        BestWith,
    }

    private fun createWantToPlay(
        items: List<CollectionItem>? = allItems.value,
        playerCount: Int? = _playerCount.value,
        playerCountType: PlayerCountType = _playerCountType.value ?: PlayerCountType.All,
    ) {
        items?.let {
            _wantToPlayItems.postValue(items
                .filter { it.wantToPlay }
                .filterByPlayerCount(playerCount, playerCountType)
                .sortedByDescending { it.geekRating }
                .take(ITEM_LIMIT))
        }
    }

    private fun createRecentlyPlayed(
        games: List<CollectionItem>? = allGames.value,
        playerCount: Int? = _playerCount.value,
        playerCountType: PlayerCountType = _playerCountType.value ?: PlayerCountType.All,
    ) {
        games?.let {
            _recentlyPlayedGames.postValue(games
                .filter { it.lastPlayDate != null && it.lastPlayDate > 0L }
                .filterByPlayerCount(playerCount, playerCountType)
                .sortedByDescending { it.lastPlayDate }
                .take(ITEM_LIMIT))
        }
    }

    private fun createFriendlessShouldPlay(
        games: List<CollectionItem>? = allGames.value,
        playerCount: Int? = _playerCount.value,
        playerCountType: PlayerCountType = _playerCountType.value ?: PlayerCountType.All,
    ) {
        games?.let {
            _friendlessShouldPlayGames.postValue(games
                .filter { it.own && it.lastPlayDate != null && it.lastPlayDate > 0L }
                .filterByPlayerCount(playerCount, playerCountType)
                .sortedByDescending { it.friendlessShouldPlay }
                .take(ITEM_LIMIT))
        }
    }

    private fun createShelfOfOpportunity(
        items: List<CollectionItem>? = ownedAndUnplayedItems.value,
        playerCount: Int? = _playerCount.value,
        playerCountType: PlayerCountType = _playerCountType.value ?: PlayerCountType.All,
    ) {
        items?.let {
            _shelfOfOpportunityItems.postValue(items
                .filterByPlayerCount(playerCount, playerCountType)
                .sortedByDescending { it.geekRating }
                .take(ITEM_LIMIT))
        }
    }

    private fun createShelfOfNewOpportunity(
        items: List<CollectionItem>? = ownedAndUnplayedItems.value,
        playerCount: Int? = _playerCount.value,
        playerCountType: PlayerCountType = _playerCountType.value ?: PlayerCountType.All,
    ) {
        items?.let {
            _shelfOfNewOpportunityItems.postValue(items
                .filter { it.acquisitionDate > 0L } // TODO filter out shelf of opportunity games
                .filterByPlayerCount(playerCount, playerCountType)
                .sortedByDescending { it.acquisitionDate }
                .take(ITEM_LIMIT))
        }
    }

    private fun List<CollectionItem>.filterByPlayerCount(playerCount: Int?, playerCountType: PlayerCountType): List<CollectionItem> {
        return playerCount?.let {
            return when (playerCountType) {
                PlayerCountType.All -> this
                PlayerCountType.Supports -> filter { item -> item.minPlayerCount <= it && item.maxPlayerCount >= it }
                PlayerCountType.GoodWith -> filter { item -> item.recommendedPlayerCounts?.contains(it) == true }
                PlayerCountType.BestWith -> filter { item -> item.bestPlayerCounts?.contains(it) == true }
            }
        } ?: this
    }

    // ANALYZE

    val ratableItems: LiveData<List<CollectionItem>> = baseItems.switchMap { list ->
        liveData {
            emit(list
                .filter {
                    it.rating == 0.0 &&
                            it.numberOfPlays > 0 &&
                            it.gameId != UNPUBLISHED_PROTOTYPE_ID
                }
                .sortedWith(compareBy({ -it.numberOfPlays }, { it.numberOfUsersRating }))
                .take(ITEM_LIMIT)
            )
        }
    }

    val commentableItems: LiveData<List<CollectionItem>> = baseItems.switchMap { list ->
        liveData {
            emit(list
                .filter {
                    it.rating > 0.0 &&
                            it.numberOfPlays > 0 &&
                            it.gameId != UNPUBLISHED_PROTOTYPE_ID &&
                            it.comment.isBlank()
                }
                .sortedBy { it.numberOfUsersRating }
                .take(ITEM_LIMIT)
            )
        }
    }

    // Related data

    val acquiredFrom = liveData {
        emit(gameCollectionRepository.loadAcquiredFrom())
    }

    // Actions

    fun refresh() {
        if (isRefreshing.value == false) {
            gameCollectionRepository.enqueueRefreshRequest(WORK_NAME)
        }
    }

    fun updateRating(internalId: Long, rating: Double) {
        viewModelScope.launch {
            val gameId = itemRepository.loadCollectionItem(internalId)?.gameId
            if (gameId != null && gameId != INVALID_ID) {
                itemRepository.updateRating(internalId, rating)
                itemRepository.enqueueUploadRequest(gameId)
            }
        }
    }

    fun logQuickPlay(gameId: Int, gameName: String) {
        viewModelScope.launch {
            val result = playRepository.logQuickPlay(gameId, gameName)
            if (result.isFailure) {
                result.exceptionOrNull()?.let { _errorMessage.setMessage(it) }
            } else {
                result.getOrNull()?.let {
                    if (it.play.playId != INVALID_ID)
                        _loggedPlayResult.value = Event(it)
                }
            }
        }
    }

    fun updatePrivateInfo(
        internalId: Long,
        priceCurrency: String?,
        pricePaid: Double?,
        quantity: Int?,
        acquisitionDate: Long?,
        acquiredFrom: String?,
    ) {
        allItems.value?.find { it.internalId == internalId }?.let { originalItem ->
            viewModelScope.launch {
                gameCollectionRepository.updateStatus(
                    internalId,
                    statusOwn = true,
                    statusPreviouslyOwned = originalItem.previouslyOwned,
                    statusForTrade = originalItem.forTrade,
                    statusWantToPlay = originalItem.wantToPlay,
                )

                val itemModified = priceCurrency != originalItem.pricePaidCurrency ||
                        pricePaid != originalItem.pricePaid ||
                        quantity != originalItem.quantity ||
                        acquisitionDate != originalItem.acquisitionDate ||
                        acquiredFrom != originalItem.acquiredFrom

                if (itemModified) {
                    gameCollectionRepository.updatePrivateInfo(
                        internalId,
                        priceCurrency,
                        pricePaid,
                        originalItem.currentValueCurrency,
                        originalItem.currentValue,
                        quantity,
                        acquisitionDate,
                        acquiredFrom,
                        originalItem.inventoryLocation,
                    )
                }
                gameCollectionRepository.enqueueUploadRequest(originalItem.gameId)
            }
        }
    }

    companion object {
        const val ITEM_LIMIT = 5
        const val UNPUBLISHED_PROTOTYPE_ID = 18291
        const val WORK_NAME = "CollectionViewModel"
    }
}
