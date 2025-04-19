package com.boardgamegeek.repository

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.db.PublisherDao
import com.boardgamegeek.export.Constants.INVALID_IMAGE_ID
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.safeApiCall
import com.boardgamegeek.mappers.mapForUpsert
import com.boardgamegeek.mappers.mapToModel
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.CollectionItem.Companion.applySort
import com.boardgamegeek.model.CollectionItem.Companion.filterBySyncedStatues
import com.boardgamegeek.model.Company
import com.boardgamegeek.model.Company.Companion.applySort
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

class PublisherRepository(
    val context: Context,
    private val api: BggService,
    private val publisherDao: PublisherDao,
    private val collectionDao: CollectionDao,
    private val imageRepository: ImageRepository,
) {
    fun loadPublishersFlow(sortBy: Company.SortType): Flow<List<Company>> {
        return publisherDao.loadPublishersFlow()
            .map { it.map { entity -> entity.mapToModel() } }
            .flowOn(Dispatchers.Default)
            .map { it.applySort(sortBy) }
            .flowOn(Dispatchers.Default)
            .conflate()
    }

    fun loadPublisherFlow(publisherId: Int) = publisherDao.loadPublisherFlow(publisherId).map { it?.mapToModel() }.flowOn(Dispatchers.Default)

    fun loadCollectionFlow(id: Int, sortBy: CollectionItem.SortType): Flow<List<CollectionItem>> {
        return collectionDao.loadForPublisherFlow(id)
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

    suspend fun deleteAll() = withContext(Dispatchers.IO) { publisherDao.deleteAll() }

    suspend fun refreshPublisher(publisherId: Int, age: Duration? = null) = withContext(Dispatchers.IO) {
        if (publisherId == BggContract.INVALID_ID) return@withContext
        if (age != null) {
            val publisher = publisherDao.loadPublisher(publisherId)
            if (!publisher?.updatedTimestamp?.time.isOlderThan(age)) return@withContext
        }
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

    suspend fun refreshMissingThumbnails(daysOld: Int = 14, limit: Int = 10) = withContext(Dispatchers.Default) {
        withContext(Dispatchers.IO) { publisherDao.loadPublishers() }
            .filter { it.publisherThumbnailUrl.isNullOrBlank() &&
                    (it.updatedTimestamp == null || it.updatedTimestamp.time.isOlderThan(daysOld.days)) }
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

    suspend fun refreshMissingThumbnailsForGame(gameId: Int, daysOld: Int = 14, limit: Int = 10) = withContext(Dispatchers.Default) {
        Timber.i("Refreshing up to $limit publisher images from game $gameId missing for more than $daysOld days")
        withContext(Dispatchers.IO) { publisherDao.loadPublishersForGame(gameId) }
            .asSequence()
            .filter { it.publisherThumbnailUrl.isNullOrBlank() &&
                    (it.updatedTimestamp == null || it.updatedTimestamp.time.isOlderThan(daysOld.days)) }
            .sortedByDescending { it.whitmoreScore }
            .take(limit.coerceIn(0, 25))
            .map { it.mapToModel() }.toList()
            .forEach {
                Timber.d("Refreshing missing images for publisher $it")
                refreshPublisher(it.id)
                attemptRefreshHeroImage(it)
                delay(2_000)
            }
    }

    private suspend fun attemptRefreshHeroImage(publisher: Company) = withContext(Dispatchers.IO) {
        val thumbnailId = publisher.thumbnailUrl.getImageId()
        if (thumbnailId != publisher.heroImageUrl.getImageId()) {
            if (thumbnailId == INVALID_IMAGE_ID) {
                publisherDao.updateHeroImageUrl(publisher.id, "")
            } else {
                val urlMap = imageRepository.getImageUrls(thumbnailId)
                urlMap[ImageRepository.ImageType.HERO]?.firstOrNull()?.let {
                    publisherDao.updateHeroImageUrl(publisher.id, it)
                }
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
