package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import androidx.palette.graphics.Palette
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.RefreshableResource
import com.boardgamegeek.extensions.getIconColor
import com.boardgamegeek.extensions.getImageId
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.repository.GameCollectionRepository
import com.boardgamegeek.util.RemoteConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

@HiltViewModel
class GameCollectionItemViewModel @Inject constructor(
    application: Application,
    private val gameCollectionRepository: GameCollectionRepository,
) : AndroidViewModel(application) {
    private val isItemRefreshing = AtomicBoolean()
    private val isImageRefreshing = AtomicBoolean()
    private val forceRefresh = AtomicBoolean()
    private val refreshMinutes = RemoteConfig.getInt(RemoteConfig.KEY_REFRESH_GAME_COLLECTION_MINUTES)

    private val _internalId = MutableLiveData<Long>()
    val internalId: LiveData<Long>
        get() = _internalId

    private val _isEditMode = MutableLiveData<Boolean>()
    val isEditMode: LiveData<Boolean>
        get() = _isEditMode.distinctUntilChanged()

    private val _isEdited = MutableLiveData<Boolean>()
    val isEdited: LiveData<Boolean>
        get() = _isEdited.distinctUntilChanged()

    init {
        _isEditMode.value = false
        _isEdited.value = false
    }

    fun setInternalId(id: Long) {
        if (_internalId.value != id) _internalId.value = id
    }

    fun enableEditMode() {
        _isEditMode.value = true
    }

    fun disableEditMode() {
        _isEditMode.value = false
    }

    val item: LiveData<RefreshableResource<CollectionItem>> = _internalId.switchMap { internalId ->
        liveData {
            try {
                latestValue?.data?.let { emit(RefreshableResource.refreshing(it)) }
                val item = gameCollectionRepository.loadCollectionItem(internalId)
                emit(RefreshableResource.success(item))
                item?.let {
                    if (it.isDirty)
                        gameCollectionRepository.enqueueUploadRequest(it.gameId)
                    if (isItemRefreshing.compareAndSet(false, true)) {
                        val refreshedItem = if (
                            forceRefresh.get() ||
                            it.syncTimestamp.isOlderThan(refreshMinutes.minutes)
                        ) {
                            emit(RefreshableResource.refreshing(it))
                            gameCollectionRepository.refreshCollectionItem(it.gameId, it.collectionId, it.subtype)
                            val loadedItem = gameCollectionRepository.loadCollectionItem(internalId)
                            emit(RefreshableResource.success(loadedItem))
                            loadedItem ?: it
                        } else it

                        if ((refreshedItem.heroImageUrl.isBlank() || refreshedItem.heroImageUrl.getImageId() != refreshedItem.thumbnailUrl.getImageId())
                            && isImageRefreshing.compareAndSet(false, true)
                        ) {
                            emit(RefreshableResource.refreshing(refreshedItem))
                            val itemWithImage = gameCollectionRepository.refreshHeroImage(refreshedItem)
                            emit(RefreshableResource.success(itemWithImage))
                            isImageRefreshing.set(false)
                        }
                        isItemRefreshing.set(false)
                        forceRefresh.set(false)
                    }
                }
            } catch (e: Exception) {
                emit(RefreshableResource.error(e, application))
                isItemRefreshing.set(false)
                isImageRefreshing.set(false)
            }
        }
    }

    val acquiredFrom = liveData {
        emit(gameCollectionRepository.loadAcquiredFrom())
    }

    val inventoryLocation = liveData {
        emit(gameCollectionRepository.loadInventoryLocation())
    }

    private val _iconColor = MutableLiveData<Int>()
    val iconColor: LiveData<Int>
        get() = _iconColor

    fun refresh() {
        _internalId.value?.let { _internalId.value = it }
    }

    fun updateGameColors(palette: Palette?) {
        palette?.let { _iconColor.value = it.getIconColor() }
    }

    fun updatePrivateInfo(
        priceCurrency: String?,
        pricePaid: Double?,
        currentValueCurrency: String?,
        currentValue: Double?,
        quantity: Int?,
        acquisitionDate: Long?,
        acquiredFrom: String?,
        inventoryLocation: String?
    ) {
        val itemModified = item.value?.data?.let {
            priceCurrency != it.pricePaidCurrency ||
                    pricePaid != it.pricePaid ||
                    currentValueCurrency != it.currentValueCurrency ||
                    currentValue != it.currentValue ||
                    quantity != it.quantity ||
                    acquisitionDate != it.acquisitionDate ||
                    acquiredFrom != it.acquiredFrom ||
                    inventoryLocation != it.inventoryLocation
        } ?: false
        if (itemModified) {
            _isEdited.value = true
            viewModelScope.launch {
                gameCollectionRepository.updatePrivateInfo(
                    internalId.value ?: BggContract.INVALID_ID.toLong(),
                    priceCurrency,
                    pricePaid,
                    currentValueCurrency,
                    currentValue,
                    quantity,
                    acquisitionDate,
                    acquiredFrom,
                    inventoryLocation
                )
                refresh()
            }
        }
    }

    fun updateStatuses(statuses: List<String>, wishlistPriority: Int) {
        val itemModified = item.value?.data?.let {
            statuses.contains(Collection.Columns.STATUS_OWN) != it.own ||
                    statuses.contains(Collection.Columns.STATUS_OWN) != it.own ||
                    statuses.contains(Collection.Columns.STATUS_PREVIOUSLY_OWNED) != it.previouslyOwned ||
                    statuses.contains(Collection.Columns.STATUS_PREORDERED) != it.preOrdered ||
                    statuses.contains(Collection.Columns.STATUS_FOR_TRADE) != it.forTrade ||
                    statuses.contains(Collection.Columns.STATUS_WANT) != it.wantInTrade ||
                    statuses.contains(Collection.Columns.STATUS_WANT_TO_BUY) != it.wantToBuy ||
                    statuses.contains(Collection.Columns.STATUS_WANT_TO_PLAY) != it.wantToPlay ||
                    statuses.contains(Collection.Columns.STATUS_WISHLIST) != it.wishList
        } ?: false
        if (itemModified) {
            _isEdited.value = true
            viewModelScope.launch {
                gameCollectionRepository.updateStatuses(internalId.value ?: BggContract.INVALID_ID.toLong(), statuses, wishlistPriority)
                refresh()
            }
        }
    }

    fun updateRating(rating: Double) {
        val currentRating = item.value?.data?.rating ?: 0.0
        if (rating != currentRating) {
            _isEdited.value = true
            viewModelScope.launch {
                gameCollectionRepository.updateRating(internalId.value ?: BggContract.INVALID_ID.toLong(), rating)
                refresh()
            }
        }
    }

    fun updateComment(text: String, originalText: String?) {
        updateText(text, originalText) { gameCollectionRepository.updateComment(internalId.value ?: BggContract.INVALID_ID.toLong(), it) }
    }

    fun updatePrivateComment(text: String, originalText: String?) {
        updateText(text, originalText) { gameCollectionRepository.updatePrivateComment(internalId.value ?: BggContract.INVALID_ID.toLong(), it) }
    }

    fun updateWishlistComment(text: String, originalText: String?) {
        updateText(text, originalText) { gameCollectionRepository.updateWishlistComment(internalId.value ?: BggContract.INVALID_ID.toLong(), it) }
    }

    fun updateCondition(text: String, originalText: String?) {
        updateText(text, originalText) { gameCollectionRepository.updateCondition(internalId.value ?: BggContract.INVALID_ID.toLong(), it) }
    }

    fun updateHasParts(text: String, originalText: String?) {
        updateText(text, originalText) { gameCollectionRepository.updateHasParts(internalId.value ?: BggContract.INVALID_ID.toLong(), it) }
    }

    fun updateWantParts(text: String, originalText: String?) {
        updateText(text, originalText) { gameCollectionRepository.updateWantParts(internalId.value ?: BggContract.INVALID_ID.toLong(), it) }
    }

    private fun updateText(text: String, originalText: String?, update: suspend (String) -> Unit) {
        if (text != originalText) {
            _isEdited.value = true
            viewModelScope.launch {
                update(text)
            }.invokeOnCompletion { refresh() }
        }
    }

    fun delete() {
        _isEdited.value = false
        viewModelScope.launch {
            if (gameCollectionRepository.markAsDeleted(internalId.value ?: BggContract.INVALID_ID.toLong()) > 0) {
                item.value?.data?.gameId?.let { gameCollectionRepository.enqueueUploadRequest(it) }
            }
        }
    }

    fun update() {
        _isEdited.value = false
        viewModelScope.launch {
            item.value?.data?.gameId?.let { gameCollectionRepository.enqueueUploadRequest(it) }
        }
    }


    fun reset() {
        _isEdited.value = false
        viewModelScope.launch {
            gameCollectionRepository.resetTimestamps(internalId.value ?: BggContract.INVALID_ID.toLong())
            forceRefresh.set(true)
            refresh()
        }
    }
}
