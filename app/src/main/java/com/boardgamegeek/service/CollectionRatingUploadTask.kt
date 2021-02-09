package com.boardgamegeek.service

import android.content.ContentValues
import com.boardgamegeek.entities.CollectionItemForUploadEntity
import com.boardgamegeek.provider.BggContract
import okhttp3.FormBody
import okhttp3.OkHttpClient

class CollectionRatingUploadTask(client: OkHttpClient) : CollectionUploadTask(client) {
    private var rating = INVALID_RATING

    override val timestampColumn = BggContract.Collection.RATING_DIRTY_TIMESTAMP

    override val isDirty = collectionItem.ratingTimestamp > 0

    override fun addCollectionItem(collectionItem: CollectionItemForUploadEntity) {
        super.addCollectionItem(collectionItem)
        rating = INVALID_RATING
    }

    override fun createForm(): FormBody {
        @Suppress("SpellCheckingInspection")
        return createFormBuilder()
                .add("fieldname", "rating")
                .add("rating", collectionItem.rating.toString())
                .build()
    }

    override fun saveContent(content: String) {
        rating = when {
            content.contains(N_A_SPAN) -> INVALID_RATING
            content.contains(RATING_DIV) -> {
                var index = content.indexOf(RATING_DIV) + RATING_DIV.length
                var message = content.substring(index)
                index = message.indexOf("<")
                if (index > 0) {
                    message = message.substring(0, index)
                }
                message.trim().toDoubleOrNull() ?: INVALID_RATING
            }
            else -> INVALID_RATING
        }
    }

    override fun appendContentValues(contentValues: ContentValues) {
        contentValues.put(BggContract.Collection.RATING, rating)
        contentValues.put(BggContract.Collection.RATING_DIRTY_TIMESTAMP, 0)
    }

    companion object {
        private const val N_A_SPAN = "<span>N/A</span>"

        @Suppress("SpellCheckingInspection")
        private const val RATING_DIV = "<div class='ratingtext'>"
        const val INVALID_RATING = -1.0
    }
}