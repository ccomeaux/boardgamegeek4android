package com.boardgamegeek.db

import android.content.ContentValues
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.boardgamegeek.BggApplication
import com.boardgamegeek.entities.PersonEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract.Artists
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class ArtistDao(private val context: BggApplication) {
    private val collectionDao = CollectionDao(context)

    enum class SortType {
        NAME, ITEM_COUNT, WHITMORE_SCORE
    }

    suspend fun loadArtists(sortBy: SortType): List<PersonEntity> = withContext(Dispatchers.IO) {
        val results = mutableListOf<PersonEntity>()
        val sortByName = Artists.ARTIST_NAME.collateNoCase().ascending()
        val sortOrder = when (sortBy) {
            SortType.NAME -> sortByName
            SortType.ITEM_COUNT -> Artists.ITEM_COUNT.descending().plus(", $sortByName")
            SortType.WHITMORE_SCORE -> Artists.WHITMORE_SCORE.descending().plus(", $sortByName")
        }
        context.contentResolver.load(
            Artists.CONTENT_URI,
            arrayOf(
                Artists.ARTIST_ID,
                Artists.ARTIST_NAME,
                Artists.ARTIST_DESCRIPTION,
                Artists.UPDATED,
                Artists.ARTIST_THUMBNAIL_URL,
                Artists.ARTIST_HERO_IMAGE_URL,
                Artists.ITEM_COUNT,
                Artists.WHITMORE_SCORE,
                Artists.ARTIST_STATS_UPDATED_TIMESTAMP
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

    suspend fun loadArtist(id: Int): PersonEntity? = withContext(Dispatchers.IO) {
        context.contentResolver.load(
            Artists.buildArtistUri(id),
            arrayOf(
                Artists.ARTIST_ID,
                Artists.ARTIST_NAME,
                Artists.ARTIST_DESCRIPTION,
                Artists.UPDATED,
                Artists.WHITMORE_SCORE,
                Artists.ARTIST_THUMBNAIL_URL,
                Artists.ARTIST_IMAGE_URL,
                Artists.ARTIST_HERO_IMAGE_URL,
                Artists.ARTIST_STATS_UPDATED_TIMESTAMP,
                Artists.ARTIST_IMAGES_UPDATED_TIMESTAMP,
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
            values.put(Artists.ARTIST_ID, artistId)
            val insertedUri = resolver.insert(Artists.CONTENT_URI, values)
            Timber.d("Inserted artist at %s", insertedUri)
            1
        }
    }
}
