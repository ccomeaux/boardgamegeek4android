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
import com.boardgamegeek.provider.BggContract.Artists
import timber.log.Timber

class ArtistDao(private val context: BggApplication) {
    private val collectionDao = CollectionDao(context)

    enum class SortType {
        NAME, ITEM_COUNT, WHITMORE_SCORE
    }

    fun loadArtistsAsLiveData(sortBy: SortType): LiveData<List<PersonEntity>> {
        return RegisteredLiveData(context, Artists.CONTENT_URI, true) {
            return@RegisteredLiveData loadArtists(sortBy)
        }
    }

    private fun loadArtists(sortBy: SortType): List<PersonEntity> {
        val results = arrayListOf<PersonEntity>()
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
                        Artists.ITEM_COUNT,
                        Artists.WHITMORE_SCORE,
                        Artists.ARTIST_STATS_UPDATED_TIMESTAMP
                ),
                sortOrder = sortOrder
        )?.use {
            if (it.moveToFirst()) {
                do {
                    results += PersonEntity(
                            it.getInt(Artists.ARTIST_ID),
                            it.getStringOrEmpty(Artists.ARTIST_NAME),
                            it.getStringOrEmpty(Artists.ARTIST_DESCRIPTION),
                            it.getLongOrZero(Artists.UPDATED),
                            it.getStringOrEmpty(Artists.ARTIST_THUMBNAIL_URL),
                            it.getIntOrZero(Artists.ITEM_COUNT),
                            it.getIntOrZero(Artists.WHITMORE_SCORE),
                            it.getLongOrZero(Artists.ARTIST_STATS_UPDATED_TIMESTAMP)
                    )
                } while (it.moveToNext())
            }
        }
        return results
    }

    fun loadArtistAsLiveData(id: Int): LiveData<PersonEntity> {
        return RegisteredLiveData(context, Artists.buildArtistUri(id), true) {
            return@RegisteredLiveData loadArtist(id)
        }
    }

    fun loadArtist(id: Int): PersonEntity? {
        return context.contentResolver.load(
                Artists.buildArtistUri(id),
                arrayOf(
                        Artists.ARTIST_ID,
                        Artists.ARTIST_NAME,
                        Artists.ARTIST_DESCRIPTION,
                        Artists.UPDATED,
                        Artists.WHITMORE_SCORE
                )
        )?.use {
            if (it.moveToFirst()) {
                PersonEntity(
                        it.getInt(Artists.ARTIST_ID),
                        it.getStringOrEmpty(Artists.ARTIST_NAME),
                        it.getStringOrEmpty(Artists.ARTIST_DESCRIPTION),
                        it.getLongOrZero(Artists.UPDATED),
                        whitmoreScore = it.getIntOrZero(Artists.WHITMORE_SCORE)
                )
            } else null
        }
    }

    fun saveArtist(id: Int, artist: Person?, updateTime: Long = System.currentTimeMillis()): Int {
        if (artist != null && !artist.name.isNullOrBlank()) {
            val values = contentValuesOf(
                    Artists.ARTIST_NAME to artist.name,
                    Artists.ARTIST_DESCRIPTION to (if (artist.description == "This page does not exist. You can edit this page to create it.") "" else artist.description),
                    Artists.UPDATED to updateTime
            )
            return upsert(values, id)
        }
        return 0
    }

    fun loadArtistImagesAsLiveData(id: Int): LiveData<PersonImagesEntity> {
        return RegisteredLiveData(context, Artists.buildArtistUri(id), true) {
            return@RegisteredLiveData loadArtistImages(id)
        }
    }

    private fun loadArtistImages(id: Int): PersonImagesEntity? {
        return context.contentResolver.load(
                Artists.buildArtistUri(id),
                arrayOf(
                        Artists.ARTIST_ID,
                        Artists.ARTIST_IMAGE_URL,
                        Artists.ARTIST_THUMBNAIL_URL,
                        Artists.ARTIST_HERO_IMAGE_URL,
                        Artists.ARTIST_IMAGES_UPDATED_TIMESTAMP
                )
        )?.use {
            if (it.moveToFirst()) {
                PersonImagesEntity(
                        it.getInt(Artists.ARTIST_ID),
                        it.getStringOrEmpty(Artists.ARTIST_IMAGE_URL),
                        it.getStringOrEmpty(Artists.ARTIST_THUMBNAIL_URL),
                        it.getStringOrEmpty(Artists.ARTIST_HERO_IMAGE_URL),
                        it.getLongOrZero(Artists.ARTIST_IMAGES_UPDATED_TIMESTAMP)
                )
            } else null
        }
    }

    fun saveArtistImage(id: Int, artist: PersonResponse2?, updateTime: Long = System.currentTimeMillis()): Int {
        if (artist != null) {
            val values = contentValuesOf(
                    Artists.ARTIST_IMAGE_URL to artist.items[0].image,
                    Artists.ARTIST_THUMBNAIL_URL to artist.items[0].thumbnail,
                    Artists.ARTIST_IMAGES_UPDATED_TIMESTAMP to updateTime
            )
            return upsert(values, id)
        }
        return 0
    }

    fun loadCollectionAsLiveData(id: Int, sortBy: CollectionDao.SortType = CollectionDao.SortType.RATING): LiveData<List<BriefGameEntity>> {
        val uri = Artists.buildArtistCollectionUri(id)
        return RegisteredLiveData(context, uri, true) {
            return@RegisteredLiveData collectionDao.loadLinkedCollection(uri, sortBy)
        }
    }

    fun loadCollection(id: Int): List<BriefGameEntity> {
        val uri = Artists.buildArtistCollectionUri(id)
        return collectionDao.loadLinkedCollection(uri)
    }

    fun update(artistId: Int, values: ContentValues): Int {
        return context.contentResolver.update(Artists.buildArtistUri(artistId), values, null, null)
    }

    private fun upsert(values: ContentValues, artistId: Int): Int {
        val resolver = context.contentResolver
        val uri = Artists.buildArtistUri(artistId)
        return if (resolver.rowExists(uri)) {
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