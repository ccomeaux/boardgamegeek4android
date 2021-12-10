package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import androidx.palette.graphics.Palette
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.extensions.getHeaderSwatch
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.provider.BggContract
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

    val isEdited = MutableLiveData<Boolean>()

    init {
        isEdited.value = false
    }

    fun setId(id: Int) {
        if (_collectionId.value != id) _collectionId.value = id
    }

    val item = _collectionId.switchMap { id ->
        liveData {
            try {
                val item = gameCollectionRepository.loadCollectionItem(id)
                val refreshedItem =
                    if (areItemsRefreshing.compareAndSet(false, true)) {
                        val gameId = item?.gameId ?: BggContract.INVALID_ID
                        val lastUpdated = item?.syncTimestamp ?: 0L
                        when {
                            lastUpdated.isOlderThan(refreshMinutes, TimeUnit.MINUTES) ||
                                    forceRefresh.compareAndSet(true, false) -> {
                                emit(RefreshableResource.refreshing(item))
                                (gameCollectionRepository.refreshCollectionItem(gameId, id) ?: item).also {
                                    forceRefresh.set(false)
                                }
                            }
                            else -> item
                        }.also { areItemsRefreshing.set(false) }
                    } else item
                val itemWithImage = if (isImageRefreshing.compareAndSet(false, true)) {
                    refreshedItem?.let {
                        gameCollectionRepository.refreshHeroImage(it)
                    }.also { isImageRefreshing.set(false) }
                } else refreshedItem
                emit(RefreshableResource.success(itemWithImage))
            } catch (e: Exception) {
                emit(RefreshableResource.error(e, application))
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
        price: Double?,
        currentValueCurrency: String?,
        currentValue: Double?,
        quantity: Int?,
        acquisitionDate: Long?,
        acquiredFrom: String?,
        inventoryLocation: String?
    ) {
        // TODO inspect to ensure something changed
        setEdited(true)
        viewModelScope.launch {
            gameCollectionRepository.updatePrivateInfo(
                item.value?.data?.internalId ?: BggContract.INVALID_ID.toLong(),
                priceCurrency,
                price,
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

    fun updateStatuses(statuses: List<String>, wishlistPriority: Int) {
        // TODO inspect to ensure something changed
        setEdited(true)
        viewModelScope.launch {
            val internalId = item.value?.data?.internalId ?: BggContract.INVALID_ID.toLong()
            gameCollectionRepository.updateStatuses(internalId, statuses, wishlistPriority)
            refresh()
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
        if (isEdited.value != edited) isEdited.value = edited
    }
}
