package com.boardgamegeek.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.provider.BaseColumns
import androidx.core.content.contentValuesOf
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.boardgamegeek.db.model.*
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract.Designers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class DesignerDao(private val context: Context) {
    private val collectionDao = CollectionDao(context)

    enum class SortType {
        NAME, ITEM_COUNT, WHITMORE_SCORE
    }

    suspend fun loadDesigners(sortBy: SortType): List<DesignerLocal> = withContext(Dispatchers.IO) {
        val sortByName = Designers.Columns.DESIGNER_NAME.collateNoCase().ascending()
        val sortOrder = when (sortBy) {
            SortType.NAME -> sortByName
            SortType.ITEM_COUNT -> Designers.Columns.ITEM_COUNT.descending().plus(", $sortByName")
            SortType.WHITMORE_SCORE -> Designers.Columns.WHITMORE_SCORE.descending().plus(", $sortByName")
        }
        context.contentResolver.loadList(
            Designers.CONTENT_URI,
            projection(),
            sortOrder = sortOrder
        ) {
            fromCursor(it)
        }
    }

    suspend fun loadDesigner(id: Int): DesignerLocal? = withContext(Dispatchers.IO) {
        context.contentResolver.loadEntity(
            Designers.buildDesignerUri(id),
            projection()
        ) {
            fromCursor(it)
        }
    }

    private fun projection() = arrayOf(
        Designers.Columns.DESIGNER_ID,
        Designers.Columns.DESIGNER_NAME,
        Designers.Columns.DESIGNER_DESCRIPTION,
        Designers.Columns.UPDATED,
        Designers.Columns.DESIGNER_THUMBNAIL_URL,
        Designers.Columns.DESIGNER_HERO_IMAGE_URL,
        Designers.Columns.DESIGNER_IMAGES_UPDATED_TIMESTAMP,
        Designers.Columns.WHITMORE_SCORE,
        Designers.Columns.DESIGNER_STATS_UPDATED_TIMESTAMP,
        Designers.Columns.DESIGNER_IMAGE_URL,
        BaseColumns._ID,
        Designers.Columns.ITEM_COUNT,
    )

    private fun fromCursor(it: Cursor) = DesignerLocal(
        internalId = it.getInt(10),
        designerId = it.getInt(0),
        designerName = it.getString(1),
        designerDescription = it.getStringOrNull(2),
        updatedTimestamp = it.getLongOrNull(3) ?: 0,
        designerImageUrl = it.getStringOrNull(9),
        designerThumbnailUrl = it.getStringOrNull(4),
        designerHeroImageUrl = it.getStringOrNull(5),
        imagesUpdatedTimestamp = it.getLongOrNull(6),
        whitmoreScore = it.getIntOrNull(7),
        statsUpdatedTimestamp = it.getLongOrNull(8),
        itemCount = it.getIntOrNull(11),
    )

    suspend fun loadCollection(designerId: Int, sortBy: CollectionDao.SortType = CollectionDao.SortType.RATING) =
        collectionDao.loadLinkedCollection(Designers.buildDesignerCollectionUri(designerId), sortBy)

    suspend fun upsert(designer: DesignerBasic): Int = withContext(Dispatchers.IO) {
        val values = contentValuesOf(
            Designers.Columns.DESIGNER_NAME to designer.designerName,
            Designers.Columns.DESIGNER_DESCRIPTION to designer.designerDescription,
            Designers.Columns.UPDATED to designer.updatedTimestamp,
        )
        upsert(designer.designerId, values)
    }

    suspend fun upsert(designer: DesignerImages): Int = withContext(Dispatchers.IO) {
        val values = contentValuesOf(
            Designers.Columns.DESIGNER_IMAGE_URL to designer.imageUrl,
            Designers.Columns.DESIGNER_THUMBNAIL_URL to designer.thumbnailUrl,
            Designers.Columns.DESIGNER_IMAGES_UPDATED_TIMESTAMP to designer.updatedTimestamp,
        )
        upsert(designer.designerId, values)
    }

    suspend fun updateHeroImageUrl(designerId: Int, url: String): Int = withContext(Dispatchers.IO) {
        val values = contentValuesOf(
            Designers.Columns.DESIGNER_HERO_IMAGE_URL to url,
        )
        upsert(designerId, values)
    }

    suspend fun updateWhitmoreScore(designerId: Int, score: Int): Int = withContext(Dispatchers.IO) {
        val values = contentValuesOf(
            Designers.Columns.WHITMORE_SCORE to score,
            Designers.Columns.DESIGNER_STATS_UPDATED_TIMESTAMP to System.currentTimeMillis(),
        )
        upsert(designerId, values)
    }

    suspend fun upsert(designerId: Int, values: ContentValues): Int = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val uri = Designers.buildDesignerUri(designerId)
        if (resolver.rowExists(uri)) {
            val count = resolver.update(uri, values, null, null)
            Timber.d("Updated %,d designer rows at %s", count, uri)
            count
        } else {
            values.put(Designers.Columns.DESIGNER_ID, designerId)
            val insertedUri = resolver.insert(Designers.CONTENT_URI, values)
            Timber.d("Inserted designer at %s", insertedUri)
            1
        }
    }

    suspend fun delete(): Int = withContext(Dispatchers.IO) {
        context.contentResolver.delete(Designers.CONTENT_URI, null, null)
    }
}
