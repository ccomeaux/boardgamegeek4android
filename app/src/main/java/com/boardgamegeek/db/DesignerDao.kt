package com.boardgamegeek.db

import android.content.ContentValues
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.boardgamegeek.BggApplication
import com.boardgamegeek.entities.PersonEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract.Designers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

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
                Designers.DESIGNER_HERO_IMAGE_URL,
                Designers.ITEM_COUNT,
                Designers.WHITMORE_SCORE,
                Designers.DESIGNER_STATS_UPDATED_TIMESTAMP
            ),
            sortOrder = sortOrder
        )?.use {
            if (it.moveToFirst()) {
                do {
                    results += PersonEntity(
                        id = it.getInt(0),
                        name = it.getStringOrNull(1).orEmpty(),
                        description = it.getStringOrNull(2).orEmpty(),
                        updatedTimestamp = it.getLongOrNull(3) ?: 0L,
                        thumbnailUrl = it.getStringOrNull(4).orEmpty(),
                        heroImageUrl = it.getStringOrNull(5).orEmpty(),
                        itemCount = it.getIntOrNull(6) ?: 0,
                        whitmoreScore = it.getIntOrNull(7) ?: 0,
                        statsUpdatedTimestamp = it.getLongOrNull(8) ?: 0L,
                    )
                } while (it.moveToNext())
            }
        }
        results
    }

    suspend fun loadDesigner(id: Int): PersonEntity? = withContext(Dispatchers.IO) {
        context.contentResolver.load(
            Designers.buildDesignerUri(id),
            arrayOf(
                Designers.DESIGNER_ID,
                Designers.DESIGNER_NAME,
                Designers.DESIGNER_DESCRIPTION,
                Designers.UPDATED,
                Designers.WHITMORE_SCORE,
                Designers.DESIGNER_THUMBNAIL_URL,
                Designers.DESIGNER_IMAGE_URL,
                Designers.DESIGNER_HERO_IMAGE_URL,
                Designers.DESIGNER_STATS_UPDATED_TIMESTAMP,
                Designers.DESIGNER_IMAGES_UPDATED_TIMESTAMP,
            )
        )?.use {
            if (it.moveToFirst()) {
                PersonEntity(
                    id = it.getInt(0),
                    name = it.getStringOrNull(1).orEmpty(),
                    description = it.getStringOrNull(2).orEmpty(),
                    updatedTimestamp = it.getLongOrNull(3) ?: 0L,
                    whitmoreScore = it.getIntOrNull(4) ?: 0,
                    thumbnailUrl = it.getStringOrNull(5).orEmpty(),
                    imageUrl = it.getStringOrNull(6).orEmpty(),
                    heroImageUrl = it.getStringOrNull(7).orEmpty(),
                    statsUpdatedTimestamp = it.getLongOrNull(8) ?: 0L,
                    imagesUpdatedTimestamp = it.getLongOrNull(9) ?: 0L,
                )
            } else null
        }
    }

    suspend fun loadCollection(designerId: Int, sortBy: CollectionDao.SortType = CollectionDao.SortType.RATING) =
        collectionDao.loadLinkedCollection(Designers.buildDesignerCollectionUri(designerId), sortBy)

    suspend fun upsert(designerId: Int, values: ContentValues): Int = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val uri = Designers.buildDesignerUri(designerId)
        if (resolver.rowExists(uri)) {
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
