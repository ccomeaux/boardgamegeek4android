package com.boardgamegeek.db

import android.content.ContentValues
import android.content.Context
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.boardgamegeek.entities.CompanyEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract.Publishers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class PublisherDao(private val context: Context) {
    private val collectionDao = CollectionDao(context)

    enum class SortType {
        NAME, ITEM_COUNT, WHITMORE_SCORE
    }

    suspend fun loadPublishers(sortBy: SortType): List<CompanyEntity> = withContext(Dispatchers.IO) {
        val sortByName = Publishers.Columns.PUBLISHER_NAME.collateNoCase().ascending()
        val sortOrder = when (sortBy) {
            SortType.NAME -> sortByName
            SortType.ITEM_COUNT -> Publishers.Columns.ITEM_COUNT.descending().plus(", $sortByName")
            SortType.WHITMORE_SCORE -> Publishers.Columns.WHITMORE_SCORE.descending().plus(", $sortByName")
        }
        context.contentResolver.loadList(
            Publishers.CONTENT_URI,
            arrayOf(
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
                Publishers.Columns.PUBLISHER_SORT_NAME,
            ),
            sortOrder = sortOrder
        ) {
            CompanyEntity(
                id = it.getInt(0),
                name = it.getStringOrNull(1).orEmpty(),
                sortName = it.getStringOrNull(10).orEmpty(),
                description = it.getStringOrNull(2).orEmpty(),
                imageUrl = it.getStringOrNull(3).orEmpty(),
                thumbnailUrl = it.getStringOrNull(4).orEmpty(),
                heroImageUrl = it.getStringOrNull(5).orEmpty(),
                updatedTimestamp = it.getLongOrNull(6) ?: 0L,
                itemCount = it.getIntOrNull(7) ?: 0,
                whitmoreScore = it.getIntOrNull(8) ?: 0,
                statsUpdatedTimestamp = it.getLongOrNull(9) ?: 0L,
            )
        }
    }

    suspend fun loadPublisher(id: Int): CompanyEntity? = withContext(Dispatchers.IO) {
        context.contentResolver.loadEntity(
            Publishers.buildPublisherUri(id),
            arrayOf(
                Publishers.Columns.PUBLISHER_ID,
                Publishers.Columns.PUBLISHER_NAME,
                Publishers.Columns.PUBLISHER_DESCRIPTION,
                Publishers.Columns.PUBLISHER_IMAGE_URL,
                Publishers.Columns.PUBLISHER_THUMBNAIL_URL,
                Publishers.Columns.PUBLISHER_HERO_IMAGE_URL,
                Publishers.Columns.UPDATED,
                Publishers.Columns.WHITMORE_SCORE,
                Publishers.Columns.PUBLISHER_SORT_NAME,
            )
        ) {
            CompanyEntity(
                id = it.getInt(0),
                name = it.getStringOrNull(1).orEmpty(),
                sortName = it.getStringOrNull(8).orEmpty(),
                description = it.getStringOrNull(2).orEmpty(),
                imageUrl = it.getStringOrNull(3).orEmpty(),
                thumbnailUrl = it.getStringOrNull(4).orEmpty(),
                heroImageUrl = it.getStringOrNull(5).orEmpty(),
                updatedTimestamp = it.getLongOrNull(6) ?: 0L,
                whitmoreScore = it.getIntOrNull(7) ?: 0,
            )
        }
    }

    suspend fun loadCollection(publisherId: Int, sortBy: CollectionDao.SortType = CollectionDao.SortType.RATING) =
        collectionDao.loadLinkedCollection(Publishers.buildCollectionUri(publisherId), sortBy)

    suspend fun upsert(publisherId: Int, values: ContentValues): Int = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val uri = Publishers.buildPublisherUri(publisherId)
        if (resolver.rowExists(uri)) {
            val count = resolver.update(uri, values, null, null)
            Timber.d("Updated %,d publisher rows at %s", count, uri)
            count
        } else {
            values.put(Publishers.Columns.PUBLISHER_ID, publisherId)
            val insertedUri = resolver.insert(Publishers.CONTENT_URI, values)
            Timber.d("Inserted publisher at %s", insertedUri)
            1
        }
    }

    suspend fun delete(): Int = withContext(Dispatchers.IO) {
        context.contentResolver.delete(Publishers.CONTENT_URI, null, null)
    }
}
