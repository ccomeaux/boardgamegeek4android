package com.boardgamegeek.repository

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.boardgamegeek.db.ArtistDao
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.export.Constants.INVALID_IMAGE_ID
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.safeApiCall
import com.boardgamegeek.mappers.mapArtistForUpsert
import com.boardgamegeek.mappers.mapToModel
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.CollectionItem.Companion.applySort
import com.boardgamegeek.model.CollectionItem.Companion.filterBySyncedStatues
import com.boardgamegeek.model.Person
import com.boardgamegeek.model.Person.Companion.applySort
import com.boardgamegeek.model.PersonStats
import com.boardgamegeek.provider.BggContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Date
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

class ArtistRepository(
    val context: Context,
    private val api: BggService,
    private val artistDao: ArtistDao,
    private val collectionDao: CollectionDao,
    private val imageRepository: ImageRepository,
) {
    fun loadArtistsFlow(sortBy: Person.SortType): Flow<List<Person>> {
        return artistDao.loadArtistsFlow()
            .map {
                it.map { entity -> entity.mapToModel() }
            }.flowOn(Dispatchers.Default)
            .map {
                it.applySort(sortBy)
            }.flowOn(Dispatchers.Default)
            .conflate()
    }

    fun loadArtistFlow(artistId: Int) = artistDao.loadArtistFlow(artistId).map { it?.mapToModel() }.flowOn(Dispatchers.Default)

    fun loadCollectionFlow(id: Int, sortBy: CollectionItem.SortType): Flow<List<CollectionItem>> {
        return collectionDao.loadForArtistAsLiveData(id)
            .map { it.map { entity -> entity.mapToModel() } }
            .flowOn(Dispatchers.Default)
            .map { list ->
                list.filter { it.deleteTimestamp == 0L }
                    .filter { it.filterBySyncedStatues(context) }
                    .applySort(sortBy)
            }
            .flowOn(Dispatchers.Default)
            .conflate()
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) { artistDao.deleteAll() }

    suspend fun refreshMissingImages(daysOld: Int = 14, limit: Int = 10) = withContext(Dispatchers.Default) {
        withContext(Dispatchers.IO) { artistDao.loadArtists() }
            .filter { it.artistThumbnailUrl.isNullOrBlank() &&
                    (it.imagesUpdatedTimestamp == null || it.imagesUpdatedTimestamp.time.isOlderThan(daysOld.days)) }
            .sortedByDescending { it.whitmoreScore }
            .take(limit.coerceIn(0, 25))
            .map { it.mapToModel() }
            .forEach {
                Timber.i("Refreshing missing images for artist $it")
                refreshImages(it)
                delay(2_000)
            }
    }

    suspend fun refreshMissingImagesForGame(gameId: Int, daysOld: Int = 14, limit: Int = 10) = withContext(Dispatchers.Default) {
        Timber.i("Refreshing up to $limit artist images from game $gameId missing for more than $daysOld days")
        withContext(Dispatchers.IO) { artistDao.loadArtistsForGame(gameId) }
            .asSequence()
            .filter { it.artistThumbnailUrl.isNullOrBlank() &&
                    (it.imagesUpdatedTimestamp == null || it.imagesUpdatedTimestamp.time.isOlderThan(daysOld.days)) }
            .sortedByDescending { it.whitmoreScore }
            .take(limit.coerceIn(0, 25))
            .map { it.mapToModel() }.toList()
            .forEach {
                Timber.i("Refreshing missing images for artist $it")
                refreshImages(it)
                delay(2_000)
            }
    }

    suspend fun refreshArtist(artistId: Int, age: Duration? = null) = withContext(Dispatchers.IO) {
        if (artistId == BggContract.INVALID_ID) return@withContext
        if (age != null) {
            val artist = artistDao.loadArtist(artistId)
            if (!artist?.updatedTimestamp?.time.isOlderThan(age)) return@withContext
        }
        val timestamp = Date()
        val response = safeApiCall(context) { api.person(BggService.PersonType.ARTIST, artistId) }
        if (response.isSuccess) {
            response.getOrNull()?.mapToModel(artistId, timestamp)?.let {
                val artist = artistDao.loadArtist(it.id)
                if (artist == null) {
                    artistDao.insert(it.mapArtistForUpsert())
                } else {
                    artistDao.update(it.mapArtistForUpsert(artist.internalId))
                }
                refreshImages(it)
            }
        }
    }

    private suspend fun refreshImages(artist: Person) = withContext(Dispatchers.IO) {
        val timestamp = Date()
        val response = safeApiCall(context) { api.person(artist.id) }
        if (response.isSuccess) {
            response.getOrNull()?.items?.firstOrNull()?.mapToModel(artist, timestamp)?.let { person ->
                artistDao.updateImageUrls(artist.id, person.imageUrl, person.thumbnailUrl, person.imagesUpdatedTimestamp ?: timestamp)
                val thumbnailId = person.thumbnailUrl.getImageId()
                if (thumbnailId != artist.heroImageUrl.getImageId()) {
                    if (thumbnailId == INVALID_IMAGE_ID) {
                        Timber.d("Updating designer $artist with blank hero URL")
                        artistDao.updateHeroImageUrl(artist.id, "")
                    } else {
                        Timber.d("Artist $artist's hero URL doesn't match thumbnail ID $thumbnailId")
                        val urlMap = imageRepository.getImageUrls(thumbnailId)
                        urlMap[ImageRepository.ImageType.HERO]?.firstOrNull()?.let { heroUrl ->
                            Timber.d("Updating artist $artist with hero URL $heroUrl")
                            artistDao.updateHeroImageUrl(artist.id, heroUrl)
                        }
                    }
                }
                Timber.i("Updated images for artist $artist")
            }
        } else Timber.w("Unable to refresh images for artist $artist" )
    }

    suspend fun calculateStats(progress: MutableLiveData<Pair<Int, Int>>) = withContext(Dispatchers.Default) {
        val artists = withContext(Dispatchers.IO) { artistDao.loadArtists() }
            .map { it.mapToModel() }
            .sortedWith(
                compareBy<Person> { it.statsUpdatedTimestamp}
                    .thenByDescending { it.itemCount }
            )
        val maxProgress = artists.size
        artists.forEachIndexed { i, person ->
            progress.postValue(i to maxProgress)
            calculateStats(person.id, person.whitmoreScore)
            Timber.i("Updated stats for artist $person")
        }
        context.preferences()[PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_ARTISTS] = System.currentTimeMillis()
        progress.postValue(0 to 0)
    }

    suspend fun calculateStats(artistId: Int, whitmoreScore: Int? = null): PersonStats? = withContext(Dispatchers.Default) {
        if (artistId == BggContract.INVALID_ID) return@withContext null
        val timestamp = Date()
        val collection = withContext(Dispatchers.IO) { collectionDao.loadForArtist(artistId) }.map { it.mapToModel() }
        val stats = PersonStats.fromLinkedCollection(collection, context)
        val currentWhitmoreScore = (whitmoreScore ?: withContext(Dispatchers.IO) { artistDao.loadArtist(artistId) }?.whitmoreScore ?: 0).coerceAtLeast(0)
        if (stats.whitmoreScore != currentWhitmoreScore) {
            withContext(Dispatchers.IO) { artistDao.updateWhitmoreScore(artistId, stats.whitmoreScore, timestamp) }
        }
        stats
    }
}
