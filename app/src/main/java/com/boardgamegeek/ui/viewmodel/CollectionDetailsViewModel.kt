package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import androidx.work.WorkManager
import com.boardgamegeek.extensions.collectionStatusLiveData
import com.boardgamegeek.livedata.Event
import com.boardgamegeek.livedata.EventLiveData
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.CollectionItem.Companion.filterBaseGames
import com.boardgamegeek.model.CollectionItem.Companion.filterOwned
import com.boardgamegeek.model.CollectionItem.Companion.filterPlayed
import com.boardgamegeek.model.CollectionItem.Companion.filterPublishedGames
import com.boardgamegeek.model.CollectionItem.Companion.filterRated
import com.boardgamegeek.model.CollectionItem.Companion.filterUncommented
import com.boardgamegeek.model.CollectionItem.Companion.filterUnplayed
import com.boardgamegeek.model.CollectionItem.Companion.filterUnrated
import com.boardgamegeek.model.CollectionStatus
import com.boardgamegeek.model.Game
import com.boardgamegeek.model.PlayUploadResult
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import com.boardgamegeek.repository.GameCollectionRepository
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.repository.StatsHelper.Companion.calculateCorrelationCoefficient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionDetailsViewModel @Inject constructor(
    application: Application,
    private val gameCollectionRepository: GameCollectionRepository,
    private val playRepository: PlayRepository,
) : AndroidViewModel(application) {
    val syncCollectionStatuses = collectionStatusLiveData(getApplication())

    private val allItems: LiveData<List<CollectionItem>> =
        liveData {
            try {
                emitSource(gameCollectionRepository.loadAllAsFlow().asLiveData())
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

    private val _itemsFilteredByPlayerCount = MediatorLiveData<List<CollectionItem>>()

    init {
        _itemsFilteredByPlayerCount.addSource(allItems) {
            createFilteredByPlayerCountList(items = it)
        }
        _itemsFilteredByPlayerCount.addSource(_playerCount) {
            createFilteredByPlayerCountList(playerCount = it)
        }
        _itemsFilteredByPlayerCount.addSource(_playerCountType) {
            createFilteredByPlayerCountList(playerCountType = it ?: PlayerCountType.All)
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

    // OWN

    val own = allItems.switchMap {
        liveData {
            val filter = it.filter { it.own }
            emit(
                filter
                    .sortedWith(compareBy<CollectionItem> { it.rating }.thenByDescending { it.geekRating })
                    .take(ITEM_LIMIT)
                        to filter.size
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
            items.filter { (it.wishList && it.wishListPriority in 1..4) }.sumOf { it.quantity },
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

    // DIVEST

    private val forTradeItems = allItems.map { list -> list.filter { item -> item.forTrade } }

    val forTrade = forTradeItems.switchMap {
        liveData {
            emit(
                it
                    .sortedByDescending { it.numberOfUsersWanting }
                    .take(ITEM_LIMIT)
                        to it.size
            )
        }
    }

    val forTradeWithoutCondition = forTradeItems.switchMap {
        liveData {
            val filter = it.sortedByDescending { it.numberOfUsersWanting }.drop(ITEM_LIMIT).filter { it.conditionText.isBlank() }
            val size = it.filter { it.conditionText.isBlank() }.size
            emit(
                filter
                    .sortedByDescending { it.numberOfUsersWanting }
                    .take(ITEM_LIMIT)
                        to size
            )
        }
    }

    val previouslyOwned = allItems.switchMap {
        liveData {
            val filter = it.filter { it.previouslyOwned }
            emit(
                filter
                    .sortedByDescending { it.geekRating }
                    .take(ITEM_LIMIT)
                        to filter.size
            )
        }
    }

    val whyOwnItems = allItems.switchMap { list ->
        liveData {
            val filter = list
                .filter { it.own && !it.forTrade }
                .filter { it.own && !it.forTrade && it.friendlessWhyOwn() > 100.0 }
                .filterPublishedGames()
            emit(
                filter
                    .sortedByDescending { it.friendlessWhyOwn() }
                    .take(ITEM_LIMIT)
                        to filter.size
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

    val wantToPlayItems = _itemsFilteredByPlayerCount.switchMap {
        liveData {
            val list = it.filter { it.wantToPlay }
            emit(
                list.sortedByDescending { it.geekRating }.take(ITEM_LIMIT) to list.size
            )
        }
    }

    val recentlyPlayedGames = _itemsFilteredByPlayerCount.switchMap {
        liveData {
            val list = it.asSequence()
                .filterBaseGames()
                .filter { it.lastPlayDate != null && it.lastPlayDate > 0L }
                .sortedByDescending { it.lastPlayDate }
                .toList()
            emit(
                list.take(ITEM_LIMIT) to list.size
            )
        }
    }

    val friendlessShouldPlayGames = _itemsFilteredByPlayerCount.switchMap {
        liveData {
            val list = it.asSequence()
                .filterOwned()
                .filterBaseGames()
                .filter { it.friendlessShouldPlay > 10_000.0 }
                .toList()
            emit(
                list.sortedByDescending { it.friendlessShouldPlay }.take(ITEM_LIMIT) to list.size
            )
        }
    }

    val shelfOfOpportunityItems = _itemsFilteredByPlayerCount.switchMap {
        liveData {
            val list = it.asSequence()
                .filterOwned()
                .filterBaseGames()
                .filterUnplayed()
                .toList()
            emit(
                list.sortedByDescending { it.geekRating }.take(ITEM_LIMIT) to list.size
            )
        }
    }

    val shelfOfNewOpportunityItems = _itemsFilteredByPlayerCount.switchMap {
        liveData {
            val list = it.asSequence()
                .filterOwned()
                .filterBaseGames()
                .filterUnplayed()
                .filter { it.acquisitionDate > 0L }
                .toList()
            emit(
                list.sortedByDescending { it.acquisitionDate }.take(ITEM_LIMIT) to list.size
            )
        }
    }

    private fun createFilteredByPlayerCountList(
        items: List<CollectionItem>? = allItems.value,
        playerCount: Int? = _playerCount.value,
        playerCountType: PlayerCountType = _playerCountType.value ?: PlayerCountType.All,
    ) {
        items?.let {
            _itemsFilteredByPlayerCount.postValue(
                items.filterByPlayerCount(playerCount, playerCountType)
            )
        }
    }

    private fun List<CollectionItem>.filterByPlayerCount(playerCount: Int?, playerCountType: PlayerCountType): List<CollectionItem> {
        return playerCount?.let {
            return when (playerCountType) {
                PlayerCountType.All -> this
                PlayerCountType.Supports -> filter { item -> it in item.minPlayerCount..item.maxPlayerCount }
                PlayerCountType.GoodWith -> filter { item -> item.recommendedPlayerCounts?.contains(it) == true }
                PlayerCountType.BestWith -> filter { item -> item.bestPlayerCounts?.contains(it) == true }
            }
        } ?: this
    }

    // ANALYZE

    data class CollectionAnalyzeStats(
        val averagePersonalRating: Double,
        val averageAverageRating: Double,
        val correlationCoefficient: Double,
    )

    val collectionAnalyzeStats: LiveData<CollectionAnalyzeStats> = allItems.map { items ->
        CollectionAnalyzeStats(
            items.filter { it.rating > 0.0 }.map { it.rating }.average(),
            items.filter { it.averageRating > 0.0 }.map { it.averageRating }.average(),
            calculateCorrelationCoefficient(items.filter { it.rating > 0 && it.averageRating > 0 }.map { it.rating to it.averageRating }),
        )
    }

    val ratableItems = baseItems.switchMap { list ->
        liveData {
            val filter = list
                .filterUnrated()
                .filterPlayed()
                .filterPublishedGames()
            emit(
                filter
                    .sortedWith(compareBy({ -it.numberOfPlays }, { it.numberOfUsersRating }))
                    .take(ITEM_LIMIT) to filter.sumOf { it.quantity }
            )
        }
    }

    val commentableItems = baseItems.switchMap { list ->
        liveData {
            val filter = list
                .filterUncommented()
                .filterRated()
                .filterPlayed()
                .filterPublishedGames()
            emit(
                filter.sortedBy { it.numberOfUsersRating }.take(ITEM_LIMIT) to filter.sumOf { it.quantity }
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

    fun removeStatus(internalId: Long, status: CollectionStatus) {
        viewModelScope.launch {
            allItems.value?.find { it.internalId == internalId }?.let { originalItem ->
                val statuses = originalItem.statuses.copy().apply {
                    first.remove(status)
                }
                gameCollectionRepository.updateStatus(
                    internalId,
                    statuses.first,
                    statuses.second,
                )
                gameCollectionRepository.enqueueUploadRequest(originalItem.gameId)
            }
        }
    }

    fun markAsTraded(internalId: Long) {
        viewModelScope.launch {
            allItems.value?.find { it.internalId == internalId }?.let { originalItem ->
                val statuses = originalItem.statuses.copy().apply {
                    first.remove(CollectionStatus.ForTrade)
                    first.add(CollectionStatus.PreviouslyOwned)
                }
                gameCollectionRepository.updateStatus(
                    internalId,
                    statuses.first,
                    statuses.second,
                )
                gameCollectionRepository.updateCondition(internalId, "")
                gameCollectionRepository.enqueueUploadRequest(originalItem.gameId)
            }
        }
    }

    fun updateRating(internalId: Long, rating: Double) {
        viewModelScope.launch {
            val gameId = gameCollectionRepository.loadCollectionItem(internalId)?.gameId
            if (gameId != null && gameId != INVALID_ID) {
                gameCollectionRepository.updateRating(internalId, rating)
                gameCollectionRepository.enqueueUploadRequest(gameId)
            }
        }
    }

    fun updateComment(internalId: Long, comment: String) {
        viewModelScope.launch {
            val gameId = gameCollectionRepository.loadCollectionItem(internalId)?.gameId
            if (gameId != null && gameId != INVALID_ID) {
                gameCollectionRepository.updateComment(internalId, comment)
                gameCollectionRepository.enqueueUploadRequest(gameId)
            }
        }
    }

    fun updateCondition(internalId: Long, text: String) {
        viewModelScope.launch {
            allItems.value?.find { it.internalId == internalId }?.let { originalItem ->
                gameCollectionRepository.updateCondition(internalId, text)
                gameCollectionRepository.enqueueUploadRequest(originalItem.gameId)
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

    fun markedAsAcquired(
        internalId: Long,
        priceCurrency: String?,
        pricePaid: Double?,
        quantity: Int?,
        acquisitionDate: Long?,
        acquiredFrom: String?,
    ) {
        allItems.value?.find { it.internalId == internalId }?.let { originalItem ->
            viewModelScope.launch {
                val statuses = originalItem.statuses.copy().apply {
                    first.remove(CollectionStatus.WantInTrade)
                    first.remove(CollectionStatus.WantToBuy)
                    first.remove(CollectionStatus.Wishlist)
                    first.remove(CollectionStatus.Preordered)
                    first.add(CollectionStatus.Own)
                }
                gameCollectionRepository.updateStatus(
                    internalId,
                    statuses.first,
                    statuses.second,
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
