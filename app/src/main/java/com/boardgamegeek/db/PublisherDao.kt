package com.boardgamegeek.db

import android.content.ContentValues
import androidx.core.content.contentValuesOf
import androidx.lifecycle.LiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.entities.BriefGameEntity
import com.boardgamegeek.entities.CompanyEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.model.CompanyResponse2
import com.boardgamegeek.livedata.RegisteredLiveData
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.Publishers
import timber.log.Timber

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
                        Publishers.WHITMORE_SCORE
                ),
                sortOrder = sortOrder
        )?.use {
            if (it.moveToFirst()) {
                do {
                    results += CompanyEntity(
                            it.getInt(Publishers.PUBLISHER_ID),
                            it.getStringOrEmpty(Publishers.PUBLISHER_NAME),
                            it.getStringOrEmpty(Publishers.PUBLISHER_DESCRIPTION),
                            it.getStringOrEmpty(Publishers.PUBLISHER_IMAGE_URL),
                            it.getStringOrEmpty(Publishers.PUBLISHER_THUMBNAIL_URL),
                            it.getStringOrEmpty(Publishers.PUBLISHER_HERO_IMAGE_URL),
                            it.getLongOrZero(Publishers.UPDATED),
                            it.getIntOrZero(Publishers.ITEM_COUNT),
                            it.getIntOrZero(Publishers.WHITMORE_SCORE)
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
                        it.getInt(Publishers.PUBLISHER_ID),
                        it.getStringOrEmpty(Publishers.PUBLISHER_NAME),
                        it.getStringOrEmpty(Publishers.PUBLISHER_DESCRIPTION),
                        it.getStringOrEmpty(Publishers.PUBLISHER_IMAGE_URL),
                        it.getStringOrEmpty(Publishers.PUBLISHER_THUMBNAIL_URL),
                        it.getStringOrEmpty(Publishers.PUBLISHER_HERO_IMAGE_URL),
                        it.getLongOrZero(Publishers.UPDATED),
                        whitmoreScore = it.getIntOrZero(Publishers.WHITMORE_SCORE)
                )
            } else null
        }
    }

    fun savePublisher(publisher: CompanyResponse2?, updateTime: Long = System.currentTimeMillis()): Int {
        if (publisher != null) {
            publisher.items[0]?.let {
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
        }
        return 0
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