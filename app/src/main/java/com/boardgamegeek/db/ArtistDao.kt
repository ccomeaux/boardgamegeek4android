package com.boardgamegeek.db

import android.content.ContentValues
import android.content.Context
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.boardgamegeek.entities.PersonEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract.Artists
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class ArtistDao(private val context: Context) {
    private val collectionDao = CollectionDao(context)

    enum class SortType {
        NAME, ITEM_COUNT, WHITMORE_SCORE
    }

    suspend fun loadArtists(sortBy: SortType): List<PersonEntity> = withContext(Dispatchers.IO) {
        val sortByName = Artists.Columns.ARTIST_NAME.collateNoCase().ascending()
        val sortOrder = when (sortBy) {
            SortType.NAME -> sortByName
            SortType.ITEM_COUNT -> Artists.Columns.ITEM_COUNT.descending().plus(", $sortByName")
            SortType.WHITMORE_SCORE -> Artists.Columns.WHITMORE_SCORE.descending().plus(", $sortByName")
        }
        context.contentResolver.loadList(
            Artists.CONTENT_URI,
            arrayOf(
                Artists.Columns.ARTIST_ID,
                Artists.Columns.ARTIST_NAME,
                Artists.Columns.ARTIST_DESCRIPTION,
                Artists.Columns.UPDATED,
                Artists.Columns.ARTIST_THUMBNAIL_URL,
                Artists.Columns.ARTIST_HERO_IMAGE_URL,
                Artists.Columns.ITEM_COUNT,
                Artists.Columns.WHITMORE_SCORE,
                Artists.Columns.ARTIST_STATS_UPDATED_TIMESTAMP,
            ),
            sortOrder = sortOrder
        ) {
            PersonEntity(
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
        }
    }

    suspend fun loadArtist(id: Int): PersonEntity? = withContext(Dispatchers.IO) {
        context.contentResolver.loadEntity(
            Artists.buildArtistUri(id),
            arrayOf(
                Artists.Columns.ARTIST_ID,
                Artists.Columns.ARTIST_NAME,
                Artists.Columns.ARTIST_DESCRIPTION,
                Artists.Columns.UPDATED,
                Artists.Columns.WHITMORE_SCORE,
                Artists.Columns.ARTIST_THUMBNAIL_URL,
                Artists.Columns.ARTIST_IMAGE_URL,
                Artists.Columns.ARTIST_HERO_IMAGE_URL,
                Artists.Columns.ARTIST_STATS_UPDATED_TIMESTAMP,
                Artists.Columns.ARTIST_IMAGES_UPDATED_TIMESTAMP,
            )
        ) {
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
        }
    }

    suspend fun loadCollection(artistId: Int, sortBy: CollectionDao.SortType = CollectionDao.SortType.RATING) =
        collectionDao.loadLinkedCollection(Artists.buildArtistCollectionUri(artistId), sortBy)

    suspend fun upsert(artistId: Int, values: ContentValues): Int = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val uri = Artists.buildArtistUri(artistId)
        if (resolver.rowExists(uri)) {
            val count = resolver.update(uri, values, null, null)
            Timber.d("Updated %,d artist rows at %s", count, uri)
            count
        } else {
            values.put(Artists.Columns.ARTIST_ID, artistId)
            val insertedUri = resolver.insert(Artists.CONTENT_URI, values)
            Timber.d("Inserted artist at %s", insertedUri)
            1
        }
    }

    suspend fun delete(): Int = withContext(Dispatchers.IO) {
        context.contentResolver.delete(Artists.CONTENT_URI, null, null)
    }
}
