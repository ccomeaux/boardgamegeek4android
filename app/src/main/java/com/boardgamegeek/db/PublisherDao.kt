package com.boardgamegeek.db

import android.content.ContentValues
import androidx.core.content.contentValuesOf
import androidx.lifecycle.LiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.entities.CompanyEntity
import com.boardgamegeek.entities.PersonGameEntity
import com.boardgamegeek.entities.YEAR_UNKNOWN
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.model.CompanyResponse2
import com.boardgamegeek.livedata.RegisteredLiveData
import com.boardgamegeek.provider.BggContract
import timber.log.Timber

class PublisherDao(private val context: BggApplication) {
    fun loadPublisherAsLiveData(id: Int): LiveData<CompanyEntity> {
        return RegisteredLiveData(context, BggContract.Publishers.buildPublisherUri(id), true) {
            return@RegisteredLiveData loadPublisher(id)
        }
    }

    private fun loadPublisher(id: Int): CompanyEntity? {
        return context.contentResolver.load(
                BggContract.Publishers.buildPublisherUri(id),
                arrayOf(
                        BggContract.Publishers.PUBLISHER_ID,
                        BggContract.Publishers.PUBLISHER_NAME,
                        BggContract.Publishers.PUBLISHER_DESCRIPTION,
                        BggContract.Publishers.PUBLISHER_IMAGE_URL,
                        BggContract.Publishers.PUBLISHER_THUMBNAIL_URL,
                        BggContract.Publishers.PUBLISHER_HERO_IMAGE_URL,
                        BggContract.Publishers.UPDATED
                )
        )?.use {
            if (it.moveToFirst()) {
                CompanyEntity(
                        it.getInt(BggContract.Publishers.PUBLISHER_ID),
                        it.getStringOrEmpty(BggContract.Publishers.PUBLISHER_NAME),
                        it.getStringOrEmpty(BggContract.Publishers.PUBLISHER_DESCRIPTION),
                        it.getStringOrEmpty(BggContract.Publishers.PUBLISHER_IMAGE_URL),
                        it.getStringOrEmpty(BggContract.Publishers.PUBLISHER_THUMBNAIL_URL),
                        it.getStringOrEmpty(BggContract.Publishers.PUBLISHER_HERO_IMAGE_URL),
                        it.getLongOrZero(BggContract.Publishers.UPDATED)
                )
            } else null
        }
    }

    fun savePublisher(publisher: CompanyResponse2?, updateTime: Long = System.currentTimeMillis()): Int {
        if (publisher != null) {
            publisher.items[0]?.let {
                val sortName = if (it.nameType == "primary") it.name.sortName(it.sortindex) else it.name
                val values = contentValuesOf(
                        BggContract.Publishers.PUBLISHER_NAME to it.name,
                        BggContract.Publishers.PUBLISHER_SORT_NAME to sortName,
                        BggContract.Publishers.PUBLISHER_DESCRIPTION to it.description,
                        BggContract.Publishers.PUBLISHER_IMAGE_URL to it.image,
                        BggContract.Publishers.PUBLISHER_THUMBNAIL_URL to it.thumbnail,
                        BggContract.Publishers.UPDATED to updateTime
                )
                return upsert(values, it.id.toIntOrNull() ?: BggContract.INVALID_ID)
            }
        }
        return 0
    }

    fun loadCollectionAsLiveData(id: Int): LiveData<List<PersonGameEntity>>? {
        return RegisteredLiveData(context, BggContract.Publishers.buildCollectionUri(id), true) {
            return@RegisteredLiveData loadCollection(id)
        }
    }

    private fun loadCollection(publisherId: Int): List<PersonGameEntity> {
        val list = arrayListOf<PersonGameEntity>()
        context.contentResolver.load(
                BggContract.Publishers.buildCollectionUri(publisherId),
                arrayOf(
                        "games." + BggContract.Collection.GAME_ID,
                        BggContract.Collection.GAME_NAME,
                        BggContract.Collection.COLLECTION_NAME,
                        BggContract.Collection.COLLECTION_YEAR_PUBLISHED,
                        BggContract.Collection.COLLECTION_THUMBNAIL_URL,
                        BggContract.Collection.THUMBNAIL_URL,
                        BggContract.Collection.HERO_IMAGE_URL
                )
        )?.use {
            if (it.moveToFirst()) {
                do {
                    list += PersonGameEntity(
                            it.getInt(BggContract.Collection.GAME_ID),
                            it.getStringOrEmpty(BggContract.Collection.GAME_NAME),
                            it.getStringOrEmpty(BggContract.Collection.COLLECTION_NAME),
                            it.getIntOrNull(BggContract.Collection.COLLECTION_YEAR_PUBLISHED) ?: YEAR_UNKNOWN,
                            it.getStringOrEmpty(BggContract.Collection.COLLECTION_THUMBNAIL_URL),
                            it.getStringOrEmpty(BggContract.Collection.THUMBNAIL_URL),
                            it.getStringOrEmpty(BggContract.Collection.HERO_IMAGE_URL)
                    )
                } while (it.moveToNext())
            }
        }
        return list
    }

    fun update(publisherId: Int, values: ContentValues): Int {
        return context.contentResolver.update(BggContract.Publishers.buildPublisherUri(publisherId), values, null, null)
    }

    private fun upsert(values: ContentValues, publisherId: Int): Int {
        val resolver = context.contentResolver
        val uri = BggContract.Publishers.buildPublisherUri(publisherId)
        return if (resolver.rowExists(uri)) {
            val count = resolver.update(uri, values, null, null)
            Timber.d("Updated %,d publisher rows at %s", count, uri)
            count
        } else {
            values.put(BggContract.Publishers.PUBLISHER_ID, publisherId)
            val insertedUri = resolver.insert(BggContract.Publishers.CONTENT_URI, values)
            Timber.d("Inserted publisher at %s", insertedUri)
            1
        }
    }
}