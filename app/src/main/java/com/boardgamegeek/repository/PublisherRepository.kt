package com.boardgamegeek.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.db.PublisherDao
import com.boardgamegeek.entities.CompanyEntity
import com.boardgamegeek.entities.PersonStatsEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.BggService
import com.boardgamegeek.mappers.mapToEntity
import com.boardgamegeek.mappers.mapToPublisherBasic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PublisherRepository(
    val context: Context,
    private val api: BggService,
    private val imageRepository: ImageRepository,
) {
    private val dao = PublisherDao(context)
    private val prefs: SharedPreferences by lazy { context.preferences() }

    suspend fun loadPublishers(sortBy: PublisherDao.SortType) = dao.loadPublishers(sortBy).map { it.mapToEntity() }

    suspend fun loadPublisher(publisherId: Int) = dao.loadPublisher(publisherId)?.mapToEntity()

    suspend fun loadCollection(id: Int, sortBy: CollectionDao.SortType) = dao.loadCollection(id, sortBy)

    suspend fun delete() = dao.delete()

    suspend fun refreshPublisher(publisherId: Int): Int = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val response = api.company(publisherId)
        response.items.firstOrNull()?.mapToEntity(timestamp)?.let {
            dao.upsert(it.mapToPublisherBasic())
        } ?: 0
    }

    suspend fun refreshImages(publisher: CompanyEntity): CompanyEntity = withContext(Dispatchers.IO) {
        val urlMap = imageRepository.getImageUrls(publisher.thumbnailUrl.getImageId())
        val urls = urlMap[ImageRepository.ImageType.HERO]
        urls?.firstOrNull()?.let {
            dao.updateHeroImageUrl(publisher.id, it)
            publisher.copy(heroImageUrl = it)
        } ?: publisher
    }

    suspend fun calculateWhitmoreScores(publishers: List<CompanyEntity>, progress: MutableLiveData<Pair<Int, Int>>) =
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

    suspend fun calculateStats(publisherId: Int, whitmoreScore: Int = -1): PersonStatsEntity = withContext(Dispatchers.Default) {
        val collection = dao.loadCollection(publisherId)
        val statsEntity = PersonStatsEntity.fromLinkedCollection(collection, context)
        val realOldScore = if (whitmoreScore == -1) dao.loadPublisher(publisherId)?.whitmoreScore ?: 0 else whitmoreScore
        if (statsEntity.whitmoreScore != realOldScore) {
            dao.updateWhitmoreScore(publisherId, statsEntity.whitmoreScore)
        }
        statsEntity
    }
}
