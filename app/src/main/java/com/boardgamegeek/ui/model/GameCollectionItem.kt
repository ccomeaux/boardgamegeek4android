package com.boardgamegeek.ui.model


import android.content.Context
import android.database.Cursor
import android.net.Uri
import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.util.PresentationUtils
import java.util.*

class GameCollectionItem private constructor() {

    var internalId: Long = 0
        private set
    var collectionId: Int = 0
        private set
    var yearPublished: Int = 0
        private set
    var imageUrl: String? = null
        private set
    var thumbnailUrl: String? = null
        private set
    var collectionName: String? = null
        private set
    var collectionYearPublished: Int = 0
        private set
    var numberOfPlays: Int = 0
        private set
    var comment: String? = null
        private set
    var rating: Double = 0.toDouble()
        private set
    private var statuses: MutableList<String> = ArrayList(0)
    var syncTimestamp: Long = 0
        private set

    fun getStatuses(): List<String>? {
        return statuses
    }

    companion object {
        val projection = arrayOf(Collection._ID, Collection.COLLECTION_ID, Collection.COLLECTION_NAME, Collection.COLLECTION_YEAR_PUBLISHED, Collection.COLLECTION_THUMBNAIL_URL, Collection.STATUS_OWN, Collection.STATUS_PREVIOUSLY_OWNED, Collection.STATUS_FOR_TRADE, Collection.STATUS_WANT, Collection.STATUS_WANT_TO_BUY, Collection.STATUS_WISHLIST, Collection.STATUS_WANT_TO_PLAY, Collection.STATUS_PREORDERED, Collection.STATUS_WISHLIST_PRIORITY, Collection.NUM_PLAYS, Collection.COMMENT, Collection.YEAR_PUBLISHED, Collection.RATING, Collection.IMAGE_URL, Collection.UPDATED)

        val uri: Uri = Collection.CONTENT_URI

        private val _ID = 0
        private val COLLECTION_ID = 1
        private val COLLECTION_NAME = 2
        private val COLLECTION_YEAR = 3
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
            val item = GameCollectionItem()
            item.internalId = cursor.getLong(_ID)
            item.collectionId = cursor.getInt(COLLECTION_ID)
            item.yearPublished = cursor.getInt(YEAR_PUBLISHED)
            item.imageUrl = cursor.getString(COLLECTION_IMAGE_URL)
            item.thumbnailUrl = cursor.getString(COLLECTION_THUMBNAIL_URL)
            item.collectionName = cursor.getString(COLLECTION_NAME)
            item.collectionYearPublished = cursor.getInt(COLLECTION_YEAR)
            item.numberOfPlays = cursor.getInt(NUM_PLAYS)
            item.comment = cursor.getString(COMMENT)
            item.rating = cursor.getDouble(RATING)
            item.syncTimestamp = cursor.getLong(UPDATED)

            item.statuses = ArrayList()
            (STATUS_1..STATUS_N)
                    .filter { cursor.getInt(it) == 1 }
                    .forEach {
                        if (it == STATUS_WISH_LIST) {
                            item.statuses.add(PresentationUtils.describeWishlist(context, cursor.getInt(STATUS_WISH_LIST_PRIORITY)))
                        } else {
                            item.statuses.add(context.resources.getStringArray(R.array.collection_status_filter_entries)[it - STATUS_1])
                        }
                    }
            return item
        }

        val selection: String
            get() = "collection.${Collection.GAME_ID}=?"

        fun getSelectionArgs(gameId: Int): Array<String> {
            return arrayOf(gameId.toString())
        }
    }
}
