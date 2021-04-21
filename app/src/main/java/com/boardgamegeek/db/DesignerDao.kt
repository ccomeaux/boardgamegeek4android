package com.boardgamegeek.db

import android.content.ContentValues
import androidx.core.content.contentValuesOf
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.lifecycle.LiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.entities.BriefGameEntity
import com.boardgamegeek.entities.PersonEntity
import com.boardgamegeek.entities.PersonImagesEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.model.Person
import com.boardgamegeek.io.model.PersonResponse
import com.boardgamegeek.livedata.RegisteredLiveData
import com.boardgamegeek.provider.BggContract.Designers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.io.use

class DesignerDao(private val context: BggApplication) {
    private val collectionDao = CollectionDao(context)

    enum class SortType {
        NAME, ITEM_COUNT, WHITMORE_SCORE
    }

    suspend fun loadDesigners(sortBy: SortType): List<PersonEntity> = withContext(Dispatchers.IO) {
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
                        Designers.WHITMORE_SCORE,
                        Designers.DESIGNER_STATS_UPDATED_TIMESTAMP
                ),
                sortOrder = sortOrder
        )?.use {
            if (it.moveToFirst()) {
                do {
                    results += PersonEntity(
                            it.getInt(0),
                            it.getStringOrNull(1).orEmpty(),
                            it.getStringOrNull(2).orEmpty(),
                            it.getLongOrNull(3) ?: 0L,
                            it.getStringOrNull(4).orEmpty(),
                            it.getIntOrNull(5) ?: 0,
                            it.getIntOrNull(6) ?: 0,
                            it.getLongOrNull(7) ?: 0L,
                    )
                } while (it.moveToNext())
            }
        }
        results
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
                        it.getInt(0),
                        it.getStringOrNull(1).orEmpty(),
                        it.getStringOrNull(2).orEmpty(),
                        it.getLongOrNull(3) ?: 0L,
                        whitmoreScore = it.getIntOrNull(4) ?: 0,
                )
            } else null
        }
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
                        it.getInt(0),
                        it.getStringOrNull(1).orEmpty(),
                        it.getStringOrNull(2).orEmpty(),
                        it.getStringOrNull(3).orEmpty(),
                        it.getLongOrNull(4) ?: 0L,
                )
            } else null
        }
    }

    suspend fun loadCollection(id: Int, sortBy: CollectionDao.SortType = CollectionDao.SortType.RATING): List<BriefGameEntity> {
        return collectionDao.loadLinkedCollection(Designers.buildDesignerCollectionUri(id), sortBy)
    }

    fun saveDesigner(id: Int, designer: Person?, updateTime: Long = System.currentTimeMillis()): Int {
        if (designer != null && !designer.name.isNullOrBlank()) {
            val missingDesignerMessage = "This page does not exist. You can edit this page to create it."
            val values = contentValuesOf(
                    Designers.DESIGNER_NAME to designer.name,
                    Designers.DESIGNER_DESCRIPTION to (if (designer.description == missingDesignerMessage) "" else designer.description),
                    Designers.UPDATED to updateTime
            )
            return upsert(values, id)
        }
        return 0
    }

    fun saveDesignerImage(id: Int, designer: PersonResponse?, updateTime: Long = System.currentTimeMillis()): Int {
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
