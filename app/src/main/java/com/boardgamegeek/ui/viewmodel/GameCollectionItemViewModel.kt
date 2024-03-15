package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import androidx.palette.graphics.Palette
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.extensions.getIconColor
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.livedata.Event
import com.boardgamegeek.livedata.EventLiveData
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.repository.GameCollectionRepository
import com.boardgamegeek.util.RemoteConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

@HiltViewModel
class GameCollectionItemViewModel @Inject constructor(
    application: Application,
    private val gameCollectionRepository: GameCollectionRepository,
) : AndroidViewModel(application) {
    private val isItemRefreshing = AtomicBoolean(false)
    private val isImageRefreshing = AtomicBoolean(false)
    private val forceRefresh = AtomicBoolean(false)
    private val refreshMinutes = RemoteConfig.getInt(RemoteConfig.KEY_REFRESH_GAME_COLLECTION_MINUTES)

    private val _internalId = MutableLiveData<Long>()

    private val _isEditMode = MutableLiveData<Boolean>()
    val isEditMode: LiveData<Boolean>
        get() = _isEditMode.distinctUntilChanged()

    private val _isEdited = MutableLiveData<Boolean>()
    val isEdited: LiveData<Boolean>
        get() = _isEdited.distinctUntilChanged()

    private val _isItemRefreshing = MutableLiveData<Boolean>()
    private val _isImageRefreshing = MutableLiveData<Boolean>()
    val isRefreshing = MediatorLiveData<Boolean>()

    private val _error = EventLiveData()
    val error: LiveData<Event<String>>
        get() = _error

    init {
        _isEditMode.value = false
        _isEdited.value = false
        isRefreshing.addSource(_isItemRefreshing) { isRefreshing.value = (it || _isImageRefreshing.value ?: false) }
        isRefreshing.addSource(_isImageRefreshing) { isRefreshing.value = (it || _isItemRefreshing.value ?: false) }
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

    val item: LiveData<CollectionItem?> = _internalId.switchMap { internalId ->
        liveData {
            val flow = gameCollectionRepository.loadCollectionItemFlow(internalId)
            emitSource(flow.distinctUntilChanged()
                .onEach {
                    if (latestValue == null) attemptUpload()
                    attemptRefresh(it)
                    refreshImage()
                }
                .asLiveData()
            )
        }
    }

    fun refresh() {
        forceRefresh.set(true)
        attemptRefresh(item.value)
    }

    private fun attemptRefresh(collectionItem: CollectionItem?) {
        viewModelScope.launch {
            collectionItem?.let {
                if (forceRefresh.get() || it.syncTimestamp.isOlderThan(refreshMinutes.minutes)) {
                    if (isItemRefreshing.compareAndSet(false, true)) {
                        try {
                            _isItemRefreshing.value = true
                            gameCollectionRepository.refreshCollectionItem(it.gameId, it.collectionId, it.subtype)?.let { errorMessage ->
                                _error.setMessage(errorMessage)
                            }
                        } catch (e: Exception) {
                            _error.setMessage(e)
                        } finally {
                            isItemRefreshing.set(false)
                            _isItemRefreshing.value = false
                        }
                        forceRefresh.set(false)
                    }
                }
            }
        }
    }

    private fun refreshImage() {
        viewModelScope.launch {
            item.value?.let {
                if (it.doesHeroImageNeedUpdating()) {
                    if (isImageRefreshing.compareAndSet(false, true)) {
                        try {
                            _isImageRefreshing.value = true
                            gameCollectionRepository.refreshHeroImage(it)
                        } finally {
                            isImageRefreshing.set(false)
                            _isImageRefreshing.value = false
                        }
                    }
                }
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
        val itemModified = item.value?.let {
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
                    getInternalId(),
                    priceCurrency,
                    pricePaid,
                    currentValueCurrency,
                    currentValue,
                    quantity,
                    acquisitionDate,
                    acquiredFrom,
                    inventoryLocation
                )
            }
        }
    }

    fun updateStatuses(statuses: List<String>, wishlistPriority: Int) {
        val itemModified = item.value?.let {
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
                gameCollectionRepository.updateStatuses(getInternalId(), statuses, wishlistPriority)
            }
        }
    }

    fun updateRating(rating: Double) {
        val currentRating = item.value?.rating ?: 0.0
        if (rating != currentRating) {
            _isEdited.value = true
            viewModelScope.launch {
                gameCollectionRepository.updateRating(getInternalId(), rating)
            }
        }
    }

    fun updateComment(text: String, originalText: String?) {
        updateText(text, originalText) { gameCollectionRepository.updateComment(getInternalId(), it) }
    }

    fun updatePrivateComment(text: String, originalText: String?) {
        updateText(text, originalText) { gameCollectionRepository.updatePrivateComment(getInternalId(), it) }
    }

    fun updateWishlistComment(text: String, originalText: String?) {
        updateText(text, originalText) { gameCollectionRepository.updateWishlistComment(getInternalId(), it) }
    }

    fun updateCondition(text: String, originalText: String?) {
        updateText(text, originalText) { gameCollectionRepository.updateCondition(getInternalId(), it) }
    }

    fun updateHasParts(text: String, originalText: String?) {
        updateText(text, originalText) { gameCollectionRepository.updateHasParts(getInternalId(), it) }
    }

    fun updateWantParts(text: String, originalText: String?) {
        updateText(text, originalText) { gameCollectionRepository.updateWantParts(getInternalId(), it) }
    }

    private fun updateText(text: String, originalText: String?, update: suspend (String) -> Unit) {
        if (text != originalText) {
            _isEdited.value = true
            viewModelScope.launch {
                update(text)
            }
        }
    }

    fun delete() {
        _isEdited.value = false
        viewModelScope.launch {
            if (gameCollectionRepository.markAsDeleted(getInternalId()) > 0) {
                item.value?.gameId?.let { gameCollectionRepository.enqueueUploadRequest(it) }
            }
        }
    }

    fun update() {
        _isEditMode.value = false
        attemptUpload()
    }

    private fun attemptUpload() {
        item.value?.let {
            if (it.isDirty && _isEditMode.value == false && _isEdited.value == true) {
                gameCollectionRepository.enqueueUploadRequest(it.gameId)
                _isEdited.value = false
            }
        }
    }

    fun reset() {
        _isEdited.value = false
        viewModelScope.launch {
            gameCollectionRepository.resetTimestamps(getInternalId())
            refresh()
        }
    }

    private fun getInternalId() = _internalId.value ?: BggContract.INVALID_ID.toLong()
}
