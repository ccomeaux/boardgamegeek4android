package com.boardgamegeek.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.boardgamegeek.db.ArtistDao
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.model.CollectionItem.Companion.filterBySyncedStatues
import com.boardgamegeek.model.Person
import com.boardgamegeek.model.PersonStats
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.safeApiCall
import com.boardgamegeek.mappers.mapArtistForUpsert
import com.boardgamegeek.mappers.mapToModel
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.Person.Companion.applySort
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

class ArtistRepository(
    val context: Context,
    private val api: BggService,
    private val artistDao: ArtistDao,
    private val collectionDao: CollectionDao,
    private val imageRepository: ImageRepository,
) {
    enum class CollectionSortType {
        NAME, RATING
    }

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

    suspend fun loadCollection(id: Int, sortBy: CollectionSortType): LiveData<List<CollectionItem>> = withContext(Dispatchers.Default) {
        if (id == BggContract.INVALID_ID) MutableLiveData(emptyList())
        else collectionDao.loadForArtistAsLiveData(id).map {
            it.map { entity -> entity.mapToModel() }
        }.map { list ->
            list.filter { it.deleteTimestamp == 0L }
                .filter { it.filterBySyncedStatues(context) }
                .sortedWith(
                    if (sortBy == CollectionSortType.RATING)
                        compareByDescending<CollectionItem> { it.rating }
                            .thenByDescending { it.isFavorite }
                            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.sortName }
                    else
                        compareBy(String.CASE_INSENSITIVE_ORDER) { it.sortName }
                )
        }
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) { artistDao.deleteAll() }

    suspend fun refreshMissingImages(limit: Int = 10) = withContext(Dispatchers.Default) {
        withContext(Dispatchers.IO) { artistDao.loadArtists() }
            .filter { it.artistThumbnailUrl.isNullOrBlank() }
            .sortedByDescending { it.whitmoreScore }
            .take(limit.coerceIn(0, 25))
            .map { it.mapToModel() }
            .forEach {
                Timber.i("Refreshing missing images for artist $it")
                refreshImages(it)
                delay(2_000)
            }
    }

    suspend fun refreshArtist(artistId: Int) = withContext(Dispatchers.IO) {
        if (artistId == BggContract.INVALID_ID) return@withContext
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
            response.getOrNull()?.items?.firstOrNull()?.mapToModel(artist, timestamp)?.let { newArtist ->
                artistDao.updateImageUrls(artist.id, newArtist.imageUrl, newArtist.thumbnailUrl, newArtist.imagesUpdatedTimestamp ?: timestamp)
                val thumbnailId = newArtist.thumbnailUrl.getImageId()
                if (thumbnailId != artist.heroImageUrl.getImageId()) {
                    Timber.d("Artist $artist's hero URL doesn't match thumbnail ID $thumbnailId")
                    val urlMap = imageRepository.getImageUrls(thumbnailId)
                    urlMap[ImageRepository.ImageType.HERO]?.firstOrNull()?.let { heroUrl ->
                        Timber.d("Updating artist $artist with hero URL $heroUrl")
                        artistDao.updateHeroImageUrl(artist.id, heroUrl)
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
