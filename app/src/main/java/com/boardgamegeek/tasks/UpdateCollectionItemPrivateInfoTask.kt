package com.boardgamegeek.tasks

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.ui.model.PrivateInfo
import timber.log.Timber

class UpdateCollectionItemPrivateInfoTask(context: Context?, gameId: Int, collectionId: Int, internalId: Long, private val privateInfo: PrivateInfo) : UpdateCollectionItemTask(context, gameId, collectionId, internalId) {
    override fun updateResolver(resolver: ContentResolver, internalId: Long): Boolean {
        val item = Item.fromResolver(resolver, internalId) ?: return false
        val values = updateValues(item)
        if (values.size() > 0) {
            values.put(Collection.PRIVATE_INFO_DIRTY_TIMESTAMP, System.currentTimeMillis())
            resolver.update(Collection.buildUri(internalId), values, null, null)
            return true
        }
        return false
    }

    private fun updateValues(item: Item): ContentValues {
        val values = ContentValues(9)
        putString(values, Collection.PRIVATE_INFO_PRICE_PAID_CURRENCY, privateInfo.priceCurrency, item.priceCurrency)
        putDouble(values, Collection.PRIVATE_INFO_PRICE_PAID, privateInfo.price, item.price)
        putString(values, Collection.PRIVATE_INFO_CURRENT_VALUE_CURRENCY, privateInfo.currentValueCurrency, item.currentValueCurrency)
        putDouble(values, Collection.PRIVATE_INFO_CURRENT_VALUE, privateInfo.currentValue, item.currentValue)
        putInt(values, Collection.PRIVATE_INFO_QUANTITY, privateInfo.quantity, item.quantity)
        putString(values, Collection.PRIVATE_INFO_ACQUISITION_DATE, privateInfo.acquisitionDate, item.acquisitionDate)
        putString(values, Collection.PRIVATE_INFO_ACQUIRED_FROM, privateInfo.acquiredFrom, item.acquiredFrom)
        putString(values, Collection.PRIVATE_INFO_INVENTORY_LOCATION, privateInfo.inventoryLocation, item.inventoryLocation)
        return values
    }

    private fun putString(values: ContentValues, columnName: String, futureValue: String?, currentValue: String?) {
        if (futureValue != currentValue) {
            values.put(columnName, futureValue)
        }
    }

    private fun putDouble(values: ContentValues, columnName: String, futureValue: Double?, currentValue: Double?) {
        if (futureValue != currentValue) {
            values.put(columnName, futureValue)
        }
    }

    private fun putInt(values: ContentValues, columnName: String, futureValue: Int?, currentValue: Int?) {
        if (futureValue != currentValue) {
            values.put(columnName, futureValue)
        }
    }

    override fun onPostExecute(result: Boolean?) {
        super.onPostExecute(result)
        if (result == true) {
            Timber.i("Updated game ID $gameId, collection ID $collectionId with private info.")
        } else {
            Timber.i("No private info to update for game ID $gameId, collection ID $collectionId.")
        }
    }

    data class Item(
            val priceCurrency: String,
            val price: Double?,
            val currentValueCurrency: String,
            val currentValue: Double?,
            val quantity: Int?,
            val acquisitionDate: String,
            val acquiredFrom: String,
            val inventoryLocation: String
    ) {
        companion object {
            fun fromResolver(contentResolver: ContentResolver, internalId: Long): Item? {
                val cursor = contentResolver.load(Collection.buildUri(internalId), arrayOf(
                        Collection.PRIVATE_INFO_PRICE_PAID_CURRENCY,
                        Collection.PRIVATE_INFO_PRICE_PAID,
                        Collection.PRIVATE_INFO_CURRENT_VALUE_CURRENCY,
                        Collection.PRIVATE_INFO_CURRENT_VALUE,
                        Collection.PRIVATE_INFO_QUANTITY,
                        Collection.PRIVATE_INFO_ACQUISITION_DATE,
                        Collection.PRIVATE_INFO_ACQUIRED_FROM,
                        Collection.PRIVATE_INFO_INVENTORY_LOCATION
                ))
                cursor?.use {
                    if (it.moveToFirst()) {
                        return Item(
                                it.getStringOrEmpty(Collection.PRIVATE_INFO_PRICE_PAID_CURRENCY),
                                it.getDoubleOrZero(Collection.PRIVATE_INFO_PRICE_PAID),
                                it.getStringOrEmpty(Collection.PRIVATE_INFO_CURRENT_VALUE_CURRENCY),
                                it.getDoubleOrZero(Collection.PRIVATE_INFO_CURRENT_VALUE),
                                it.getIntOrNull(Collection.PRIVATE_INFO_QUANTITY) ?: 1,
                                it.getStringOrEmpty(Collection.PRIVATE_INFO_ACQUISITION_DATE),
                                it.getStringOrEmpty(Collection.PRIVATE_INFO_ACQUIRED_FROM),
                                it.getStringOrEmpty(Collection.PRIVATE_INFO_INVENTORY_LOCATION)
                        )
                    }
                }
                return null
            }
        }
    }
}
