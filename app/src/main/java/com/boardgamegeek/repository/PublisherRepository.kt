package com.boardgamegeek.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.db.PublisherDao
import com.boardgamegeek.model.CollectionItem.Companion.filterBySyncedStatues
import com.boardgamegeek.model.Company
import com.boardgamegeek.model.PersonStats
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.BggService
import com.boardgamegeek.mappers.mapToModel
import com.boardgamegeek.mappers.mapToPublisherBasic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PublisherRepository(
    val context: Context,
    private val api: BggService,
    private val imageRepository: ImageRepository,
) {
    private val publisherDao = PublisherDao(context)
    private val collectionDao = CollectionDao(context)
    private val prefs: SharedPreferences by lazy { context.preferences() }

    suspend fun loadPublishers(sortBy: PublisherDao.SortType) = publisherDao.loadPublishers(sortBy).map { it.mapToModel() }

    suspend fun loadPublisher(publisherId: Int) = publisherDao.loadPublisher(publisherId)?.mapToModel()

    suspend fun loadCollection(id: Int, sortBy: CollectionDao.SortType) =
        collectionDao.loadCollectionForPublisher(id, sortBy)
            .map { it.mapToModel() }
            .filter { it.deleteTimestamp == 0L }
            .filter { it.filterBySyncedStatues(context) }

    suspend fun delete() = publisherDao.delete()

    suspend fun refreshPublisher(publisherId: Int): Int = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val response = api.company(publisherId)
        response.items.firstOrNull()?.mapToModel(timestamp)?.let {
            publisherDao.upsert(it.mapToPublisherBasic())
        } ?: 0
    }

    suspend fun refreshImages(publisher: Company): Company = withContext(Dispatchers.IO) {
        val urlMap = imageRepository.getImageUrls(publisher.thumbnailUrl.getImageId())
        val urls = urlMap[ImageRepository.ImageType.HERO]
        urls?.firstOrNull()?.let {
            publisherDao.updateHeroImageUrl(publisher.id, it)
            publisher.copy(heroImageUrl = it)
        } ?: publisher
    }

    suspend fun calculateWhitmoreScores(publishers: List<Company>, progress: MutableLiveData<Pair<Int, Int>>) =
        withContext(Dispatchers.Default) {
            val sortedList = publishers.sortedBy { it.statsUpdatedTimestamp }
            val maxProgress = sortedList.size
            sortedList.forEachIndexed { i, data ->
                progress.postValue(i to maxProgress)
                calculateStats(data.id, data.whitmoreScore)
            }
            prefs[PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_PUBLISHERS] = System.currentTimeMillis()
            progress.postValue(0 to 0)
        }

    suspend fun calculateStats(publisherId: Int, whitmoreScore: Int = -1): PersonStats = withContext(Dispatchers.Default) {
        val collection = collectionDao.loadCollectionForPublisher(publisherId).map { it.mapToModel() }
        val stats = PersonStats.fromLinkedCollection(collection, context)
        val realOldScore = if (whitmoreScore == -1) publisherDao.loadPublisher(publisherId)?.whitmoreScore ?: 0 else whitmoreScore
        if (stats.whitmoreScore != realOldScore) {
            publisherDao.updateWhitmoreScore(publisherId, stats.whitmoreScore)
        }
        stats
    }
}
