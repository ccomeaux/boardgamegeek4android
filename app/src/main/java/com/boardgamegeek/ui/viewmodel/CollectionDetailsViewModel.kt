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
import com.boardgamegeek.provider.BggContract.Collection
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

    private val _allItems: LiveData<List<CollectionItem>> =
        liveData {
            try {
                emitSource(itemRepository.loadAllAsFlow().asLiveData())
            } catch (e: Exception) {
                _errorMessage.setMessage(e.localizedMessage.ifEmpty { "Error loading collection" })
            }
        }

    private val _baseGames = _allItems.map {
        it.filter { item ->
            item.subtype == Game.Subtype.BOARDGAME || item.subtype == null
        }
    }

    private val ownedAndUnplayedGames = _baseGames.map { list ->
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

    val recentlyViewedItems: LiveData<List<CollectionItem>> = _allItems.switchMap {
        liveData {
            emit(it.filter { it.lastViewedDate > 0L }
                .sortedByDescending { it.lastViewedDate }
                .take(ITEM_LIMIT))
        }
    }

    val highlyRatedItems: LiveData<List<CollectionItem>> = _allItems.switchMap { list ->
        liveData {
            emit(
                list.filter { it.subtype == Game.Subtype.BOARDGAME || it.subtype == null }
                    .sortedWith(compareByDescending<CollectionItem> { it.rating }.thenBy { it.averageRating })
                    .take(ITEM_LIMIT)
            )
        }
    }

    val underratedItems: LiveData<List<CollectionItem>> = _allItems.switchMap {
        liveData {
            emit(
                it.filter { it.rating > 0.0 }
                    .sortedByDescending { it.ratingDelta }
                    .take(ITEM_LIMIT)
            )
        }
    }

    @Suppress("SpellCheckingInspection")
    val hawtItems: LiveData<List<CollectionItem>> = _allItems.switchMap { list ->
        liveData {
            emit(list.filter { it.gameId != UNPUBLISHED_PROTOTYPE_ID }
                .filter { it.own }
                .sortedByDescending { 1 + (it.numberOfUsersWanting + it.numberOfUsersWishing) / (2 + it.numberOfUsersOwned / 500) }
                .take(ITEM_LIMIT)
            )
        }
    }

    // Acquire

    val preordered: LiveData<List<CollectionItem>> = _allItems.switchMap {
        liveData {
            val (withDate, withoutDate) = it.filter { it.preOrdered }.partition { it.acquisitionDate > 0L }
            emit(
                (withDate.sortedBy { it.acquisitionDate } + withoutDate.sortedByDescending { it.averageRating })
                    .take(ITEM_LIMIT)
            )
        }
    }

    val wishlist: LiveData<List<CollectionItem>> = _allItems.switchMap {
        liveData {
            emit(
                it.filter { it.wishList }
                    .sortedWith(compareBy<CollectionItem> { it.wishListPriority }.thenByDescending { it.geekRating }) // TODO geek or average?
                    .take(ITEM_LIMIT)
            )
        }
    }

    val wantToBuy: LiveData<List<CollectionItem>> = _allItems.switchMap {
        liveData {
            emit(
                it.filter { it.wantToBuy }
                    .sortedByDescending { it.geekRating }
                    .take(ITEM_LIMIT)
            )
        }
    }

    val wantInTrade: LiveData<List<CollectionItem>> = _allItems.switchMap {
        liveData {
            emit(
                it.filter { it.wantInTrade }
                    .sortedByDescending { it.geekRating }
                    .take(ITEM_LIMIT)
            )
        }
    }

    val favoriteUnownedItems: LiveData<List<CollectionItem>> = _allItems.switchMap {
        liveData {
            emit(
                // TODO group by game ID and OR the statuses (otherwise, Through the Ages will show up)
                // Not owned will include games with multiple editions
                // TODO figure out how to hide un-buy-able games like Celebrities
                it.filter { !it.own && it.rating > 0.0 }
                    .filter { (it.wishList && it.wishListPriority < 5) || !it.wishList }
                    .sortedByDescending { it.rating }
                    .take(ITEM_LIMIT)
            )
        }
    }

    val playedButUnownedItems: LiveData<List<CollectionItem>> = _allItems.switchMap { list ->
        liveData {
            emit(
                list.filter { !it.own && it.numberOfPlays > 0 } // Not owned will include games with multiple editions
                    .filter { (it.wishList && it.wishListPriority < 4) || !it.wishList }
                    .sortedByDescending { it.numberOfPlays }
                    .take(ITEM_LIMIT)
            )
        }
    }

    @Suppress("SpellCheckingInspection")
    val hawtUnownedItems: LiveData<List<CollectionItem>> = _allItems.switchMap { list ->
        liveData {
            emit(list.filter { it.gameId != UNPUBLISHED_PROTOTYPE_ID }
                .filter { it.wantToBuy }
                .sortedByDescending { 1 + (it.numberOfUsersWanting + it.numberOfUsersWishing) / (2 + it.numberOfUsersOwned / 500) }
                .take(ITEM_LIMIT)
            )
        }
    }

    // Play

    val wantToPlayItems: LiveData<List<CollectionItem>> = _allItems.switchMap {
        liveData {
            emit(
                it.filter { it.wantToPlay }
                    .sortedByDescending { it.geekRating }
                    .take(ITEM_LIMIT)
            )
        }
    }

    val recentlyPlayedItems: LiveData<List<CollectionItem>> = _allItems.switchMap {
        liveData {
            emit(
                it.filter { it.lastPlayDate != null && it.lastPlayDate > 0L }
                    .sortedByDescending { it.lastPlayDate } // TODO Would like to sort by play ID descending as well
                    .take(ITEM_LIMIT)
            )
        }
    }

    val shelfOfOpportunityItems: LiveData<List<CollectionItem>> = ownedAndUnplayedGames.switchMap { list ->
        liveData {
            emit(
                list
                    .sortedByDescending { it.geekRating }
                    .take(ITEM_LIMIT)
            )
        }
    }

    val shelfOfNewOpportunityItems: LiveData<List<CollectionItem>> = ownedAndUnplayedGames.switchMap { list ->
        liveData {
            emit(
                list.filter { it.acquisitionDate > 0L } // TODO filter out shelf of opportunity games
                    .sortedByDescending { it.acquisitionDate }
                    .take(ITEM_LIMIT)
            )
        }
    }

//    val grayItems: LiveData<List<CollectionItem>> = _allItems.switchMap { list ->
//        liveData {
//            emit(
//                list.filter { (it.subtype == null || it.subtype == Game.Subtype.BOARDGAME) && it.rating > 0.0 }
//                    .sortedByDescending { it.zefquaaviusScore() }
//            )
//        }
//    }

    // Analyze

    val ratableItems: LiveData<List<CollectionItem>> = _baseGames.switchMap { list ->
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

    val commentableItems: LiveData<List<CollectionItem>> = _baseGames.switchMap { list ->
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
        _allItems.value?.find { it.internalId == internalId }?.let { originalItem ->
            viewModelScope.launch {
                // disable wishlist, want in trade, want to buy, and preordered; enable own
                val statuses = mutableListOf(Collection.Columns.STATUS_OWN)
                if (originalItem.previouslyOwned) statuses.add(Collection.Columns.STATUS_PREVIOUSLY_OWNED)
                if (originalItem.forTrade) statuses.add(Collection.Columns.STATUS_FOR_TRADE)
                if (originalItem.wantToPlay) statuses.add(Collection.Columns.STATUS_WANT_TO_PLAY)
                gameCollectionRepository.updateStatuses(internalId, statuses, originalItem.wishListPriority) // TODO set this to WISHLIST_PRIORITY_UNKNOWN?

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
