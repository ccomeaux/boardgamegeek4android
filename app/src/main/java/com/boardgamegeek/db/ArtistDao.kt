package com.boardgamegeek.db

import android.content.ContentValues
import androidx.core.content.contentValuesOf
import androidx.lifecycle.LiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.entities.ArtistEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.livedata.RegisteredLiveData
import com.boardgamegeek.model.Person
import com.boardgamegeek.provider.BggContract
import timber.log.Timber

class ArtistDao(private val context: BggApplication) {
    fun loadArtistAsLiveData(id: Int): LiveData<ArtistEntity> {
        return RegisteredLiveData(context, BggContract.Artists.buildArtistUri(id), true) {
            return@RegisteredLiveData loadArtist(id)
        }
    }

    private fun loadArtist(id: Int): ArtistEntity? {
        return context.contentResolver.load(
                BggContract.Artists.buildArtistUri(id),
                arrayOf(
                        BggContract.Artists.ARTIST_ID,
                        BggContract.Artists.ARTIST_NAME,
                        BggContract.Artists.ARTIST_DESCRIPTION,
                        BggContract.Artists.UPDATED
                )
        )?.use {
            if (it.moveToFirst()) {
                ArtistEntity(
                        it.getInt(BggContract.Artists.ARTIST_ID),
                        it.getStringOrEmpty(BggContract.Artists.ARTIST_NAME),
                        it.getStringOrEmpty(BggContract.Artists.ARTIST_DESCRIPTION),
                        it.getLongOrZero(BggContract.Artists.UPDATED)
                )
            } else null
        }
    }

    fun saveArtist(id: Int, artist: Person?, updateTime: Long = System.currentTimeMillis()): Int {
        if (artist != null && !artist.name.isNullOrBlank()) {
            val values = contentValuesOf(
                    BggContract.Artists.ARTIST_NAME to artist.name,
                    BggContract.Artists.ARTIST_DESCRIPTION to (if (artist.description == "This page does not exist. You can edit this page to create it.") "" else artist.description),
                    BggContract.Artists.UPDATED to updateTime
            )
            return upsert(values, id)
        }
        return 0
    }

    private fun upsert(values: ContentValues, artistId: Int): Int {
        val resolver = context.contentResolver
        val uri = BggContract.Artists.buildArtistUri(artistId)
        return if (resolver.rowExists(uri)) {
            val count = resolver.update(uri, values, null, null)
            Timber.d("Updated %,d artist rows at %s", count, uri)
            count
        } else {
            values.put(BggContract.Artists.ARTIST_ID, artistId)
            val insertedUri = resolver.insert(BggContract.Artists.CONTENT_URI, values)
            Timber.d("Inserted artist at %s", insertedUri)
            1
        }
    }
}