package com.boardgamegeek.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.provider.BaseColumns
import androidx.core.content.contentValuesOf
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.boardgamegeek.db.model.PublisherBasic
import com.boardgamegeek.db.model.PublisherLocal
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract.Publishers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class PublisherDao(private val context: Context) {
    enum class SortType {
        NAME, ITEM_COUNT, WHITMORE_SCORE
    }

    suspend fun loadPublishers(sortBy: SortType): List<PublisherLocal> = withContext(Dispatchers.IO) {
        val sortByName = Publishers.Columns.PUBLISHER_NAME.collateNoCase().ascending()
        val sortOrder = when (sortBy) {
            SortType.NAME -> sortByName
            SortType.ITEM_COUNT -> Publishers.Columns.ITEM_COUNT.descending().plus(", $sortByName")
            SortType.WHITMORE_SCORE -> Publishers.Columns.WHITMORE_SCORE.descending().plus(", $sortByName")
        }
        context.contentResolver.loadList(
            Publishers.CONTENT_URI,
            projection(),
            sortOrder = sortOrder
        ) {
            fromCursor(it)
        }
    }

    suspend fun loadPublisher(id: Int): PublisherLocal? = withContext(Dispatchers.IO) {
        context.contentResolver.loadEntity(
            Publishers.buildPublisherUri(id),
            projection()
        ) {
            fromCursor(it)
        }
    }

    private fun projection() = arrayOf(
        Publishers.Columns.PUBLISHER_ID,
        Publishers.Columns.PUBLISHER_NAME,
        Publishers.Columns.PUBLISHER_DESCRIPTION,
        Publishers.Columns.PUBLISHER_IMAGE_URL,
        Publishers.Columns.PUBLISHER_THUMBNAIL_URL,
        Publishers.Columns.PUBLISHER_HERO_IMAGE_URL,
        Publishers.Columns.UPDATED,
        Publishers.Columns.ITEM_COUNT,
        Publishers.Columns.WHITMORE_SCORE,
        Publishers.Columns.PUBLISHER_STATS_UPDATED_TIMESTAMP,
        Publishers.Columns.PUBLISHER_SORT_NAME, // 10
        //Publishers.Columns.ARTIST_IMAGES_UPDATED_TIMESTAMP,
        BaseColumns._ID,
    )

    private fun fromCursor(it: Cursor) = PublisherLocal(
        internalId = it.getInt(11),
        publisherId = it.getInt(0),
        publisherName = it.getStringOrNull(1).orEmpty(),
        //sortName = it.getStringOrNull(10).orEmpty(),
        publisherDescription = it.getStringOrNull(2).orEmpty(),
        publisherImageUrl = it.getStringOrNull(3).orEmpty(),
        publisherThumbnailUrl = it.getStringOrNull(4).orEmpty(),
        publisherHeroImageUrl = it.getStringOrNull(5).orEmpty(),
        updatedTimestamp = it.getLongOrNull(6) ?: 0L,
        itemCount = it.getIntOrNull(7) ?: 0,
        whitmoreScore = it.getIntOrNull(8) ?: 0,
        statsUpdatedTimestamp = it.getLongOrNull(9) ?: 0L,
    )

    suspend fun upsert(publisherLocal: PublisherBasic): Int = withContext(Dispatchers.IO) {
        val values = contentValuesOf(
            Publishers.Columns.PUBLISHER_NAME to publisherLocal.publisherName,
            Publishers.Columns.PUBLISHER_SORT_NAME to publisherLocal.sortName,
            Publishers.Columns.PUBLISHER_DESCRIPTION to publisherLocal.publisherDescription,
            Publishers.Columns.PUBLISHER_IMAGE_URL to publisherLocal.publisherImageUrl,
            Publishers.Columns.PUBLISHER_THUMBNAIL_URL to publisherLocal.publisherThumbnailUrl,
            Publishers.Columns.UPDATED to System.currentTimeMillis(),
        )
        upsert(publisherLocal.publisherId, values)
    }

    suspend fun updateHeroImageUrl(publisherId: Int, url: String): Int = withContext(Dispatchers.IO) {
        val values = contentValuesOf(
            Publishers.Columns.PUBLISHER_HERO_IMAGE_URL to url,
        )
        upsert(publisherId, values)
    }

    suspend fun updateWhitmoreScore(publisherId: Int, score: Int): Int = withContext(Dispatchers.IO) {
        val values = contentValuesOf(
            Publishers.Columns.WHITMORE_SCORE to score,
            Publishers.Columns.PUBLISHER_STATS_UPDATED_TIMESTAMP to System.currentTimeMillis(),
        )
        upsert(publisherId, values)
    }

    private suspend fun upsert(publisherId: Int, values: ContentValues): Int = withContext(Dispatchers.IO) {
        val uri = Publishers.buildPublisherUri(publisherId)
        if (context.contentResolver.rowExists(uri)) {
            val count = context.contentResolver.update(uri, values, null, null)
            Timber.d("Updated %,d publisher rows at %s", count, uri)
            count
        } else {
            values.put(Publishers.Columns.PUBLISHER_ID, publisherId)
            val insertedUri = context.contentResolver.insert(Publishers.CONTENT_URI, values)
            Timber.d("Inserted publisher at %s", insertedUri)
            1
        }
    }

    suspend fun delete(): Int = withContext(Dispatchers.IO) {
        context.contentResolver.delete(Publishers.CONTENT_URI, null, null)
    }
}
