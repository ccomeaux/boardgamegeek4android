package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import androidx.palette.graphics.Palette
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.extensions.getHeaderSwatch
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.repository.GameCollectionRepository
import com.boardgamegeek.util.RemoteConfig
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class GameCollectionItemViewModel(application: Application) : AndroidViewModel(application) {
    private val gameCollectionRepository = GameCollectionRepository(getApplication())
    private val areItemsRefreshing = AtomicBoolean()
    private val isImageRefreshing = AtomicBoolean()
    private val forceRefresh = AtomicBoolean()
    private val refreshMinutes = RemoteConfig.getInt(RemoteConfig.KEY_REFRESH_GAME_COLLECTION_MINUTES)

    private val _collectionId = MutableLiveData<Int>()
    val collectionId: LiveData<Int>
        get() = _collectionId

    private val _isEditMode = MutableLiveData<Boolean>()
    val isEditMode: LiveData<Boolean>
        get() = _isEditMode
    private val _isEdited = MutableLiveData<Boolean>()
    val isEdited: LiveData<Boolean>
        get() = _isEdited

    init {
        _isEditMode.value = false
        _isEdited.value = false
    }

    fun setId(id: Int) {
        if (_collectionId.value != id) _collectionId.value = id
    }

    fun toggleEditMode() {
        if (item.value?.data != null)
            _isEditMode.value?.let { _isEditMode.value = !it }
    }

    val item: LiveData<RefreshableResource<CollectionItemEntity>> = _collectionId.switchMap { id ->
        liveData {
            try {
                latestValue?.data?.let { emit(RefreshableResource.refreshing(it)) }
                val item = gameCollectionRepository.loadCollectionItem(id)
                emit(RefreshableResource.success(item))
                val refreshedItem = if (areItemsRefreshing.compareAndSet(false, true)) {
                    item?.let {
                        val canRefresh = it.gameId != BggContract.INVALID_ID
                        val shouldRefresh = it.syncTimestamp.isOlderThan(refreshMinutes, TimeUnit.HOURS)
                        if (canRefresh && (shouldRefresh || forceRefresh.compareAndSet(true, false))) {
                            emit(RefreshableResource.refreshing(it))
                            gameCollectionRepository.refreshCollectionItem(it.gameId, id)
                            val loadedItem = gameCollectionRepository.loadCollectionItem(id)
                            emit(RefreshableResource.success(loadedItem))
                            loadedItem
                        } else item
                    }
                } else item
                if (isImageRefreshing.compareAndSet(false, true)) {
                    refreshedItem?.let {
                        gameCollectionRepository.refreshHeroImage(it)
                        emit(RefreshableResource.success(gameCollectionRepository.loadCollectionItem(id)))
                    }
                }
            } catch (e: Exception) {
                emit(RefreshableResource.error(e, application))
            } finally {
                areItemsRefreshing.set(false)
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

    private val _swatch = MutableLiveData<Palette.Swatch>()
    val swatch: LiveData<Palette.Swatch>
        get() = _swatch

    fun refresh() {
        _collectionId.value?.let { _collectionId.value = it }
    }

    fun updateGameColors(palette: Palette?) {
        palette?.let { _swatch.value = it.getHeaderSwatch() }
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
            setEdited(true)
            viewModelScope.launch {
                gameCollectionRepository.updatePrivateInfo(
                    item.value?.data?.internalId ?: BggContract.INVALID_ID.toLong(),
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
            setEdited(true)
            viewModelScope.launch {
                val internalId = item.value?.data?.internalId ?: BggContract.INVALID_ID.toLong()
                gameCollectionRepository.updateStatuses(internalId, statuses, wishlistPriority)
                refresh()
            }
        }
    }

    fun updateRating(rating: Double) {
        val currentRating = item.value?.data?.rating ?: 0.0
        if (rating != currentRating) {
            setEdited(true)
            viewModelScope.launch {
                val internalId = item.value?.data?.internalId ?: BggContract.INVALID_ID.toLong()
                gameCollectionRepository.updateRating(internalId, rating)
                refresh()
            }
        }
    }

    fun updateText(text: String, textColumn: String, timestampColumn: String, originalText: String? = null) {
        if (text != originalText) {
            setEdited(true)
            viewModelScope.launch {
                val internalId = item.value?.data?.internalId ?: BggContract.INVALID_ID.toLong()
                gameCollectionRepository.updateText(internalId, text, textColumn, timestampColumn)
                refresh()
            }
        }
    }

    fun delete() {
        setEdited(false)
        viewModelScope.launch {
            gameCollectionRepository.markAsDeleted(item.value?.data?.internalId ?: BggContract.INVALID_ID.toLong())
            refresh()
        }
    }

    fun reset() {
        setEdited(false)
        viewModelScope.launch {
            val internalId = item.value?.data?.internalId ?: BggContract.INVALID_ID.toLong()
            gameCollectionRepository.resetTimestamps(internalId)
            forceRefresh.set(true)
            refresh()
        }
    }

    private fun setEdited(edited: Boolean) {
        if (_isEdited.value != edited) _isEdited.value = edited
    }
}
