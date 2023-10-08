package com.boardgamegeek.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import com.boardgamegeek.db.ArtistDao
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.entities.CollectionItem.Companion.filterBySyncedStatues
import com.boardgamegeek.entities.Person
import com.boardgamegeek.entities.PersonStats
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.BggService
import com.boardgamegeek.mappers.mapToModel
import com.boardgamegeek.mappers.mapToArtistImages
import com.boardgamegeek.mappers.mapToArtistBasic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ArtistRepository(
    val context: Context,
    private val api: BggService,
    private val imageRepository: ImageRepository,
) {
    private val artistDao = ArtistDao(context)
    private val collectionDao = CollectionDao(context)
    private val prefs: SharedPreferences by lazy { context.preferences() }

    suspend fun loadArtists(sortBy: ArtistDao.SortType) = artistDao.loadArtists(sortBy).map { it.mapToModel() }

    suspend fun loadArtist(artistId: Int) = artistDao.loadArtist(artistId)?.mapToModel()

    suspend fun loadCollection(id: Int, sortBy: CollectionDao.SortType) =
        collectionDao.loadCollectionForArtist(id, sortBy)
            .map { it.mapToModel() }
            .filter { it.deleteTimestamp == 0L }
            .filter { it.filterBySyncedStatues(context) }

    suspend fun delete() = artistDao.delete()

    suspend fun refreshArtist(artistId: Int): Int = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val response = api.person(BggService.PersonType.ARTIST, artistId)
        response.mapToModel(artistId, timestamp)?.let {
            artistDao.upsert(it.mapToArtistBasic())
        } ?: 0
    }

    suspend fun refreshImages(artist: Person): Person = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val response = api.person(artist.id)
        val person = response.items.firstOrNull()?.mapToModel(artist, timestamp)
        person?.let {
            artistDao.upsert(it.mapToArtistImages())
        }
        person ?: artist
    }

    suspend fun refreshHeroImage(artist: Person): Person = withContext(Dispatchers.IO) {
        val urlMap = imageRepository.fetchImageUrls(artist.thumbnailUrl.getImageId())
        val urls = urlMap[ImageRepository.ImageType.HERO]
        if (urls?.isNotEmpty() == true) {
            artistDao.updateHeroImageUrl(artist.id, urls.first())
            artist.copy(heroImageUrl = urls.first())
        } else artist
    }

    suspend fun calculateWhitmoreScores(artists: List<Person>, progress: MutableLiveData<Pair<Int, Int>>) = withContext(Dispatchers.Default) {
        val sortedList = artists.sortedBy { it.statsUpdatedTimestamp }
        val maxProgress = sortedList.size
        sortedList.forEachIndexed { i, data ->
            progress.postValue(i to maxProgress)
            calculateStats(data.id, data.whitmoreScore)
        }
        prefs[PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_ARTISTS] = System.currentTimeMillis()
        progress.postValue(0 to 0)
    }

    suspend fun calculateStats(artistId: Int, whitmoreScore: Int = -1): PersonStats = withContext(Dispatchers.Default) {
        val collection = collectionDao.loadCollectionForArtist(artistId).map { it.mapToModel() }
        val stats = PersonStats.fromLinkedCollection(collection, context)
        val realOldScore = if (whitmoreScore == -1) artistDao.loadArtist(artistId)?.whitmoreScore ?: 0 else whitmoreScore
        if (stats.whitmoreScore != realOldScore) {
            artistDao.updateWhitmoreScore(artistId, stats.whitmoreScore)
        }
        stats
    }
}
