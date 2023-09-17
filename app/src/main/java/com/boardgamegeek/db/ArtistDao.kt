package com.boardgamegeek.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.provider.BaseColumns
import androidx.core.content.contentValuesOf
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.boardgamegeek.db.model.ArtistBasic
import com.boardgamegeek.db.model.ArtistImages
import com.boardgamegeek.db.model.ArtistLocal
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract.Artists
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class ArtistDao(private val context: Context) {
    enum class SortType {
        NAME, ITEM_COUNT, WHITMORE_SCORE
    }

    suspend fun loadArtists(sortBy: SortType): List<ArtistLocal> = withContext(Dispatchers.IO) {
        val sortByName = Artists.Columns.ARTIST_NAME.collateNoCase().ascending()
        val sortOrder = when (sortBy) {
            SortType.NAME -> sortByName
            SortType.ITEM_COUNT -> Artists.Columns.ITEM_COUNT.descending().plus(", $sortByName")
            SortType.WHITMORE_SCORE -> Artists.Columns.WHITMORE_SCORE.descending().plus(", $sortByName")
        }
        context.contentResolver.loadList(
            Artists.CONTENT_URI,
            projection(),
            sortOrder = sortOrder
        ) {
            buildFromCursor(it)
        }
    }

    suspend fun loadArtist(id: Int): ArtistLocal? = withContext(Dispatchers.IO) {
        context.contentResolver.loadEntity(
            Artists.buildArtistUri(id),
            projection()
        ) {
            buildFromCursor(it)
        }
    }

    private fun projection() = arrayOf(
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
        BaseColumns._ID,
        Artists.Columns.ITEM_COUNT,
    )

    private fun buildFromCursor(it: Cursor) = ArtistLocal(
        internalId = it.getInt(10),
        artistId = it.getInt(0),
        artistName = it.getStringOrNull(1).orEmpty(),
        artistDescription = it.getStringOrNull(2),
        updatedTimestamp = it.getLongOrNull(3),
        whitmoreScore = it.getIntOrNull(4),
        artistThumbnailUrl = it.getStringOrNull(5),
        artistImageUrl = it.getStringOrNull(6),
        artistHeroImageUrl = it.getStringOrNull(7),
        statsUpdatedTimestamp = it.getLongOrNull(8),
        imagesUpdatedTimestamp = it.getLongOrNull(9),
        itemCount = it.getIntOrNull(11),
    )

    suspend fun upsert(artist: ArtistBasic): Int = withContext(Dispatchers.IO) {
        val values = contentValuesOf(
            Artists.Columns.ARTIST_NAME to artist.artistName,
            Artists.Columns.ARTIST_DESCRIPTION to artist.artistDescription,
            Artists.Columns.UPDATED to artist.updatedTimestamp,
        )
        upsert(artist.artistId, values)
    }

    suspend fun upsert(artist: ArtistImages): Int = withContext(Dispatchers.IO) {
        val values = contentValuesOf(
            Artists.Columns.ARTIST_IMAGE_URL to artist.imageUrl,
            Artists.Columns.ARTIST_THUMBNAIL_URL to artist.thumbnailUrl,
            Artists.Columns.ARTIST_IMAGES_UPDATED_TIMESTAMP to artist.updatedTimestamp,
        )
        upsert(artist.artistId, values)
    }

    suspend fun updateHeroImageUrl(artistId: Int, url: String): Int = withContext(Dispatchers.IO) {
        val values = contentValuesOf(
            Artists.Columns.ARTIST_HERO_IMAGE_URL to url,
        )
        upsert(artistId, values)
    }

    suspend fun updateWhitmoreScore(artistId: Int, score: Int): Int = withContext(Dispatchers.IO) {
        val values = contentValuesOf(
            Artists.Columns.WHITMORE_SCORE to score,
            Artists.Columns.ARTIST_STATS_UPDATED_TIMESTAMP to System.currentTimeMillis(),
        )
        upsert(artistId, values)
    }

    private suspend fun upsert(artistId: Int, values: ContentValues): Int = withContext(Dispatchers.IO) {
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
