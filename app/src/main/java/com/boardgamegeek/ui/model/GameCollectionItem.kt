package com.boardgamegeek.ui.model


import android.content.Context
import android.database.Cursor
import android.net.Uri
import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.util.PresentationUtils
import java.util.*

data class GameCollectionItem(
        val internalId: Long,
        val collectionId: Int,
        val collectionName: String,
        val collectionYearPublished: Int,
        val yearPublished: Int,
        val imageUrl: String,
        val thumbnailUrl: String,
        val comment: String,
        val numberOfPlays: Int,
        val rating: Double,
        val syncTimestamp: Long,
        val statuses: List<String>
) {

    companion object {
        val uri: Uri = Collection.CONTENT_URI

        val selection: String
            get() = "collection.${Collection.GAME_ID}=?"

        fun getSelectionArgs(gameId: Int): Array<String> {
            return arrayOf(gameId.toString())
        }

        val projection = arrayOf(
                Collection._ID,
                Collection.COLLECTION_ID,
                Collection.COLLECTION_NAME,
                Collection.COLLECTION_YEAR_PUBLISHED,
                Collection.COLLECTION_THUMBNAIL_URL,
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
                Collection.COMMENT,
                Collection.YEAR_PUBLISHED,
                Collection.RATING,
                Collection.IMAGE_URL,
                Collection.UPDATED
        )

        private val _ID = 0
        private val COLLECTION_ID = 1
        private val COLLECTION_NAME = 2
        private val COLLECTION_YEAR_PUBLISHED = 3
        private val COLLECTION_THUMBNAIL_URL = 4
        private val STATUS_1 = 5
        private val STATUS_N = 12
        private val STATUS_WISH_LIST = 10
        private val STATUS_WISH_LIST_PRIORITY = 13
        private val NUM_PLAYS = 14
        private val COMMENT = 15
        private val YEAR_PUBLISHED = 16
        private val RATING = 17
        private val COLLECTION_IMAGE_URL = 18
        private val UPDATED = 19

        fun fromCursor(context: Context, cursor: Cursor): GameCollectionItem {
            val statuses = ArrayList<String>()
            (STATUS_1..STATUS_N)
                    .filter { cursor.getInt(it) == 1 }
                    .forEach {
                        if (it == STATUS_WISH_LIST) {
                            statuses.add(PresentationUtils.describeWishlist(context, cursor.getInt(STATUS_WISH_LIST_PRIORITY)))
                        } else {
                            statuses.add(context.resources.getStringArray(R.array.collection_status_filter_entries)[it - STATUS_1])
                        }
                    }

            return GameCollectionItem(
                    cursor.getLong(_ID),
                    cursor.getInt(COLLECTION_ID),
                    cursor.getString(COLLECTION_NAME) ?: "",
                    cursor.getInt(COLLECTION_YEAR_PUBLISHED),
                    cursor.getInt(YEAR_PUBLISHED),
                    cursor.getString(COLLECTION_IMAGE_URL) ?: "",
                    cursor.getString(COLLECTION_THUMBNAIL_URL) ?: "",
                    cursor.getString(COMMENT) ?: "",
                    cursor.getInt(NUM_PLAYS),
                    cursor.getDouble(RATING),
                    cursor.getLong(UPDATED),
                    statuses
            )
        }
    }
}
