package com.boardgamegeek.repository

import android.content.SharedPreferences
import androidx.core.content.contentValuesOf
import androidx.lifecycle.MutableLiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.db.PublisherDao
import com.boardgamegeek.entities.BriefGameEntity
import com.boardgamegeek.entities.CompanyEntity
import com.boardgamegeek.entities.PersonStatsEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.mappers.mapToEntity
import com.boardgamegeek.provider.BggContract.Publishers
import com.boardgamegeek.util.ImageUtils.getImageId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PublisherRepository(val application: BggApplication) {
    private val dao = PublisherDao(application)
    private val prefs: SharedPreferences by lazy { application.preferences() }

    suspend fun loadPublishers(sortBy: PublisherDao.SortType): List<CompanyEntity> {
        return dao.loadPublishers(sortBy)
    }

    suspend fun loadPublisher(publisherId: Int): CompanyEntity? {
        return dao.loadPublisher(publisherId)
    }

    suspend fun loadCollection(id: Int, sortBy: CollectionDao.SortType): List<BriefGameEntity> {
        return dao.loadCollection(id, sortBy)
    }

    suspend fun refreshPublisher(publisherId: Int): CompanyEntity? = withContext(Dispatchers.IO) {
        val response = Adapter.createForXml().company(publisherId)
        response.items.firstOrNull()?.mapToEntity()?.let {
            dao.upsert(
                publisherId, contentValuesOf(
                    Publishers.PUBLISHER_NAME to it.name,
                    Publishers.PUBLISHER_SORT_NAME to it.sortName,
                    Publishers.PUBLISHER_DESCRIPTION to it.description,
                    Publishers.PUBLISHER_IMAGE_URL to it.imageUrl,
                    Publishers.PUBLISHER_THUMBNAIL_URL to it.thumbnailUrl,
                    Publishers.UPDATED to System.currentTimeMillis(),
                )
            )
            it
        }
    }

    suspend fun refreshImages(publisher: CompanyEntity): CompanyEntity = withContext(Dispatchers.IO) {
        val response = Adapter.createGeekdoApi().image(publisher.thumbnailUrl.getImageId())
        val url = response.images.medium.url
        dao.upsert(publisher.id, contentValuesOf(Publishers.PUBLISHER_HERO_IMAGE_URL to url))
        publisher.copy(heroImageUrl = url)
    }

    suspend fun calculateWhitmoreScores(publishers: List<CompanyEntity>, progress: MutableLiveData<Pair<Int, Int>>) =
        withContext(Dispatchers.Default) {
            val sortedList = publishers.sortedBy { it.statsUpdatedTimestamp }
            val maxProgress = sortedList.size
            sortedList.forEachIndexed { i, data ->
                progress.postValue(i to maxProgress)
                val collection = dao.loadCollection(data.id)
                val statsEntity = PersonStatsEntity.fromLinkedCollection(collection, application)
                updateWhitmoreScore(data.id, statsEntity.whitmoreScore, data.whitmoreScore)
            }
            prefs[PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_PUBLISHERS] = System.currentTimeMillis()
            progress.postValue(0 to 0)
        }

    suspend fun calculateStats(publisherId: Int): PersonStatsEntity = withContext(Dispatchers.Default) {
        val collection = dao.loadCollection(publisherId)
        val linkedCollection = PersonStatsEntity.fromLinkedCollection(collection, application)
        updateWhitmoreScore(publisherId, linkedCollection.whitmoreScore)
        linkedCollection
    }

    private suspend fun updateWhitmoreScore(id: Int, newScore: Int, oldScore: Int = -1) = withContext(Dispatchers.IO) {
        val realOldScore = if (oldScore == -1) dao.loadPublisher(id)?.whitmoreScore ?: 0 else oldScore
        if (newScore != realOldScore) {
            dao.upsert(
                id, contentValuesOf(
                    Publishers.WHITMORE_SCORE to newScore,
                    Publishers.PUBLISHER_STATS_UPDATED_TIMESTAMP to System.currentTimeMillis()
                )
            )
        }
    }
}
