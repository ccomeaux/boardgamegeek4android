package com.boardgamegeek.tasks

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import com.boardgamegeek.provider.BggContract.Collection
import hugo.weaving.DebugLog
import timber.log.Timber

class UpdateCollectionItemStatusTask @DebugLog
constructor(context: Context,
            gameId: Int,
            collectionId: Int,
            internalId: Long,
            private val statuses: List<String>,
            private val wishListPriority: Int) :
        UpdateCollectionItemTask(context, gameId, collectionId, internalId) {

    override fun updateResolver(resolver: ContentResolver, internalId: Long) {
        val item = Item.fromResolver(resolver, internalId) ?: return
        val values = ContentValues(10)
        val changed = updateValues(values, item)
        if (changed) {
            values.put(Collection.STATUS_DIRTY_TIMESTAMP, System.currentTimeMillis())
            resolver.update(Collection.buildUri(internalId), values, null, null)
        }
    }

    private fun updateValues(values: ContentValues, item: Item): Boolean {
        var changed = false
        changed = changed || putValue(values, Collection.STATUS_OWN, item.owned)
        changed = changed || putValue(values, Collection.STATUS_PREORDERED, item.preOrdered)
        changed = changed || putValue(values, Collection.STATUS_FOR_TRADE, item.forTrade)
        changed = changed || putValue(values, Collection.STATUS_WANT, item.wantInTrade)
        changed = changed || putValue(values, Collection.STATUS_WANT_TO_PLAY, item.wantToPlay)
        changed = changed || putValue(values, Collection.STATUS_WANT_TO_BUY, item.wantToBuy)
        changed = changed || putValue(values, Collection.STATUS_PREVIOUSLY_OWNED, item.previouslyOwned)
        changed = changed || putWishList(values, item)
        return changed
    }

    private fun putValue(values: ContentValues, statusColumn: String, currentStatus: Boolean): Boolean {
        val futureValue = statuses.contains(statusColumn)
        if (currentStatus != futureValue) {
            values.put(statusColumn, if (futureValue) 1 else 0)
            return true
        }
        return false
    }

    private fun putWishList(values: ContentValues, item: Item): Boolean {
        val futureValue = statuses.contains(Collection.STATUS_WISHLIST)
        if (futureValue != item.wishList && wishListPriority != item.wishListPriority) {
            if (statuses.contains(Collection.STATUS_WISHLIST)) {
                values.put(Collection.STATUS_WISHLIST, 1)
                values.put(Collection.STATUS_WISHLIST_PRIORITY, wishListPriority)
            } else {
                values.put(Collection.STATUS_WISHLIST, 0)
            }
            return true
        }
        return false
    }

    @DebugLog
    override fun onPostExecute(result: Void?) {
        super.onPostExecute(result)
        Timber.i("Updated game ID %1\$s, collection ID %2\$s with statuses \"%3\$s\"", gameId, collectionId, statuses.toString())
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

            private val STATUS_OWN = 0
            private val STATUS_PRE_ORDERED = 1
            private val STATUS_FOR_TRADE = 2
            private val STATUS_WANT = 3
            private val STATUS_WANT_TO_PLAY = 4
            private val STATUS_WANT_TO_BUY = 5
            private val STATUS_WISH_LIST = 6
            private val STATUS_PREVIOUSLY_OWNED = 7
            private val STATUS_WISH_LIST_PRIORITY = 8

            fun fromResolver(contentResolver: ContentResolver, internalId: Long): Item? {
                val cursor = contentResolver.query(Collection.buildUri(internalId), projection, null, null, null)
                cursor.use { cursor ->
                    if (cursor.moveToFirst()) {
                        return Item(
                                cursor.getInt(STATUS_OWN) == 1,
                                cursor.getInt(STATUS_PRE_ORDERED) == 1,
                                cursor.getInt(STATUS_FOR_TRADE) == 1,
                                cursor.getInt(STATUS_WANT) == 1,
                                cursor.getInt(STATUS_WANT_TO_PLAY) == 1,
                                cursor.getInt(STATUS_WANT_TO_BUY) == 1,
                                cursor.getInt(STATUS_WISH_LIST) == 1,
                                cursor.getInt(STATUS_PREVIOUSLY_OWNED) == 1,
                                cursor.getInt(STATUS_WISH_LIST_PRIORITY)
                        )
                    }
                }
                return null
            }
        }
    }
}