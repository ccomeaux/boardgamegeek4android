package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.core.content.contentValuesOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.palette.graphics.Palette
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.livedata.AbsentLiveData
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.GameCollectionRepository
import com.boardgamegeek.util.PaletteUtils

class GameCollectionItemViewModel(application: Application) : AndroidViewModel(application) {
    private val gameCollectionRepository = GameCollectionRepository(getApplication())

    private val _collectionId = MutableLiveData<Int>()
    val collectionId: LiveData<Int>
        get() = _collectionId

    val isEdited = MutableLiveData<Boolean>()

    init {
        isEdited.value = false
    }

    fun setId(id: Int?) {
        if (_collectionId.value != id) _collectionId.value = id
    }

    val item: LiveData<RefreshableResource<CollectionItemEntity>> = Transformations.switchMap(_collectionId) { id ->
        when (id) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> gameCollectionRepository.getCollectionItem(id)
        }
    }

    private val _swatch = MutableLiveData<Palette.Swatch>()
    val swatch: LiveData<Palette.Swatch>
        get() = _swatch

    fun refresh() {
        // TODO test if we're already refreshing?
        _collectionId.value?.let { _collectionId.value = it }
    }

    fun updateGameColors(palette: Palette?) {
        if (palette != null) {
            _swatch.value = PaletteUtils.getHeaderSwatch(palette)
        }
    }

    fun updatePrivateInfo(priceCurrency: String?, price: Double?, currentValueCurrency: String?, currentValue: Double?, quantity: Int?, acquisitionDate: String?, acquiredFrom: String?, inventoryLocation: String?) {
        setEdited(true)
        val internalId = item.value?.data?.internalId ?: BggContract.INVALID_ID.toLong()

        // TODO inspect to ensure something changed
        val values = contentValuesOf(
                BggContract.Collection.PRIVATE_INFO_DIRTY_TIMESTAMP to System.currentTimeMillis(),
                BggContract.Collection.PRIVATE_INFO_PRICE_PAID_CURRENCY to priceCurrency,
                BggContract.Collection.PRIVATE_INFO_PRICE_PAID to price,
                BggContract.Collection.PRIVATE_INFO_CURRENT_VALUE_CURRENCY to currentValueCurrency,
                BggContract.Collection.PRIVATE_INFO_CURRENT_VALUE to currentValue,
                BggContract.Collection.PRIVATE_INFO_QUANTITY to quantity,
                BggContract.Collection.PRIVATE_INFO_ACQUISITION_DATE to acquisitionDate,
                BggContract.Collection.PRIVATE_INFO_ACQUIRED_FROM to acquiredFrom,
                BggContract.Collection.PRIVATE_INFO_INVENTORY_LOCATION to inventoryLocation
        )
        gameCollectionRepository.update(internalId, values)
    }

    fun updateStatuses(statuses: List<String>, wishlistPriority: Int) {
        setEdited(true)
        val internalId = item.value?.data?.internalId ?: BggContract.INVALID_ID.toLong()
        // TODO inspect to ensure something changed
        val values = contentValuesOf(
                BggContract.Collection.STATUS_DIRTY_TIMESTAMP to System.currentTimeMillis(),
                BggContract.Collection.STATUS_OWN to statuses.contains(BggContract.Collection.STATUS_OWN),
                BggContract.Collection.STATUS_PREVIOUSLY_OWNED to statuses.contains(BggContract.Collection.STATUS_PREVIOUSLY_OWNED),
                BggContract.Collection.STATUS_PREORDERED to statuses.contains(BggContract.Collection.STATUS_PREORDERED),
                BggContract.Collection.STATUS_FOR_TRADE to statuses.contains(BggContract.Collection.STATUS_FOR_TRADE),
                BggContract.Collection.STATUS_WANT to statuses.contains(BggContract.Collection.STATUS_WANT),
                BggContract.Collection.STATUS_WANT_TO_BUY to statuses.contains(BggContract.Collection.STATUS_WANT_TO_BUY),
                BggContract.Collection.STATUS_WANT_TO_PLAY to statuses.contains(BggContract.Collection.STATUS_WANT_TO_PLAY),
                BggContract.Collection.STATUS_WISHLIST to statuses.contains(BggContract.Collection.STATUS_WISHLIST)
        )
        if (statuses.contains(BggContract.Collection.STATUS_WISHLIST)) {
            values.put(BggContract.Collection.STATUS_WISHLIST_PRIORITY, wishlistPriority.coerceIn(1..5))
        }
        gameCollectionRepository.update(internalId, values)
    }

    fun updateRating(rating: Double) {
        val internalId = item.value?.data?.internalId ?: BggContract.INVALID_ID.toLong()
        val currentRating = item.value?.data?.rating ?: 0.0
        if (rating != currentRating) {
            setEdited(true)
            val values = contentValuesOf(
                    BggContract.Collection.RATING to rating,
                    BggContract.Collection.RATING_DIRTY_TIMESTAMP to System.currentTimeMillis()
            )
            gameCollectionRepository.update(internalId, values)
        }
    }

    fun updateText(text: String, textColumn: String, timestampColumn: String, originalText: String? = null) {
        if (text != originalText) {
            setEdited(true)
            val internalId = item.value?.data?.internalId ?: BggContract.INVALID_ID.toLong()
            val values = contentValuesOf(
                    textColumn to text,
                    timestampColumn to System.currentTimeMillis()
            )
            gameCollectionRepository.update(internalId, values)
        }
    }

    fun delete() {
        setEdited(false)
        val internalId = item.value?.data?.internalId ?: BggContract.INVALID_ID.toLong()
        val values = contentValuesOf(BggContract.Collection.COLLECTION_DELETE_TIMESTAMP to System.currentTimeMillis())
        gameCollectionRepository.update(internalId, values)
    }

    fun reset() {
        setEdited(false)
        val internalId = item.value?.data?.internalId ?: BggContract.INVALID_ID.toLong()
        val gameId = item.value?.data?.gameId ?: BggContract.INVALID_ID
        gameCollectionRepository.resetTimestamps(internalId, gameId)
    }

    private fun setEdited(edited: Boolean) {
        if (isEdited.value != edited) isEdited.value = edited
    }
}