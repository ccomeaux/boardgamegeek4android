package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import androidx.work.WorkManager
import com.boardgamegeek.extensions.PREFERENCES_KEY_SYNC_STATUSES
import com.boardgamegeek.extensions.howManyDaysOld
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
import kotlin.math.pow

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
            item.subtype == Game.Subtype.BOARDGAME || item.subtype == null
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
                list.sortedByDescending { it.rating * 5 + it.numberOfPlays } // number of months played * 4 + hours played
                    .take(ITEM_LIMIT)
            )
        }
    }

    val underratedItems: LiveData<List<CollectionItem>> = allItems.switchMap {
        liveData {
            emit(
                it.filter { it.rating > 0.0 }
                    .sortedByDescending { it.ratingDelta }
                    .take(ITEM_LIMIT)
            )
        }
    }

    @Suppress("SpellCheckingInspection")
    val hawtItems: LiveData<List<CollectionItem>> = allItems.switchMap { list ->
        liveData {
            emit(list.filter { it.gameId != UNPUBLISHED_PROTOTYPE_ID }
                .filter { it.own }
                .sortedByDescending { 1 + (it.numberOfUsersWanting + it.numberOfUsersWishing) / (2 + it.numberOfUsersOwned / 500) }
                .take(ITEM_LIMIT)
            )
        }
    }

    // ACQUIRE

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
                it.filter { !it.own && it.rating > 0.0 }
                    .filter { (it.wishList && it.wishListPriority < 5) || !it.wishList }
                    .sortedByDescending { it.rating }
                    .take(ITEM_LIMIT)
            )
        }
    }

    val playedButUnownedItems: LiveData<List<CollectionItem>> = allGames.switchMap { list ->
        liveData {
            emit(
                list.filter { !it.own && it.numberOfPlays > 0 }
                    .filter { (it.wishList && it.wishListPriority < 4) || !it.wishList }
                    .sortedByDescending { it.numberOfPlays }
                    .take(ITEM_LIMIT)
            )
        }
    }

    @Suppress("SpellCheckingInspection")
    val hawtUnownedItems: LiveData<List<CollectionItem>> = allGames.switchMap { list ->
        liveData {
            emit(list.filter { it.gameId != UNPUBLISHED_PROTOTYPE_ID }
                .filter { !it.own && !it.preOrdered }
                .sortedByDescending { 1 + (it.numberOfUsersWanting + it.numberOfUsersWishing) / (2 + it.numberOfUsersOwned / 500) }
                .take(ITEM_LIMIT)
            )
        }
    }

    // PLAY

    val wantToPlayItems: LiveData<List<CollectionItem>> = allItems.switchMap {
        liveData {
            emit(
                it.filter { it.wantToPlay }
                    .sortedByDescending { it.geekRating }
                    .take(ITEM_LIMIT)
            )
        }
    }

    val recentlyPlayedItems: LiveData<List<CollectionItem>> = allGames.switchMap {
        liveData {
            emit(
                it.filter { it.lastPlayDate != null && it.lastPlayDate > 0L }
                    .sortedByDescending { it.lastPlayDate } // TODO Would like to sort by play ID descending as well
                    .take(ITEM_LIMIT)
            )
        }
    }

    val friendlessShouldPlayGames: LiveData<List<CollectionItem>> = allGames.switchMap {
        liveData {
            emit(
                it.filter { it.own && it.lastPlayDate != null && it.lastPlayDate > 0L }
                    .sortedByDescending { it.rating.pow(4) + it.lastPlayDate!!.howManyDaysOld() }
                    .take(ITEM_LIMIT)
            )
        }
    }

    val shelfOfOpportunityItems: LiveData<List<CollectionItem>> = ownedAndUnplayedItems.switchMap { list ->
        liveData {
            emit(
                list
                    .sortedByDescending { it.geekRating }
                    .take(ITEM_LIMIT)
            )
        }
    }

    val shelfOfNewOpportunityItems: LiveData<List<CollectionItem>> = ownedAndUnplayedItems.switchMap { list ->
        liveData {
            emit(
                list.filter { it.acquisitionDate > 0L } // TODO filter out shelf of opportunity games
                    .sortedByDescending { it.acquisitionDate }
                    .take(ITEM_LIMIT)
            )
        }
    }

//    val grayItems: LiveData<List<CollectionItem>> = _baseGames.switchMap { list ->
//        liveData {
//            emit(
//                list.filter { it.rating > 0.0 }
//                    .sortedByDescending { it.zefquaaviusScore() }
//            )
//        }
//    }

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
