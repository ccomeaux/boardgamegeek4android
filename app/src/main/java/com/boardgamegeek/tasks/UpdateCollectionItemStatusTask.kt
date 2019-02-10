package com.boardgamegeek.tasks

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import com.boardgamegeek.extensions.use
import com.boardgamegeek.provider.BggContract.Collection
import timber.log.Timber

class UpdateCollectionItemStatusTask(context: Context?,
            gameId: Int,
            collectionId: Int,
            internalId: Long,
            private val statuses: List<String>,
            private val wishListPriority: Int) :
        UpdateCollectionItemTask(context, gameId, collectionId, internalId) {

    override fun updateResolver(resolver: ContentResolver, internalId: Long): Boolean {
        val item = Item.fromResolver(resolver, internalId) ?: return false
        val values = updateValues(item)
        if (values.size() > 0) {
            values.put(Collection.STATUS_DIRTY_TIMESTAMP, System.currentTimeMillis())
            resolver.update(Collection.buildUri(internalId), values, null, null)
            return true
        }
        return false
    }

    private fun updateValues(item: Item): ContentValues {
        val values = ContentValues(10)
        putStatus(values, Collection.STATUS_OWN, item.owned)
        putStatus(values, Collection.STATUS_PREORDERED, item.preOrdered)
        putStatus(values, Collection.STATUS_FOR_TRADE, item.forTrade)
        putStatus(values, Collection.STATUS_WANT, item.wantInTrade)
        putStatus(values, Collection.STATUS_WANT_TO_PLAY, item.wantToPlay)
        putStatus(values, Collection.STATUS_WANT_TO_BUY, item.wantToBuy)
        putStatus(values, Collection.STATUS_PREVIOUSLY_OWNED, item.previouslyOwned)
        putWishList(values, item)
        return values
    }

    private fun putStatus(values: ContentValues, statusColumn: String, currentStatus: Boolean) {
        val futureValue = statuses.contains(statusColumn)
        if (currentStatus != futureValue) {
            values.put(statusColumn, if (futureValue) 1 else 0)
        }
    }

    private fun putWishList(values: ContentValues, item: Item) {
        val futureValue = statuses.contains(Collection.STATUS_WISHLIST)
        if (futureValue != item.wishList || wishListPriority != item.wishListPriority) {
            if (statuses.contains(Collection.STATUS_WISHLIST)) {
                values.put(Collection.STATUS_WISHLIST, 1)
                values.put(Collection.STATUS_WISHLIST_PRIORITY, wishListPriority)
            } else {
                values.put(Collection.STATUS_WISHLIST, 0)
            }
        }
    }

    override fun onPostExecute(result: Boolean?) {
        super.onPostExecute(result)
        if (result == true) {
            Timber.i("Updated game ID $gameId, collection ID $collectionId with statuses \"$statuses\".")
        } else {
            Timber.i("No statuses to update for game ID $gameId, collection ID $collectionId.")
        }
    }

    data class Item(
            val owned: Boolean,
            val preOrdered: Boolean,
            val forTrade: Boolean,
            val wantInTrade: Boolean,
            val wantToPlay: Boolean,
            val wantToBuy: Boolean,
            val wishList: Boolean,
            val previouslyOwned: Boolean,
            val wishListPriority: Int
    ) {
        companion object {
            val projection = arrayOf(
                    Collection.STATUS_OWN,
                    Collection.STATUS_PREORDERED,
                    Collection.STATUS_FOR_TRADE,
                    Collection.STATUS_WANT,
                    Collection.STATUS_WANT_TO_PLAY,
                    Collection.STATUS_WANT_TO_BUY,
                    Collection.STATUS_WISHLIST,
                    Collection.STATUS_PREVIOUSLY_OWNED,
                    Collection.STATUS_WISHLIST_PRIORITY
            )

            private const val STATUS_OWN = 0
            private const val STATUS_PRE_ORDERED = 1
            private const val STATUS_FOR_TRADE = 2
            private const val STATUS_WANT = 3
            private const val STATUS_WANT_TO_PLAY = 4
            private const val STATUS_WANT_TO_BUY = 5
            private const val STATUS_WISH_LIST = 6
            private const val STATUS_PREVIOUSLY_OWNED = 7
            private const val STATUS_WISH_LIST_PRIORITY = 8

            fun fromResolver(contentResolver: ContentResolver, internalId: Long): Item? {
                val cursor = contentResolver.query(Collection.buildUri(internalId), projection, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        return Item(
                                it.getInt(STATUS_OWN) == 1,
                                it.getInt(STATUS_PRE_ORDERED) == 1,
                                it.getInt(STATUS_FOR_TRADE) == 1,
                                it.getInt(STATUS_WANT) == 1,
                                it.getInt(STATUS_WANT_TO_PLAY) == 1,
                                it.getInt(STATUS_WANT_TO_BUY) == 1,
                                it.getInt(STATUS_WISH_LIST) == 1,
                                it.getInt(STATUS_PREVIOUSLY_OWNED) == 1,
                                it.getInt(STATUS_WISH_LIST_PRIORITY)
                        )
                    }
                }
                return null
            }
        }
    }
}