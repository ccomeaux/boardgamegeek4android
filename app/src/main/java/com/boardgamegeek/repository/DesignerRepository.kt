package com.boardgamegeek.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.db.DesignerDao
import com.boardgamegeek.entities.CollectionItemEntity.Companion.filterBySyncedStatues
import com.boardgamegeek.entities.PersonEntity
import com.boardgamegeek.entities.PersonStatsEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.BggService
import com.boardgamegeek.mappers.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DesignerRepository(
    val context: Context,
    private val api: BggService,
    private val imageRepository: ImageRepository,
) {
    private val designerDao = DesignerDao(context)
    private val collectionDao = CollectionDao(context)
    private val prefs: SharedPreferences by lazy { context.preferences() }

    suspend fun loadDesigners(sortBy: DesignerDao.SortType) = designerDao.loadDesigners(sortBy).map { it.mapToDesignerEntity() }

    suspend fun loadDesigner(designerId: Int) = designerDao.loadDesigner(designerId)?.mapToDesignerEntity()

    suspend fun loadCollection(id: Int, sortBy: CollectionDao.SortType) =
        collectionDao.loadCollectionForDesigner(id, sortBy)
            .map { it.mapToEntity() }
            .filter { it.deleteTimestamp == 0L }
            .filter { it.filterBySyncedStatues(context) }

    suspend fun delete() = designerDao.delete()

    suspend fun refreshDesigner(designerId: Int): Int = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val response = api.person(BggService.PersonType.DESIGNER, designerId)
        response.mapToEntity(designerId, timestamp)?.let {
            designerDao.upsert(it.mapToDesignerBasic())
        } ?: 0
    }

    suspend fun refreshImages(designer: PersonEntity): PersonEntity = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val response = api.person(designer.id)
        val entity = response.items.firstOrNull()?.mapToEntity(designer, timestamp)
        entity?.let {
            designerDao.upsert(it.mapToDesignerImages())
        }
        entity ?: designer
    }

    suspend fun refreshHeroImage(designer: PersonEntity): PersonEntity = withContext(Dispatchers.IO) {
        val urlMap = imageRepository.getImageUrls(designer.thumbnailUrl.getImageId())
        val urls = urlMap[ImageRepository.ImageType.HERO]
        if (urls?.isNotEmpty() == true) {
            designerDao.updateHeroImageUrl(designer.id, urls.first())
            designer.copy(heroImageUrl = urls.first())
        } else designer
    }

    suspend fun calculateWhitmoreScores(designers: List<PersonEntity>, progress: MutableLiveData<Pair<Int, Int>>) = withContext(Dispatchers.Default) {
        val sortedList = designers.sortedBy { it.statsUpdatedTimestamp }
        val maxProgress = sortedList.size
        sortedList.forEachIndexed { i, data ->
            progress.postValue(i to maxProgress)
            calculateStats(data.id, data.whitmoreScore)
        }
        prefs[PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_DESIGNERS] = System.currentTimeMillis()
        progress.postValue(0 to 0)
    }

    suspend fun calculateStats(designerId: Int, whitmoreScore: Int = -1): PersonStatsEntity = withContext(Dispatchers.Default) {
        val collection = collectionDao.loadCollectionForDesigner(designerId)
            .map { it.second.mapToEntity(it.first.mapToEntity()) }
        val statsEntity = PersonStatsEntity.fromLinkedCollection(collection, context)
        val realOldScore = if (whitmoreScore == -1) designerDao.loadDesigner(designerId)?.whitmoreScore ?: 0 else whitmoreScore
        if (statsEntity.whitmoreScore != realOldScore) {
            designerDao.updateWhitmoreScore(designerId, statsEntity.whitmoreScore)
        }
        statsEntity
    }
}
