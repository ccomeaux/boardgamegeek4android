package com.boardgamegeek.db

import android.content.ContentValues
import androidx.core.content.contentValuesOf
import androidx.lifecycle.LiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.entities.BriefGameEntity
import com.boardgamegeek.entities.PersonEntity
import com.boardgamegeek.entities.PersonImagesEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.model.Person
import com.boardgamegeek.io.model.PersonResponse2
import com.boardgamegeek.livedata.RegisteredLiveData
import com.boardgamegeek.provider.BggContract.Designers
import timber.log.Timber

class DesignerDao(private val context: BggApplication) {
    private val collectionDao = CollectionDao(context)

    enum class SortType {
        NAME, ITEM_COUNT, WHITMORE_SCORE
    }

    fun loadDesignersAsLiveData(sortBy: SortType): LiveData<List<PersonEntity>> {
        return RegisteredLiveData(context, Designers.CONTENT_URI, true) {
            return@RegisteredLiveData loadDesigners(sortBy)
        }
    }

    private fun loadDesigners(sortBy: SortType): List<PersonEntity> {
        val results = arrayListOf<PersonEntity>()
        val sortByName = Designers.DESIGNER_NAME.collateNoCase().ascending()
        val sortOrder = when (sortBy) {
            SortType.NAME -> sortByName
            SortType.ITEM_COUNT -> Designers.ITEM_COUNT.descending().plus(", $sortByName")
            SortType.WHITMORE_SCORE -> Designers.WHITMORE_SCORE.descending().plus(", $sortByName")
        }
        context.contentResolver.load(
                Designers.CONTENT_URI,
                arrayOf(
                        Designers.DESIGNER_ID,
                        Designers.DESIGNER_NAME,
                        Designers.DESIGNER_DESCRIPTION,
                        Designers.UPDATED,
                        Designers.DESIGNER_THUMBNAIL_URL,
                        Designers.ITEM_COUNT,
                        Designers.WHITMORE_SCORE
                ),
                sortOrder = sortOrder
        )?.use {
            if (it.moveToFirst()) {
                do {
                    results += PersonEntity(
                            it.getInt(Designers.DESIGNER_ID),
                            it.getStringOrEmpty(Designers.DESIGNER_NAME),
                            it.getStringOrEmpty(Designers.DESIGNER_DESCRIPTION),
                            it.getLongOrZero(Designers.UPDATED),
                            it.getStringOrEmpty(Designers.DESIGNER_THUMBNAIL_URL),
                            it.getIntOrZero(Designers.ITEM_COUNT),
                            it.getIntOrZero(Designers.WHITMORE_SCORE)
                    )
                } while (it.moveToNext())
            }
        }
        return results
    }

    fun loadDesignerAsLiveData(id: Int): LiveData<PersonEntity> {
        return RegisteredLiveData(context, Designers.buildDesignerUri(id), true) {
            return@RegisteredLiveData loadDesigner(id)
        }
    }

    fun loadDesigner(id: Int): PersonEntity? {
        return context.contentResolver.load(
                Designers.buildDesignerUri(id),
                arrayOf(
                        Designers.DESIGNER_ID,
                        Designers.DESIGNER_NAME,
                        Designers.DESIGNER_DESCRIPTION,
                        Designers.UPDATED,
                        Designers.WHITMORE_SCORE
                )
        )?.use {
            if (it.moveToFirst()) {
                PersonEntity(
                        it.getInt(Designers.DESIGNER_ID),
                        it.getStringOrEmpty(Designers.DESIGNER_NAME),
                        it.getStringOrEmpty(Designers.DESIGNER_DESCRIPTION),
                        it.getLongOrZero(Designers.UPDATED),
                        whitmoreScore = it.getIntOrZero(Designers.WHITMORE_SCORE)
                )
            } else null
        }
    }

    fun saveDesigner(id: Int, designer: Person?, updateTime: Long = System.currentTimeMillis()): Int {
        if (designer != null && !designer.name.isNullOrBlank()) {
            val values = contentValuesOf(
                    Designers.DESIGNER_NAME to designer.name,
                    Designers.DESIGNER_DESCRIPTION to (if (designer.description == "This page does not exist. You can edit this page to create it.") "" else designer.description),
                    Designers.UPDATED to updateTime
            )
            return upsert(values, id)
        }
        return 0
    }

    fun loadDesignerImagesAsLiveData(id: Int): LiveData<PersonImagesEntity> {
        return RegisteredLiveData(context, Designers.buildDesignerUri(id), true) {
            return@RegisteredLiveData loadDesignerImages(id)
        }
    }

    private fun loadDesignerImages(id: Int): PersonImagesEntity? {
        return context.contentResolver.load(
                Designers.buildDesignerUri(id),
                arrayOf(
                        Designers.DESIGNER_ID,
                        Designers.DESIGNER_IMAGE_URL,
                        Designers.DESIGNER_THUMBNAIL_URL,
                        Designers.DESIGNER_HERO_IMAGE_URL,
                        Designers.DESIGNER_IMAGES_UPDATED_TIMESTAMP
                )
        )?.use {
            if (it.moveToFirst()) {
                PersonImagesEntity(
                        it.getInt(Designers.DESIGNER_ID),
                        it.getStringOrEmpty(Designers.DESIGNER_IMAGE_URL),
                        it.getStringOrEmpty(Designers.DESIGNER_THUMBNAIL_URL),
                        it.getStringOrEmpty(Designers.DESIGNER_HERO_IMAGE_URL),
                        it.getLongOrZero(Designers.DESIGNER_IMAGES_UPDATED_TIMESTAMP)
                )
            } else null
        }
    }

    fun saveDesignerImage(id: Int, designer: PersonResponse2?, updateTime: Long = System.currentTimeMillis()): Int {
        if (designer != null) {
            val values = contentValuesOf(
                    Designers.DESIGNER_IMAGE_URL to designer.items[0].image,
                    Designers.DESIGNER_THUMBNAIL_URL to designer.items[0].thumbnail,
                    Designers.DESIGNER_IMAGES_UPDATED_TIMESTAMP to updateTime
            )
            return upsert(values, id)
        }
        return 0
    }

    fun loadCollectionAsLiveData(id: Int, sortBy: CollectionDao.SortType = CollectionDao.SortType.RATING): LiveData<List<BriefGameEntity>> {
        val uri = Designers.buildDesignerCollectionUri(id)
        return RegisteredLiveData(context, uri, true) {
            return@RegisteredLiveData collectionDao.loadLinkedCollection(uri, sortBy)
        }
    }

    fun loadCollection(id: Int): List<BriefGameEntity> {
        val uri = Designers.buildDesignerCollectionUri(id)
        return collectionDao.loadLinkedCollection(uri)
    }

    fun update(designerId: Int, values: ContentValues): Int {
        return context.contentResolver.update(Designers.buildDesignerUri(designerId), values, null, null)
    }

    private fun upsert(values: ContentValues, designerId: Int): Int {
        val resolver = context.contentResolver
        val uri = Designers.buildDesignerUri(designerId)
        return if (resolver.rowExists(uri)) {
            val count = resolver.update(uri, values, null, null)
            Timber.d("Updated %,d designer rows at %s", count, uri)
            count
        } else {
            values.put(Designers.DESIGNER_ID, designerId)
            val insertedUri = resolver.insert(Designers.CONTENT_URI, values)
            Timber.d("Inserted designer at %s", insertedUri)
            1
        }
    }
}