package com.boardgamegeek.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.db.PublisherDao
import com.boardgamegeek.model.CollectionItem.Companion.filterBySyncedStatues
import com.boardgamegeek.model.Company
import com.boardgamegeek.model.PersonStats
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.safeApiCall
import com.boardgamegeek.mappers.mapForUpsert
import com.boardgamegeek.mappers.mapToModel
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.Company.Companion.applySort
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

class PublisherRepository(
    val context: Context,
    private val api: BggService,
    private val publisherDao: PublisherDao,
    private val collectionDao: CollectionDao,
    private val imageRepository: ImageRepository,
) {
    enum class CollectionSortType {
        NAME, RATING
    }

    fun loadPublishersFlow(sortBy: Company.SortType): Flow<List<Company>> {
        return publisherDao.loadPublishersFlow()
            .map { it.map { entity -> entity.mapToModel() } }
            .flowOn(Dispatchers.Default)
            .map { it.applySort(sortBy) }
            .flowOn(Dispatchers.Default)
            .conflate()
    }

    fun loadPublisherFlow(publisherId: Int) = publisherDao.loadPublisherFlow(publisherId).map { it?.mapToModel() }.flowOn(Dispatchers.Default)

    suspend fun loadCollection(id: Int, sortBy: CollectionSortType): LiveData<List<CollectionItem>> = withContext(Dispatchers.Default) {
        if (id == BggContract.INVALID_ID) MutableLiveData(emptyList())
        else collectionDao.loadForPublisherAsLiveData(id).map {
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

    suspend fun deleteAll() = withContext(Dispatchers.IO) { publisherDao.deleteAll() }

    suspend fun refreshPublisher(publisherId: Int) = withContext(Dispatchers.IO) {
        if (publisherId == BggContract.INVALID_ID) return@withContext
        val timestamp = Date()
        val response = safeApiCall(context) { api.company(publisherId) }
        if (response.isSuccess) {
            response.getOrNull()?.items?.firstOrNull()?.mapToModel(timestamp)?.let { company ->
                val publisher = publisherDao.loadPublisher(company.id)
                if (publisher == null) {
                    publisherDao.insert(company.mapForUpsert())
                } else {
                    publisherDao.update(company.mapForUpsert(publisher.internalId))
                }
                attemptRefreshHeroImage(company)
            }
        }
    }

    suspend fun refreshMissingThumbnails(limit: Int = 10) = withContext(Dispatchers.Default) {
        withContext(Dispatchers.IO) { publisherDao.loadPublishers() }
            .filter { it.publisherThumbnailUrl.isNullOrBlank() }
            .sortedByDescending { it.whitmoreScore }
            .take(limit.coerceIn(0, 25))
            .map { it.mapToModel() }
            .forEach {
                Timber.d("Refreshing missing images for publisher $it")
                refreshPublisher(it.id)
                attemptRefreshHeroImage(it)
                delay(2_000)
            }
    }

    private suspend fun attemptRefreshHeroImage(publisher: Company) = withContext(Dispatchers.IO) {
        if (publisher.thumbnailUrl.getImageId() != publisher.heroImageUrl.getImageId()) {
            val urlMap = imageRepository.getImageUrls(publisher.thumbnailUrl.getImageId())
            urlMap[ImageRepository.ImageType.HERO]?.firstOrNull()?.let {
                publisherDao.updateHeroImageUrl(publisher.id, it)
            }
        }
    }

    suspend fun calculateStats(progress: MutableLiveData<Pair<Int, Int>>) = withContext(Dispatchers.Default) {
        val publishers = withContext(Dispatchers.IO) { publisherDao.loadPublishers() }
            .map { it.mapToModel() }
            .sortedWith(
                compareBy<Company> { it.statsUpdatedTimestamp }
                    .thenByDescending { it.itemCount }
            )
        val maxProgress = publishers.size
        publishers.forEachIndexed { i, publisher ->
            progress.postValue(i to maxProgress)
            calculateStats(publisher.id, publisher.whitmoreScore)
            Timber.i("Updated stats for publisher $publisher")
        }
        context.preferences()[PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_PUBLISHERS] = System.currentTimeMillis()
        progress.postValue(0 to 0)
    }

    suspend fun calculateStats(publisherId: Int, whitmoreScore: Int? = null): PersonStats? = withContext(Dispatchers.Default) {
        if (publisherId == BggContract.INVALID_ID) return@withContext null
        val timestamp = Date()
        val collection = withContext(Dispatchers.IO) { collectionDao.loadForPublisher(publisherId).map { it.mapToModel() } }
        val stats = PersonStats.fromLinkedCollection(collection, context)
        val currentWhitmoreScore = (whitmoreScore ?: withContext(Dispatchers.IO) { publisherDao.loadPublisher(publisherId) }?.whitmoreScore ?: 0).coerceAtLeast(0)
        if (stats.whitmoreScore != currentWhitmoreScore) {
            withContext(Dispatchers.IO) { publisherDao.updateWhitmoreScore(publisherId, stats.whitmoreScore, timestamp) }
        }
        stats
    }
}
