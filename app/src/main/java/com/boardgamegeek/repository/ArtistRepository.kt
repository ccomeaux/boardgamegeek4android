package com.boardgamegeek.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import com.boardgamegeek.db.ArtistDao
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.entities.PersonEntity
import com.boardgamegeek.entities.PersonStatsEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.BggService
import com.boardgamegeek.mappers.mapToArtistEntity
import com.boardgamegeek.mappers.mapToArtistImages
import com.boardgamegeek.mappers.mapToArtistBasic
import com.boardgamegeek.mappers.mapToEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ArtistRepository(
    val context: Context,
    private val api: BggService,
    private val imageRepository: ImageRepository,
) {
    private val dao = ArtistDao(context)
    private val prefs: SharedPreferences by lazy { context.preferences() }

    suspend fun loadArtists(sortBy: ArtistDao.SortType) = dao.loadArtists(sortBy).map { it.mapToArtistEntity() }

    suspend fun loadArtist(artistId: Int) = dao.loadArtist(artistId)?.mapToArtistEntity()

    suspend fun loadCollection(id: Int, sortBy: CollectionDao.SortType) = dao.loadCollection(id, sortBy)

    suspend fun delete() = dao.delete()

    suspend fun refreshArtist(artistId: Int): Int = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val response = api.person(BggService.PersonType.ARTIST, artistId)
        response.mapToEntity(artistId, timestamp)?.let {
            dao.upsert(it.mapToArtistBasic())
        } ?: 0
    }

    suspend fun refreshImages(artist: PersonEntity): PersonEntity = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val response = api.person(artist.id)
        val entity = response.items.firstOrNull()?.mapToEntity(artist, timestamp)
        entity?.let {
            dao.upsert(it.mapToArtistImages())
        }
        entity ?: artist
    }

    suspend fun refreshHeroImage(artist: PersonEntity): PersonEntity = withContext(Dispatchers.IO) {
        val urlMap = imageRepository.fetchImageUrls(artist.thumbnailUrl.getImageId())
        val urls = urlMap[ImageRepository.ImageType.HERO]
        if (urls?.isNotEmpty() == true) {
            dao.updateHeroImageUrl(artist.id, urls.first())
            artist.copy(heroImageUrl = urls.first())
        } else artist
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
        val statsEntity = PersonStatsEntity.fromLinkedCollection(collection, context)
        val realOldScore = if (whitmoreScore == -1) dao.loadArtist(artistId)?.whitmoreScore ?: 0 else whitmoreScore
        if (statsEntity.whitmoreScore != realOldScore) {
            dao.updateWhitmoreScore(artistId, statsEntity.whitmoreScore)
        }
        statsEntity
    }
}
