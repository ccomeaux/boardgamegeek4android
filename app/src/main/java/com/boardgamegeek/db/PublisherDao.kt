package com.boardgamegeek.db

import android.content.ContentValues
import androidx.core.content.contentValuesOf
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.lifecycle.LiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.entities.BriefGameEntity
import com.boardgamegeek.entities.CompanyEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.model.CompanyItem
import com.boardgamegeek.livedata.RegisteredLiveData
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.Publishers
import timber.log.Timber
import kotlin.io.use

class PublisherDao(private val context: BggApplication) {
    private val collectionDao = CollectionDao(context)

    enum class SortType {
        NAME, ITEM_COUNT, WHITMORE_SCORE
    }

    fun loadPublishersAsLiveData(sortBy: SortType): LiveData<List<CompanyEntity>> {
        return RegisteredLiveData(context, Publishers.CONTENT_URI, true) {
            return@RegisteredLiveData loadPublishers(sortBy)
        }
    }

    private fun loadPublishers(sortBy: SortType): List<CompanyEntity> {
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
                            it.getInt(0),
                            it.getStringOrNull(1).orEmpty(),
                            it.getStringOrNull(2).orEmpty(),
                            it.getStringOrNull(3).orEmpty(),
                            it.getStringOrNull(4).orEmpty(),
                            it.getStringOrNull(5).orEmpty(),
                            it.getLongOrNull(6) ?: 0L,
                            it.getIntOrNull(7) ?: 0,
                            it.getIntOrNull(8) ?: 0,
                            it.getLongOrNull(9) ?: 0L,
                    )
                } while (it.moveToNext())
            }
        }
        return results
    }

    fun loadPublisherAsLiveData(id: Int): LiveData<CompanyEntity> {
        return RegisteredLiveData(context, Publishers.buildPublisherUri(id), true) {
            return@RegisteredLiveData loadPublisher(id)
        }
    }

    fun loadPublisher(id: Int): CompanyEntity? {
        return context.contentResolver.load(
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
                        it.getInt(0),
                        it.getStringOrNull(1).orEmpty(),
                        it.getStringOrNull(2).orEmpty(),
                        it.getStringOrNull(3).orEmpty(),
                        it.getStringOrNull(4).orEmpty(),
                        it.getStringOrNull(5).orEmpty(),
                        it.getLongOrNull(6) ?: 0L,
                        whitmoreScore = it.getIntOrNull(7) ?: 0,
                )
            } else null
        }
    }

    fun loadCollectionAsLiveData(id: Int, sortBy: CollectionDao.SortType = CollectionDao.SortType.RATING): LiveData<List<BriefGameEntity>> {
        val uri = Publishers.buildCollectionUri(id)
        return RegisteredLiveData(context, uri, true) {
            return@RegisteredLiveData collectionDao.loadLinkedCollection(uri, sortBy)
        }
    }

    fun loadCollection(id: Int): List<BriefGameEntity> {
        val uri = Publishers.buildCollectionUri(id)
        return collectionDao.loadLinkedCollection(uri)
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
