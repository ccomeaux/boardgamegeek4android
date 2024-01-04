package com.boardgamegeek.repository

import android.content.Context
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

    suspend fun loadPublishers(sortBy: SortType): List<Company> {
        return publisherDao.loadPublishers()
            .sortedWith(
                compareBy(
                    String.CASE_INSENSITIVE_ORDER
                ) { // TODO test these options
                    when (sortBy) {
                        SortType.NAME -> it.publisherSortName
                        SortType.WHITMORE_SCORE -> it.whitmoreScore
                        SortType.ITEM_COUNT -> 0 // TODO
                    }.toString()
                }
            )
            .map { it.mapToModel() }
    }

    suspend fun loadPublisher(publisherId: Int) = publisherDao.loadPublisher(publisherId)?.mapToModel()

    fun loadPublisherAsLiveData(publisherId: Int) = publisherDao.loadPublisherAsLiveData(publisherId).map { it.mapToModel() }

    suspend fun loadCollection(id: Int, sortBy: CollectionSortType): List<CollectionItem> {
        if (id == BggContract.INVALID_ID) return emptyList()
        return collectionDao.loadForPublisher(id)
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

    suspend fun deleteAll() = publisherDao.deleteAll()

    suspend fun refreshPublisher(publisherId: Int) = withContext(Dispatchers.IO) {
        val timestamp = Date()
        val response = api.company(publisherId)
        response.items.firstOrNull()?.mapToModel(timestamp)?.let {
            val publisher = publisherDao.loadPublisher(it.id)
            if (publisher == null) {
                publisherDao.insert(it.mapForUpsert())
                // TODO update item count
            } else {
                publisherDao.update(it.mapForUpsert(publisher.internalId))
            }
        }
    }

    suspend fun refreshImages(publisher: Company) = withContext(Dispatchers.IO) {
        val urlMap = imageRepository.getImageUrls(publisher.thumbnailUrl.getImageId())
        val urls = urlMap[ImageRepository.ImageType.HERO]
        urls?.firstOrNull()?.let {
            publisherDao.updateHeroImageUrl(publisher.id, it)
        }
    }

    suspend fun calculateWhitmoreScores(publishers: List<Company>, progress: MutableLiveData<Pair<Int, Int>>) =
        withContext(Dispatchers.Default) {
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
        val collection = collectionDao.loadForPublisher(publisherId).map { it.mapToModel() }
        val stats = PersonStats.fromLinkedCollection(collection, context)
        val realOldScore = if (whitmoreScore == -1) publisherDao.loadPublisher(publisherId)?.whitmoreScore ?: 0 else whitmoreScore
        if (stats.whitmoreScore != realOldScore) {
            publisherDao.updateWhitmoreScore(publisherId, stats.whitmoreScore, timestamp)
        }
        stats
    }
}
