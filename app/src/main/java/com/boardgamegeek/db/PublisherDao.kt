package com.boardgamegeek.db

import android.content.ContentValues
import androidx.core.content.contentValuesOf
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.boardgamegeek.BggApplication
import com.boardgamegeek.entities.BriefGameEntity
import com.boardgamegeek.entities.CompanyEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.model.CompanyItem
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.Publishers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class PublisherDao(private val context: BggApplication) {
    private val collectionDao = CollectionDao(context)

    enum class SortType {
        NAME, ITEM_COUNT, WHITMORE_SCORE
    }

    suspend fun loadPublishers(sortBy: SortType): List<CompanyEntity> = withContext(Dispatchers.IO) {
        val results = arrayListOf<CompanyEntity>()
        val sortByName = Publishers.PUBLISHER_NAME.collateNoCase().ascending()
        val sortOrder = when (sortBy) {
            SortType.NAME -> sortByName
            SortType.ITEM_COUNT -> Publishers.ITEM_COUNT.descending().plus(", $sortByName")
            SortType.WHITMORE_SCORE -> Publishers.WHITMORE_SCORE.descending().plus(", $sortByName")
        }
        context.contentResolver.load(
                Publishers.CONTENT_URI,
                arrayOf(
                        Publishers.PUBLISHER_ID,
                        Publishers.PUBLISHER_NAME,
                        Publishers.PUBLISHER_DESCRIPTION,
                        Publishers.PUBLISHER_IMAGE_URL,
                        Publishers.PUBLISHER_THUMBNAIL_URL,
                        Publishers.PUBLISHER_HERO_IMAGE_URL,
                        Publishers.UPDATED,
                        Publishers.ITEM_COUNT,
                        Publishers.WHITMORE_SCORE,
                        Publishers.PUBLISHER_STATS_UPDATED_TIMESTAMP
                ),
                sortOrder = sortOrder
        )?.use {
            if (it.moveToFirst()) {
                do {
                    results += CompanyEntity(
                            id = it.getInt(0),
                            name = it.getStringOrNull(1).orEmpty(),
                            description = it.getStringOrNull(2).orEmpty(),
                            imageUrl = it.getStringOrNull(3).orEmpty(),
                            thumbnailUrl = it.getStringOrNull(4).orEmpty(),
                            heroImageUrl = it.getStringOrNull(5).orEmpty(),
                            updatedTimestamp = it.getLongOrNull(6) ?: 0L,
                            itemCount = it.getIntOrNull(7) ?: 0,
                            whitmoreScore = it.getIntOrNull(8) ?: 0,
                            statsUpdatedTimestamp = it.getLongOrNull(9) ?: 0L,
                    )
                } while (it.moveToNext())
            }
        }
        results
    }

    suspend fun loadPublisher(id: Int): CompanyEntity? = withContext(Dispatchers.IO) {
        context.contentResolver.load(
                Publishers.buildPublisherUri(id),
                arrayOf(
                        Publishers.PUBLISHER_ID,
                        Publishers.PUBLISHER_NAME,
                        Publishers.PUBLISHER_DESCRIPTION,
                        Publishers.PUBLISHER_IMAGE_URL,
                        Publishers.PUBLISHER_THUMBNAIL_URL,
                        Publishers.PUBLISHER_HERO_IMAGE_URL,
                        Publishers.UPDATED,
                        Publishers.WHITMORE_SCORE
                )
        )?.use {
            if (it.moveToFirst()) {
                CompanyEntity(
                        id = it.getInt(0),
                        name = it.getStringOrNull(1).orEmpty(),
                        description = it.getStringOrNull(2).orEmpty(),
                        imageUrl = it.getStringOrNull(3).orEmpty(),
                        thumbnailUrl = it.getStringOrNull(4).orEmpty(),
                        heroImageUrl = it.getStringOrNull(5).orEmpty(),
                        updatedTimestamp = it.getLongOrNull(6) ?: 0L,
                        whitmoreScore = it.getIntOrNull(7) ?: 0,
                )
            } else null
        }
    }

    suspend fun loadCollection(id: Int, sortBy: CollectionDao.SortType = CollectionDao.SortType.RATING): List<BriefGameEntity> {
        return collectionDao.loadLinkedCollection(Publishers.buildCollectionUri(id), sortBy)
    }

    fun savePublisher(item: CompanyItem?, updateTime: Long = System.currentTimeMillis()): Int {
        item?.let {
            val sortName = if (it.nameType == "primary") it.name.sortName(it.sortindex) else it.name
            val values = contentValuesOf(
                    Publishers.PUBLISHER_NAME to it.name,
                    Publishers.PUBLISHER_SORT_NAME to sortName,
                    Publishers.PUBLISHER_DESCRIPTION to it.description,
                    Publishers.PUBLISHER_IMAGE_URL to it.image,
                    Publishers.PUBLISHER_THUMBNAIL_URL to it.thumbnail,
                    Publishers.UPDATED to updateTime
            )
            return upsert(values, it.id.toIntOrNull() ?: BggContract.INVALID_ID)
        }
        return 0
    }

    fun update(publisherId: Int, values: ContentValues): Int {
        return context.contentResolver.update(Publishers.buildPublisherUri(publisherId), values, null, null)
    }

    private fun upsert(values: ContentValues, publisherId: Int): Int {
        val resolver = context.contentResolver
        val uri = Publishers.buildPublisherUri(publisherId)
        return if (resolver.rowExists(uri)) {
            val count = resolver.update(uri, values, null, null)
            Timber.d("Updated %,d publisher rows at %s", count, uri)
            count
        } else {
            values.put(Publishers.PUBLISHER_ID, publisherId)
            val insertedUri = resolver.insert(Publishers.CONTENT_URI, values)
            Timber.d("Inserted publisher at %s", insertedUri)
            1
        }
    }
}
