package com.boardgamegeek.repository

import android.content.ContentValues
import android.content.SharedPreferences
import androidx.core.content.contentValuesOf
import androidx.lifecycle.MutableLiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.db.ArtistDao
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.entities.BriefGameEntity
import com.boardgamegeek.entities.PersonEntity
import com.boardgamegeek.entities.PersonStatsEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.BggService
import com.boardgamegeek.mappers.mapToEntity
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.Artists
import com.boardgamegeek.util.ImageUtils.getImageId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.anko.collections.forEachWithIndex

class ArtistRepository(val application: BggApplication) {
    private val dao = ArtistDao(application)
    private val prefs: SharedPreferences by lazy { application.preferences() }

    suspend fun loadArtists(sortBy: ArtistDao.SortType): List<PersonEntity> {
        return dao.loadArtists(sortBy)
    }

    suspend fun loadArtist(artistId: Int): PersonEntity? {
        return dao.loadArtist(artistId)
    }

    suspend fun loadCollection(id: Int, sortBy: CollectionDao.SortType): List<BriefGameEntity> {
        return dao.loadCollection(id, sortBy)
    }

    suspend fun refreshArtist(artistId: Int): PersonEntity = withContext(Dispatchers.IO) {
        val response = Adapter.createForXml().person(BggService.PERSON_TYPE_ARTIST, artistId)
        if (!response.name.isNullOrBlank()) {
            val missingArtistMessage = "This page does not exist. You can edit this page to create it."
            val values = contentValuesOf(
                    Artists.ARTIST_NAME to response.name,
                    Artists.ARTIST_DESCRIPTION to (if (response.description == missingArtistMessage) "" else response.description),
                    Artists.UPDATED to System.currentTimeMillis(),
            )
            dao.upsert(artistId, values)
        }
        response.mapToEntity(artistId)
    }

    suspend fun refreshImages(artist: PersonEntity): PersonEntity = withContext(Dispatchers.IO) {
        val response = Adapter.createForXml().person(artist.id)
        response.items.firstOrNull()?.let {
            dao.upsert(artist.id, contentValuesOf(
                    Artists.ARTIST_THUMBNAIL_URL to it.thumbnail,
                    Artists.ARTIST_IMAGE_URL to it.image,
                    Artists.ARTIST_IMAGES_UPDATED_TIMESTAMP to System.currentTimeMillis(),
            ))
            artist.copy(thumbnailUrl = it.thumbnail.orEmpty(), imageUrl = it.image.orEmpty())
        } ?: artist
    }

    suspend fun refreshHeroImage(artist: PersonEntity): PersonEntity = withContext(Dispatchers.IO) {
        val response = Adapter.createGeekdoApi().image(artist.thumbnailUrl.getImageId())
        val url = response.images.medium.url
        dao.upsert(artist.id, contentValuesOf(BggContract.Designers.DESIGNER_HERO_IMAGE_URL to url))
        artist.copy(heroImageUrl = url)
    }

    suspend fun calculateWhitmoreScores(artists: List<PersonEntity>, progress: MutableLiveData<Pair<Int, Int>>) = withContext(Dispatchers.Default) {
        val sortedList = artists.sortedBy { it.statsUpdatedTimestamp }
        val maxProgress = sortedList.size
        sortedList.forEachWithIndex { i, data ->
            progress.postValue(i to maxProgress)
            val collection = dao.loadCollection(data.id)
            val statsEntity = PersonStatsEntity.fromLinkedCollection(collection, application)
            updateWhitmoreScore(data.id, statsEntity.whitmoreScore, data.whitmoreScore)
        }
        prefs[PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_ARTISTS] = System.currentTimeMillis()
        progress.postValue(0 to 0)
    }

    suspend fun calculateStats(artistId: Int): PersonStatsEntity = withContext(Dispatchers.Default) {
        val collection = withContext(Dispatchers.IO) {
            dao.loadCollection(artistId)
        }
        val linkedCollection = PersonStatsEntity.fromLinkedCollection(collection, application)
        updateWhitmoreScore(artistId, linkedCollection.whitmoreScore)
        linkedCollection
    }

    private suspend fun updateWhitmoreScore(id: Int, newScore: Int, oldScore: Int = -1) = withContext(Dispatchers.IO) {
        val realOldScore = if (oldScore == -1) dao.loadArtist(id)?.whitmoreScore ?: 0 else oldScore
        if (newScore != realOldScore) {
            dao.upsert(id, ContentValues().apply {
                put(Artists.WHITMORE_SCORE, newScore)
                put(Artists.ARTIST_STATS_UPDATED_TIMESTAMP, System.currentTimeMillis())
            })
        }
    }
}
