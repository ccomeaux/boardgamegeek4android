package com.boardgamegeek.db

import android.content.ContentValues
import androidx.core.content.contentValuesOf
import androidx.lifecycle.LiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.entities.BriefGameEntity
import com.boardgamegeek.entities.PersonEntity
import com.boardgamegeek.entities.PersonImagesEntity
import com.boardgamegeek.entities.YEAR_UNKNOWN
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.model.Person
import com.boardgamegeek.io.model.PersonResponse2
import com.boardgamegeek.livedata.RegisteredLiveData
import com.boardgamegeek.provider.BggContract
import timber.log.Timber

class ArtistDao(private val context: BggApplication) {
    enum class SortType {
        NAME, ITEM_COUNT
    }

    fun loadArtistsAsLiveData(sortBy: SortType = SortType.NAME): LiveData<List<PersonEntity>> {
        return RegisteredLiveData(context, BggContract.Artists.CONTENT_URI, true) {
            return@RegisteredLiveData loadArtists(sortBy)
        }
    }

    private fun loadArtists(sortBy: SortType): List<PersonEntity> {
        val results = arrayListOf<PersonEntity>()
        val sortByName = BggContract.Artists.ARTIST_NAME.collateNoCase().ascending()
        val sortOrder = when (sortBy) {
            SortType.NAME -> sortByName
            SortType.ITEM_COUNT -> BggContract.Artists.ITEM_COUNT.descending().plus(", $sortByName")
        }
        context.contentResolver.load(
                BggContract.Artists.CONTENT_URI,
                arrayOf(
                        BggContract.Artists.ARTIST_ID,
                        BggContract.Artists.ARTIST_NAME,
                        BggContract.Artists.ARTIST_DESCRIPTION,
                        BggContract.Artists.UPDATED,
                        BggContract.Artists.ARTIST_THUMBNAIL_URL,
                        BggContract.Artists.ITEM_COUNT
                ),
                sortOrder = sortOrder
        )?.use {
            if (it.moveToFirst()) {
                do {
                    results += PersonEntity(
                            it.getInt(BggContract.Artists.ARTIST_ID),
                            it.getStringOrEmpty(BggContract.Artists.ARTIST_NAME),
                            it.getStringOrEmpty(BggContract.Artists.ARTIST_DESCRIPTION),
                            it.getLongOrZero(BggContract.Artists.UPDATED),
                            it.getStringOrEmpty(BggContract.Artists.ARTIST_THUMBNAIL_URL),
                            it.getIntOrZero(BggContract.Artists.ITEM_COUNT)
                    )
                } while (it.moveToNext())
            }
        }
        return results
    }

    fun loadArtistAsLiveData(id: Int): LiveData<PersonEntity> {
        return RegisteredLiveData(context, BggContract.Artists.buildArtistUri(id), true) {
            return@RegisteredLiveData loadArtist(id)
        }
    }

    private fun loadArtist(id: Int): PersonEntity? {
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
                PersonEntity(
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

    fun loadArtistImagesAsLiveData(id: Int): LiveData<PersonImagesEntity> {
        return RegisteredLiveData(context, BggContract.Artists.buildArtistUri(id), true) {
            return@RegisteredLiveData loadArtistImages(id)
        }
    }

    private fun loadArtistImages(id: Int): PersonImagesEntity? {
        return context.contentResolver.load(
                BggContract.Artists.buildArtistUri(id),
                arrayOf(
                        BggContract.Artists.ARTIST_ID,
                        BggContract.Artists.ARTIST_IMAGE_URL,
                        BggContract.Artists.ARTIST_THUMBNAIL_URL,
                        BggContract.Artists.ARTIST_HERO_IMAGE_URL,
                        BggContract.Artists.ARTIST_IMAGES_UPDATED_TIMESTAMP
                )
        )?.use {
            if (it.moveToFirst()) {
                PersonImagesEntity(
                        it.getInt(BggContract.Artists.ARTIST_ID),
                        it.getStringOrEmpty(BggContract.Artists.ARTIST_IMAGE_URL),
                        it.getStringOrEmpty(BggContract.Artists.ARTIST_THUMBNAIL_URL),
                        it.getStringOrEmpty(BggContract.Artists.ARTIST_HERO_IMAGE_URL),
                        it.getLongOrZero(BggContract.Artists.ARTIST_IMAGES_UPDATED_TIMESTAMP)
                )
            } else null
        }
    }

    fun saveArtistImage(id: Int, artist: PersonResponse2?, updateTime: Long = System.currentTimeMillis()): Int {
        if (artist != null) {
            val values = contentValuesOf(
                    BggContract.Artists.ARTIST_IMAGE_URL to artist.items[0].image,
                    BggContract.Artists.ARTIST_THUMBNAIL_URL to artist.items[0].thumbnail,
                    BggContract.Artists.ARTIST_IMAGES_UPDATED_TIMESTAMP to updateTime
            )
            return upsert(values, id)
        }
        return 0
    }

    fun loadCollectionAsLiveData(id: Int): LiveData<List<BriefGameEntity>>? {
        return RegisteredLiveData(context, BggContract.Artists.buildArtistCollectionUri(id), true) {
            return@RegisteredLiveData loadCollection(id)
        }
    }

    private fun loadCollection(artistId: Int): List<BriefGameEntity> {
        val list = arrayListOf<BriefGameEntity>()
        context.contentResolver.load(
                BggContract.Artists.buildArtistCollectionUri(artistId),
                arrayOf(
                        "games." + BggContract.Collection.GAME_ID,
                        BggContract.Collection.GAME_NAME,
                        BggContract.Collection.COLLECTION_NAME,
                        BggContract.Collection.COLLECTION_YEAR_PUBLISHED,
                        BggContract.Collection.COLLECTION_THUMBNAIL_URL,
                        BggContract.Collection.THUMBNAIL_URL,
                        BggContract.Collection.HERO_IMAGE_URL
                ),
                sortOrder = BggContract.Collection.GAME_SORT_NAME.collateNoCase().ascending()
        )?.use {
            if (it.moveToFirst()) {
                do {
                    list += BriefGameEntity(
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

    fun update(artistId: Int, values: ContentValues): Int {
        return context.contentResolver.update(BggContract.Artists.buildArtistUri(artistId), values, null, null)
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