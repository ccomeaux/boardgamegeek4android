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
import com.boardgamegeek.mappers.mapForUpsert
import com.boardgamegeek.mappers.mapToModel
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.provider.BggContract
import kotlinx.coroutines.Dispatchers
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
    enum class SortType {
        NAME, ITEM_COUNT, WHITMORE_SCORE
    }

    enum class CollectionSortType {
        NAME, RATING
    }

    suspend fun loadPublishersAsLiveData(sortBy: SortType): LiveData<List<Company>> = withContext(Dispatchers.Default) {
        publisherDao.loadPublishersAsLiveData().map {
            it.map { entity -> entity.mapToModel() }
        }.map { list ->
            list.sortedWith(
                when (sortBy) {
                    SortType.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                    SortType.WHITMORE_SCORE -> compareByDescending<Company> { it.whitmoreScore }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                    SortType.ITEM_COUNT -> compareByDescending<Company> { it.itemCount }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                }
            )
        }
    }

    fun loadPublisherAsLiveData(publisherId: Int) = publisherDao.loadPublisherAsLiveData(publisherId).map { it.mapToModel() }

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
        val timestamp = Date()
        val response = api.company(publisherId)
        response.items.firstOrNull()?.mapToModel(timestamp)?.let {
            val publisher = publisherDao.loadPublisher(it.id)
            if (publisher == null) {
                publisherDao.insert(it.mapForUpsert())
            } else {
                publisherDao.update(it.mapForUpsert(publisher.internalId))
            }
            val imageId = it.thumbnailUrl.getImageId()
            if (imageId != it.heroImageUrl.getImageId()) {
                refreshHeroImage(it)
            }
        }
    }

    suspend fun refreshMissingThumbnails(limit: Int = 10) = withContext(Dispatchers.IO) {
        val publishers = publisherDao.loadPublishers()
        publishers
            .filter { it.publisherThumbnailUrl.isNullOrBlank() }
            .map { it.mapToModel() }
            .sortedByDescending { it.whitmoreScore }
            .take(limit.coerceIn(0, 25))
            .forEach {
                Timber.d("Refreshing missing images for publisher $it")
                refreshPublisher(it.id)
                refreshHeroImage(it)
            }
    }

    private suspend fun refreshHeroImage(publisher: Company) = withContext(Dispatchers.IO) {
        val urlMap = imageRepository.getImageUrls(publisher.thumbnailUrl.getImageId())
        val urls = urlMap[ImageRepository.ImageType.HERO]
        urls?.firstOrNull()?.let {
            publisherDao.updateHeroImageUrl(publisher.id, it)
        }
    }

    suspend fun calculateWhitmoreScores(progress: MutableLiveData<Pair<Int, Int>>) = withContext(Dispatchers.Default) {
        val publishers = withContext(Dispatchers.IO) { publisherDao.loadPublishers().map { it.mapToModel() } }
        val sortedList = publishers.sortedBy { it.statsUpdatedTimestamp }
        val maxProgress = sortedList.size
        sortedList.forEachIndexed { i, data ->
            progress.postValue(i to maxProgress)
            calculateStats(data.id, data.whitmoreScore)
        }
        context.preferences()[PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_PUBLISHERS] = System.currentTimeMillis()
        progress.postValue(0 to 0)
    }

    suspend fun calculateStats(publisherId: Int, whitmoreScore: Int = -1): PersonStats = withContext(Dispatchers.Default) {
        val timestamp = Date()
        val collection = withContext(Dispatchers.IO) { collectionDao.loadForPublisher(publisherId).map { it.mapToModel() } }
        val stats = PersonStats.fromLinkedCollection(collection, context)
        val realOldScore = if (whitmoreScore == -1) publisherDao.loadPublisher(publisherId)?.whitmoreScore ?: 0 else whitmoreScore
        if (stats.whitmoreScore != realOldScore) {
            withContext(Dispatchers.IO) { publisherDao.updateWhitmoreScore(publisherId, stats.whitmoreScore, timestamp) }
        }
        stats
    }
}
