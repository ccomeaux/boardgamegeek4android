package com.boardgamegeek.repository

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.boardgamegeek.db.ArtistDao
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.model.CollectionItem.Companion.filterBySyncedStatues
import com.boardgamegeek.model.Person
import com.boardgamegeek.model.PersonStats
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.BggService
import com.boardgamegeek.mappers.mapArtistForUpsert
import com.boardgamegeek.mappers.mapToModel
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.provider.BggContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Date

class ArtistRepository(
    val context: Context,
    private val api: BggService,
    private val artistDao: ArtistDao,
    private val collectionDao: CollectionDao,
    private val imageRepository: ImageRepository,
) {
    enum class SortType {
        NAME, ITEM_COUNT, WHITMORE_SCORE
    }

    enum class CollectionSortType {
        NAME, RATING
    }

    suspend fun loadArtists(sortBy: SortType): List<Person> {
        return artistDao.loadArtists()
            .sortedWith(
                compareBy(
                    String.CASE_INSENSITIVE_ORDER
                ) {
                    when (sortBy) {
                        SortType.NAME -> it.artistName
                        SortType.WHITMORE_SCORE -> it.whitmoreScore
                        SortType.ITEM_COUNT -> 0 // TODO
                    }.toString()
                }
            )
            .map { it.mapToModel() }
    }

    suspend fun loadArtist(artistId: Int) = artistDao.loadArtist(artistId)?.mapToModel()

    fun loadArtistAsLiveData(artistId: Int) = artistDao.loadArtistAsLiveData(artistId).map { it.mapToModel() }

    suspend fun loadCollection(id: Int, sortBy: CollectionSortType): List<CollectionItem> {
        if (id == BggContract.INVALID_ID) return emptyList()
        return collectionDao.loadForArtist(id)
            .map { it.mapToModel() }
            .filter { it.deleteTimestamp == 0L }
            .filter { it.filterBySyncedStatues(context) }
            .sortedWith(
                if (sortBy == CollectionSortType.RATING)
                    compareByDescending<CollectionItem> { it.rating }.thenByDescending { it.isFavorite }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.sortName }
                else
                    compareBy(String.CASE_INSENSITIVE_ORDER) { it.sortName }
            )
    }

    suspend fun deleteAll() = artistDao.deleteAll()

    suspend fun refreshArtist(artistId: Int) = withContext(Dispatchers.IO) {
        val timestamp = Date()
        val response = api.person(BggService.PersonType.ARTIST, artistId)
        response.mapToModel(artistId, timestamp)?.let {
            val artist = artistDao.loadArtist(it.id)
            if (artist == null) {
                artistDao.insert(it.mapArtistForUpsert())
                // TODO update item count
            } else {
                artistDao.update(it.mapArtistForUpsert(artist.internalId))
            }
        }
    }

    suspend fun refreshImages(artist: Person) = withContext(Dispatchers.IO) {
        val timestamp = Date()
        val response = api.person(artist.id)
        val person = response.items.firstOrNull()?.mapToModel(artist, timestamp)
        person?.let {
            artistDao.updateImageUrls(artist.id, it.imageUrl, it.thumbnailUrl, it.imagesUpdatedTimestamp ?: timestamp)
            val imageId = it.thumbnailUrl.getImageId()
            if (imageId != artist.heroImageUrl.getImageId()) {
                Timber.d("Artist $artist's hero URL doesn't match image ID $imageId")
                val urlMap = imageRepository.getImageUrls(imageId)
                urlMap[ImageRepository.ImageType.HERO]?.firstOrNull()?.let { heroUrl ->
                    Timber.d("Updating artist $artist with hero URL $heroUrl")
                    artistDao.updateHeroImageUrl(artist.id, heroUrl)
                }
            }
        }
    }

    suspend fun calculateWhitmoreScores(artists: List<Person>, progress: MutableLiveData<Pair<Int, Int>>) = withContext(Dispatchers.Default) {
        val sortedList = artists.sortedBy { it.statsUpdatedTimestamp }
        val maxProgress = sortedList.size
        sortedList.forEachIndexed { i, data ->
            progress.postValue(i to maxProgress)
            calculateStats(data.id, data.whitmoreScore)
        }
        context.preferences()[PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_ARTISTS] = System.currentTimeMillis()
        progress.postValue(0 to 0)
    }

    suspend fun calculateStats(artistId: Int, whitmoreScore: Int = -1): PersonStats = withContext(Dispatchers.Default) {
        val timestamp = Date()
        val collection = collectionDao.loadForArtist(artistId).map { it.mapToModel() }
        val stats = PersonStats.fromLinkedCollection(collection, context)
        val realOldScore = if (whitmoreScore == -1) artistDao.loadArtist(artistId)?.whitmoreScore ?: 0 else whitmoreScore
        if (stats.whitmoreScore != realOldScore) {
            artistDao.updateWhitmoreScore(artistId, stats.whitmoreScore, timestamp)
        }
        stats
    }
}
