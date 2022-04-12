package com.boardgamegeek.repository

import android.content.SharedPreferences
import androidx.core.content.contentValuesOf
import androidx.lifecycle.MutableLiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.db.ArtistDao
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.entities.PersonEntity
import com.boardgamegeek.entities.PersonStatsEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.BggService
import com.boardgamegeek.mappers.mapToEntity
import com.boardgamegeek.provider.BggContract.Artists
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ArtistRepository(val application: BggApplication) {
    private val dao = ArtistDao(application)
    private val prefs: SharedPreferences by lazy { application.preferences() }

    suspend fun loadArtists(sortBy: ArtistDao.SortType) = dao.loadArtists(sortBy)

    suspend fun loadArtist(artistId: Int) = dao.loadArtist(artistId)

    suspend fun loadCollection(id: Int, sortBy: CollectionDao.SortType) = dao.loadCollection(id, sortBy)

    suspend fun delete() = dao.delete()

    suspend fun refreshArtist(artistId: Int): PersonEntity = withContext(Dispatchers.IO) {
        val response = Adapter.createForXml().person(BggService.PERSON_TYPE_ARTIST, artistId)
        if (!response.name.isNullOrBlank()) {
            val missingArtistMessage = "This page does not exist. You can edit this page to create it."
            dao.upsert(
                artistId, contentValuesOf(
                    Artists.Columns.ARTIST_NAME to response.name,
                    Artists.Columns.ARTIST_DESCRIPTION to (if (response.description == missingArtistMessage) "" else response.description),
                    Artists.Columns.UPDATED to System.currentTimeMillis(),
                )
            )
        }
        response.mapToEntity(artistId)
    }

    suspend fun refreshImages(artist: PersonEntity): PersonEntity = withContext(Dispatchers.IO) {
        val response = Adapter.createForXml().person(artist.id)
        response.items.firstOrNull()?.let {
            dao.upsert(
                artist.id, contentValuesOf(
                    Artists.Columns.ARTIST_THUMBNAIL_URL to it.thumbnail,
                    Artists.Columns.ARTIST_IMAGE_URL to it.image,
                    Artists.Columns.ARTIST_IMAGES_UPDATED_TIMESTAMP to System.currentTimeMillis(),
                )
            )
            artist.copy(thumbnailUrl = it.thumbnail.orEmpty(), imageUrl = it.image.orEmpty())
        } ?: artist
    }

    suspend fun refreshHeroImage(artist: PersonEntity): PersonEntity = withContext(Dispatchers.IO) {
        val response = Adapter.createGeekdoApi().image(artist.thumbnailUrl.getImageId())
        val url = response.images.medium.url
        dao.upsert(artist.id, contentValuesOf(Artists.Columns.ARTIST_HERO_IMAGE_URL to url))
        artist.copy(heroImageUrl = url)
    }

    suspend fun calculateWhitmoreScores(artists: List<PersonEntity>, progress: MutableLiveData<Pair<Int, Int>>) = withContext(Dispatchers.Default) {
        val sortedList = artists.sortedBy { it.statsUpdatedTimestamp }
        val maxProgress = sortedList.size
        sortedList.forEachIndexed { i, data ->
            progress.postValue(i to maxProgress)
            calculateStats(data.id, data.whitmoreScore)
        }
        prefs[PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_ARTISTS] = System.currentTimeMillis()
        progress.postValue(0 to 0)
    }

    suspend fun calculateStats(artistId: Int, whitmoreScore: Int = -1): PersonStatsEntity = withContext(Dispatchers.Default) {
        val collection = dao.loadCollection(artistId)
        val statsEntity = PersonStatsEntity.fromLinkedCollection(collection, application)
        updateWhitmoreScore(artistId, statsEntity.whitmoreScore, whitmoreScore)
        statsEntity
    }

    private suspend fun updateWhitmoreScore(artistId: Int, newScore: Int, oldScore: Int = -1) = withContext(Dispatchers.IO) {
        val realOldScore = if (oldScore == -1) dao.loadArtist(artistId)?.whitmoreScore ?: 0 else oldScore
        if (newScore != realOldScore) {
            dao.upsert(
                artistId,
                contentValuesOf(
                    Artists.Columns.WHITMORE_SCORE to newScore,
                    Artists.Columns.ARTIST_STATS_UPDATED_TIMESTAMP to System.currentTimeMillis(),
                )
            )
        }
    }
}
