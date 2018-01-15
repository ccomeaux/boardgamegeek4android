package com.boardgamegeek.ui.model


import android.database.Cursor
import android.net.Uri
import com.boardgamegeek.provider.BggContract.Collection

data class CollectionItem(
        var id: Int,
        var internalId: Long,
        var name: String,
        var comment: String,
        var commentTimestamp: Long,
        var lastModifiedDateTime: Long,
        var rating: Double,
        var ratingTimestamp: Long,
        var updated: Long,
        var priceCurrency: String,
        var price: Double,
        var currentValueCurrency: String,
        var currentValue: Double,
        var quantity: Int,
        var acquiredFrom: String,
        var acquisitionDate: String,
        var privateComment: String,
        var privateInfoTimestamp: Long,
        var statusTimestamp: Long,
        var imageUrl: String,
        var thumbnailUrl: String,
        var year: Int,
        var condition: String,
        var wantParts: String,
        var hasParts: String,
        var wishlistPriority: Int,
        var wishlistComment: String,
        var numberOfPlays: Int,
        var isOwn: Boolean,
        var isPreviouslyOwned: Boolean,
        var isWantToBuy: Boolean,
        var isWantToPlay: Boolean,
        var isPreordered: Boolean,
        var isWantInTrade: Boolean,
        var isForTrade: Boolean,
        var isWishlist: Boolean,
        var dirtyTimestamp: Long,
        var wishlistCommentDirtyTimestamp: Long,
        var tradeConditionDirtyTimestamp: Long,
        var wantPartsDirtyTimestamp: Long,
        var hasPartsDirtyTimestamp: Long
) {

    companion object {
        val uri: Uri = Collection.CONTENT_URI

        fun getSelection(collectionId: Int): String {
            return if (collectionId != 0) {
                "${Collection.COLLECTION_ID}=?"
            } else {
                "collection.${Collection.GAME_ID}=? AND ${Collection.COLLECTION_ID} IS NULL"
            }
        }

        fun getSelectionArgs(collectionId: Int, gameId: Int): Array<String> {
            return if (collectionId != 0) {
                arrayOf(collectionId.toString())
            } else {
                arrayOf(gameId.toString())
            }
        }

        val projection = arrayOf(Collection._ID,
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
                Collection.HAS_PARTS_DIRTY_TIMESTAMP)

        private val _ID = 0
        private val COLLECTION_ID = 1
        private val COLLECTION_NAME = 2
        // int COLLECTION_SORT_NAME = 3;
        private val COMMENT = 4
        private val PRIVATE_INFO_PRICE_PAID_CURRENCY = 5
        private val PRIVATE_INFO_PRICE_PAID = 6
        private val PRIVATE_INFO_CURRENT_VALUE_CURRENCY = 7
        private val PRIVATE_INFO_CURRENT_VALUE = 8
        private val PRIVATE_INFO_QUANTITY = 9
        private val PRIVATE_INFO_ACQUISITION_DATE = 10
        private val PRIVATE_INFO_ACQUIRED_FROM = 11
        private val PRIVATE_INFO_COMMENT = 12
        private val LAST_MODIFIED = 13
        private val COLLECTION_THUMBNAIL_URL = 14
        private val COLLECTION_IMAGE_URL = 15
        private val COLLECTION_YEAR_PUBLISHED = 16
        private val CONDITION = 17
        private val HAS_PARTS_LIST = 18
        private val WANT_PARTS_LIST = 19
        private val WISHLIST_COMMENT = 20
        private val RATING = 21
        private val UPDATED = 22
        private val STATUS_OWN = 23
        private val STATUS_PREVIOUSLY_OWNED = 24
        private val STATUS_FOR_TRADE = 25
        private val STATUS_WANT = 26
        private val STATUS_WANT_TO_BUY = 27
        private val STATUS_WISHLIST = 28
        private val STATUS_WANT_TO_PLAY = 29
        private val STATUS_PRE_ORDERED = 30
        private val STATUS_WISHLIST_PRIORITY = 31
        private val NUM_PLAYS = 32
        private val RATING_DIRTY_TIMESTAMP = 33
        private val COMMENT_DIRTY_TIMESTAMP = 34
        private val PRIVATE_INFO_DIRTY_TIMESTAMP = 35
        private val STATUS_DIRTY_TIMESTAMP = 36
        private val COLLECTION_DIRTY_TIMESTAMP = 37
        private val WISHLIST_COMMENT_DIRTY_TIMESTAMP = 38
        private val TRADE_CONDITION_DIRTY_TIMESTAMP = 39
        private val WANT_PARTS_DIRTY_TIMESTAMP = 40
        private val HAS_PARTS_DIRTY_TIMESTAMP = 41

        fun fromCursor(cursor: Cursor): CollectionItem {
            return CollectionItem(
                    cursor.getInt(COLLECTION_ID),
                    cursor.getLong(_ID),
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
                    cursor.getLong(PRIVATE_INFO_DIRTY_TIMESTAMP),
                    cursor.getLong(STATUS_DIRTY_TIMESTAMP),
                    cursor.getString(COLLECTION_IMAGE_URL) ?: "",
                    cursor.getString(COLLECTION_THUMBNAIL_URL) ?: "",
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

    val safeWishlistPriorty: Int
        get() {
            if (wishlistPriority < 1) return 1
            if (wishlistPriority > 5) return 5
            return wishlistPriority;
        }
}
