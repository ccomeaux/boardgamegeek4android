package com.boardgamegeek.ui.model

import android.database.Cursor
import android.net.Uri
import com.boardgamegeek.provider.BggContract.Collection

data class CollectionItem(
        val id: Int,
        val internalId: Long,
        val name: String,
        val comment: String,
        val commentTimestamp: Long,
        val lastModifiedDateTime: Long,
        val rating: Double,
        val ratingTimestamp: Long,
        val updated: Long,
        val priceCurrency: String,
        val price: Double,
        val currentValueCurrency: String,
        val currentValue: Double,
        val quantity: Int,
        val acquiredFrom: String,
        val acquisitionDate: String,
        val privateComment: String,
        val inventoryLocation: String,
        val privateInfoTimestamp: Long,
        val statusTimestamp: Long,
        val imageUrl: String,
        val thumbnailUrl: String,
        val heroImageUrl: String,
        val year: Int,
        val condition: String,
        val wantParts: String,
        val hasParts: String,
        val wishlistPriority: Int,
        val wishlistComment: String,
        val numberOfPlays: Int,
        val isOwn: Boolean,
        val isPreviouslyOwned: Boolean,
        val isWantToBuy: Boolean,
        val isWantToPlay: Boolean,
        val isPreordered: Boolean,
        val isWantInTrade: Boolean,
        val isForTrade: Boolean,
        val isWishlist: Boolean,
        val dirtyTimestamp: Long,
        val wishlistCommentDirtyTimestamp: Long,
        val tradeConditionDirtyTimestamp: Long,
        val wantPartsDirtyTimestamp: Long,
        val hasPartsDirtyTimestamp: Long
) {

    val safeWishlistPriority: Int
        get() = when {
            wishlistPriority < 1 -> 1
            wishlistPriority > 5 -> 5
            else -> wishlistPriority
        }

    fun isDirty(): Boolean {
        return dirtyTimestamp > 0L ||
                ratingTimestamp > 0L ||
                commentTimestamp > 0L ||
                privateInfoTimestamp > 0L ||
                statusTimestamp > 0L ||
                wishlistCommentDirtyTimestamp > 0L ||
                tradeConditionDirtyTimestamp > 0L ||
                wantPartsDirtyTimestamp > 0L ||
                hasPartsDirtyTimestamp > 0L
    }

    companion object {
        @JvmStatic
        val uri: Uri = Collection.CONTENT_URI

        @JvmStatic
        fun getSelection(collectionId: Int): String {
            return if (collectionId != 0) {
                "${Collection.COLLECTION_ID}=?"
            } else {
                "collection.${Collection.GAME_ID}=? AND ${Collection.COLLECTION_ID} IS NULL"
            }
        }

        @JvmStatic
        fun getSelectionArgs(collectionId: Int, gameId: Int): Array<String> {
            return if (collectionId != 0) {
                arrayOf(collectionId.toString())
            } else {
                arrayOf(gameId.toString())
            }
        }

        @JvmStatic
        val projection = arrayOf(
                Collection._ID,
                Collection.COLLECTION_ID,
                Collection.COLLECTION_NAME,
                Collection.COLLECTION_SORT_NAME,
                Collection.COMMENT,
                Collection.PRIVATE_INFO_PRICE_PAID_CURRENCY,
                Collection.PRIVATE_INFO_PRICE_PAID,
                Collection.PRIVATE_INFO_CURRENT_VALUE_CURRENCY,
                Collection.PRIVATE_INFO_CURRENT_VALUE,
                Collection.PRIVATE_INFO_QUANTITY,
                Collection.PRIVATE_INFO_ACQUISITION_DATE,
                Collection.PRIVATE_INFO_ACQUIRED_FROM,
                Collection.PRIVATE_INFO_COMMENT,
                Collection.LAST_MODIFIED,
                Collection.COLLECTION_THUMBNAIL_URL,
                Collection.COLLECTION_IMAGE_URL,
                Collection.COLLECTION_YEAR_PUBLISHED,
                Collection.CONDITION,
                Collection.HASPARTS_LIST,
                Collection.WANTPARTS_LIST,
                Collection.WISHLIST_COMMENT,
                Collection.RATING,
                Collection.UPDATED,
                Collection.STATUS_OWN,
                Collection.STATUS_PREVIOUSLY_OWNED,
                Collection.STATUS_FOR_TRADE,
                Collection.STATUS_WANT,
                Collection.STATUS_WANT_TO_BUY,
                Collection.STATUS_WISHLIST,
                Collection.STATUS_WANT_TO_PLAY,
                Collection.STATUS_PREORDERED,
                Collection.STATUS_WISHLIST_PRIORITY,
                Collection.NUM_PLAYS,
                Collection.RATING_DIRTY_TIMESTAMP,
                Collection.COMMENT_DIRTY_TIMESTAMP,
                Collection.PRIVATE_INFO_DIRTY_TIMESTAMP,
                Collection.STATUS_DIRTY_TIMESTAMP,
                Collection.COLLECTION_DIRTY_TIMESTAMP,
                Collection.WISHLIST_COMMENT_DIRTY_TIMESTAMP,
                Collection.TRADE_CONDITION_DIRTY_TIMESTAMP,
                Collection.WANT_PARTS_DIRTY_TIMESTAMP,
                Collection.HAS_PARTS_DIRTY_TIMESTAMP,
                Collection.COLLECTION_HERO_IMAGE_URL,
                Collection.PRIVATE_INFO_INVENTORY_LOCATION
        )

        private const val ID = 0
        private const val COLLECTION_ID = 1
        private const val COLLECTION_NAME = 2
        //  private const val COLLECTION_SORT_NAME = 3;
        private const val COMMENT = 4
        private const val PRIVATE_INFO_PRICE_PAID_CURRENCY = 5
        private const val PRIVATE_INFO_PRICE_PAID = 6
        private const val PRIVATE_INFO_CURRENT_VALUE_CURRENCY = 7
        private const val PRIVATE_INFO_CURRENT_VALUE = 8
        private const val PRIVATE_INFO_QUANTITY = 9
        private const val PRIVATE_INFO_ACQUISITION_DATE = 10
        private const val PRIVATE_INFO_ACQUIRED_FROM = 11
        private const val PRIVATE_INFO_COMMENT = 12
        private const val LAST_MODIFIED = 13
        private const val COLLECTION_THUMBNAIL_URL = 14
        private const val COLLECTION_IMAGE_URL = 15
        private const val COLLECTION_YEAR_PUBLISHED = 16
        private const val CONDITION = 17
        private const val HAS_PARTS_LIST = 18
        private const val WANT_PARTS_LIST = 19
        private const val WISHLIST_COMMENT = 20
        private const val RATING = 21
        private const val UPDATED = 22
        private const val STATUS_OWN = 23
        private const val STATUS_PREVIOUSLY_OWNED = 24
        private const val STATUS_FOR_TRADE = 25
        private const val STATUS_WANT = 26
        private const val STATUS_WANT_TO_BUY = 27
        private const val STATUS_WISHLIST = 28
        private const val STATUS_WANT_TO_PLAY = 29
        private const val STATUS_PRE_ORDERED = 30
        private const val STATUS_WISHLIST_PRIORITY = 31
        private const val NUM_PLAYS = 32
        private const val RATING_DIRTY_TIMESTAMP = 33
        private const val COMMENT_DIRTY_TIMESTAMP = 34
        private const val PRIVATE_INFO_DIRTY_TIMESTAMP = 35
        private const val STATUS_DIRTY_TIMESTAMP = 36
        private const val COLLECTION_DIRTY_TIMESTAMP = 37
        private const val WISHLIST_COMMENT_DIRTY_TIMESTAMP = 38
        private const val TRADE_CONDITION_DIRTY_TIMESTAMP = 39
        private const val WANT_PARTS_DIRTY_TIMESTAMP = 40
        private const val HAS_PARTS_DIRTY_TIMESTAMP = 41
        private const val COLLECTION_HERO_IMAGE_URL = 42
        private const val PRIVATE_INFO_INVENTORY_LOCATION = 43

        @JvmStatic
        fun fromCursor(cursor: Cursor): CollectionItem {
            return CollectionItem(
                    cursor.getInt(COLLECTION_ID),
                    cursor.getLong(ID),
                    cursor.getString(COLLECTION_NAME),
                    cursor.getString(COMMENT) ?: "",
                    cursor.getLong(COMMENT_DIRTY_TIMESTAMP),
                    cursor.getLong(LAST_MODIFIED),
                    cursor.getDouble(RATING),
                    cursor.getLong(RATING_DIRTY_TIMESTAMP),
                    cursor.getLong(UPDATED),
                    cursor.getString(PRIVATE_INFO_PRICE_PAID_CURRENCY) ?: "",
                    cursor.getDouble(PRIVATE_INFO_PRICE_PAID),
                    cursor.getString(PRIVATE_INFO_CURRENT_VALUE_CURRENCY) ?: "",
                    cursor.getDouble(PRIVATE_INFO_CURRENT_VALUE),
                    cursor.getInt(PRIVATE_INFO_QUANTITY),
                    cursor.getString(PRIVATE_INFO_ACQUIRED_FROM) ?: "",
                    cursor.getString(PRIVATE_INFO_ACQUISITION_DATE) ?: "",
                    cursor.getString(PRIVATE_INFO_COMMENT) ?: "",
                    cursor.getString(PRIVATE_INFO_INVENTORY_LOCATION) ?: "",
                    cursor.getLong(PRIVATE_INFO_DIRTY_TIMESTAMP),
                    cursor.getLong(STATUS_DIRTY_TIMESTAMP),
                    cursor.getString(COLLECTION_IMAGE_URL) ?: "",
                    cursor.getString(COLLECTION_THUMBNAIL_URL) ?: "",
                    cursor.getString(COLLECTION_HERO_IMAGE_URL) ?: "",
                    cursor.getInt(COLLECTION_YEAR_PUBLISHED),
                    cursor.getString(CONDITION) ?: "",
                    cursor.getString(WANT_PARTS_LIST) ?: "",
                    cursor.getString(HAS_PARTS_LIST) ?: "",
                    cursor.getInt(STATUS_WISHLIST_PRIORITY),
                    cursor.getString(WISHLIST_COMMENT) ?: "",
                    cursor.getInt(NUM_PLAYS),
                    cursor.getInt(STATUS_OWN) == 1,
                    cursor.getInt(STATUS_PREVIOUSLY_OWNED) == 1,
                    cursor.getInt(STATUS_WANT_TO_BUY) == 1,
                    cursor.getInt(STATUS_WANT_TO_PLAY) == 1,
                    cursor.getInt(STATUS_PRE_ORDERED) == 1,
                    cursor.getInt(STATUS_WANT) == 1,
                    cursor.getInt(STATUS_FOR_TRADE) == 1,
                    cursor.getInt(STATUS_WISHLIST) == 1,
                    cursor.getLong(COLLECTION_DIRTY_TIMESTAMP),
                    cursor.getLong(WISHLIST_COMMENT_DIRTY_TIMESTAMP),
                    cursor.getLong(TRADE_CONDITION_DIRTY_TIMESTAMP),
                    cursor.getLong(WANT_PARTS_DIRTY_TIMESTAMP),
                    cursor.getLong(HAS_PARTS_DIRTY_TIMESTAMP)
            )
        }
    }
}
