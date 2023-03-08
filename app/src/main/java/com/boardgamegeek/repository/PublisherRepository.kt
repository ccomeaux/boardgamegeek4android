package com.boardgamegeek.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.contentValuesOf
import androidx.lifecycle.MutableLiveData
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.db.PublisherDao
import com.boardgamegeek.entities.CompanyEntity
import com.boardgamegeek.entities.PersonStatsEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.GeekdoApi
import com.boardgamegeek.mappers.mapToEntity
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.Publishers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PublisherRepository(
    val context: Context,
    private val api: BggService,
    private val imageRepository: ImageRepository,
) {
    private val dao = PublisherDao(context)
    private val prefs: SharedPreferences by lazy { context.preferences() }

    suspend fun loadPublishers(sortBy: PublisherDao.SortType) = dao.loadPublishers(sortBy)

    suspend fun loadPublisher(publisherId: Int) = dao.loadPublisher(publisherId)

    suspend fun loadCollection(id: Int, sortBy: CollectionDao.SortType) = dao.loadCollection(id, sortBy)

    suspend fun delete() = dao.delete()

    suspend fun refreshPublisher(publisherId: Int): CompanyEntity? = withContext(Dispatchers.IO) {
        val response = api.company(publisherId)
        response.items.firstOrNull()?.mapToEntity()?.let {
            dao.upsert(
                publisherId, contentValuesOf(
                    Publishers.Columns.PUBLISHER_NAME to it.name,
                    Publishers.Columns.PUBLISHER_SORT_NAME to it.sortName,
                    Publishers.Columns.PUBLISHER_DESCRIPTION to it.description,
                    Publishers.Columns.PUBLISHER_IMAGE_URL to it.imageUrl,
                    Publishers.Columns.PUBLISHER_THUMBNAIL_URL to it.thumbnailUrl,
                    Publishers.Columns.UPDATED to System.currentTimeMillis(),
                )
            )
            it
        }
    }

    suspend fun refreshImages(publisher: CompanyEntity): CompanyEntity = withContext(Dispatchers.IO) {
        val urlMap = imageRepository.getImageUrls(publisher.thumbnailUrl.getImageId())
        val urls = urlMap[ImageRepository.ImageType.HERO]
        if (urls?.isNotEmpty() == true) {
            dao.upsert(publisher.id, contentValuesOf(Publishers.Columns.PUBLISHER_HERO_IMAGE_URL to urls.first()))
            publisher.copy(heroImageUrl = urls.first())
        } else publisher
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
        updateWhitmoreScore(publisherId, statsEntity.whitmoreScore, whitmoreScore)
        statsEntity
    }

    private suspend fun updateWhitmoreScore(id: Int, newScore: Int, oldScore: Int = -1) = withContext(Dispatchers.IO) {
        val realOldScore = if (oldScore == -1) dao.loadPublisher(id)?.whitmoreScore ?: 0 else oldScore
        if (newScore != realOldScore) {
            dao.upsert(
                id,
                contentValuesOf(
                    Publishers.Columns.WHITMORE_SCORE to newScore,
                    Publishers.Columns.PUBLISHER_STATS_UPDATED_TIMESTAMP to System.currentTimeMillis(),
                )
            )
        }
    }
}
