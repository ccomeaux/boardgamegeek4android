package com.boardgamegeek.tasks


import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context

import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.ui.model.PrivateInfo

import hugo.weaving.DebugLog
import timber.log.Timber

class UpdateCollectionItemPrivateInfoTask @DebugLog
constructor(context: Context, gameId: Int, collectionId: Int, internalId: Long, private val privateInfo: PrivateInfo) : UpdateCollectionItemTask(context, gameId, collectionId, internalId) {

    @DebugLog
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
        putString(values, Collection.PRIVATE_INFO_COMMENT, privateInfo.privateComment, item.privateComment)
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

    @DebugLog
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
            val privateComment: String
    ) {
        companion object {
            val projection = arrayOf(
                    Collection.PRIVATE_INFO_PRICE_PAID_CURRENCY,
                    Collection.PRIVATE_INFO_PRICE_PAID,
                    Collection.PRIVATE_INFO_CURRENT_VALUE_CURRENCY,
                    Collection.PRIVATE_INFO_CURRENT_VALUE,
                    Collection.PRIVATE_INFO_QUANTITY,
                    Collection.PRIVATE_INFO_ACQUISITION_DATE,
                    Collection.PRIVATE_INFO_ACQUIRED_FROM,
                    Collection.PRIVATE_INFO_COMMENT
            )

            private val PRIVATE_INFO_PRICE_PAID_CURRENCY = 0
            private val PRIVATE_INFO_PRICE_PAID = 1
            private val PRIVATE_INFO_CURRENT_VALUE_CURRENCY = 2
            private val PRIVATE_INFO_CURRENT_VALUE = 3
            private val PRIVATE_INFO_QUANTITY = 4
            private val PRIVATE_INFO_ACQUISITION_DATE = 5
            private val PRIVATE_INFO_ACQUIRED_FROM = 6
            private val PRIVATE_INFO_COMMENT = 7

            fun fromResolver(contentResolver: ContentResolver, internalId: Long): Item? {
                val cursor = contentResolver.query(Collection.buildUri(internalId), projection, null, null, null)
                cursor.use { c ->
                    if (c.moveToFirst()) {
                        return Item(
                                c.getString(PRIVATE_INFO_PRICE_PAID_CURRENCY) ?: "",
                                c.getDouble(PRIVATE_INFO_PRICE_PAID),
                                c.getString(PRIVATE_INFO_CURRENT_VALUE_CURRENCY) ?: "",
                                c.getDouble(PRIVATE_INFO_CURRENT_VALUE),
                                c.getInt(PRIVATE_INFO_QUANTITY),
                                c.getString(PRIVATE_INFO_ACQUISITION_DATE) ?: "",
                                c.getString(PRIVATE_INFO_ACQUIRED_FROM) ?: "",
                                c.getString(PRIVATE_INFO_COMMENT) ?: ""
                        )
                    }
                }
                return null
            }
        }
    }
}
