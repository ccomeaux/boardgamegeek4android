package com.boardgamegeek.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.db.DesignerDao
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
    private val dao = DesignerDao(context)
    private val prefs: SharedPreferences by lazy { context.preferences() }

    suspend fun loadDesigners(sortBy: DesignerDao.SortType) = dao.loadDesigners(sortBy).map { it.mapToDesignerEntity() }

    suspend fun loadDesigner(designerId: Int) = dao.loadDesigner(designerId)?.mapToDesignerEntity()

    suspend fun loadCollection(id: Int, sortBy: CollectionDao.SortType) = dao.loadCollection(id, sortBy)

    suspend fun delete() = dao.delete()

    suspend fun refreshDesigner(designerId: Int): Int = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val response = api.person(BggService.PersonType.DESIGNER, designerId)
        response.mapToEntity(designerId, timestamp)?.let {
            dao.upsert(it.mapToDesignerBasic())
        } ?: 0
    }

    suspend fun refreshImages(designer: PersonEntity): PersonEntity = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val response = api.person(designer.id)
        val entity = response.items.firstOrNull()?.mapToEntity(designer, timestamp)
        entity?.let {
            dao.upsert(it.mapToDesignerImages())
        }
        entity ?: designer
    }

    suspend fun refreshHeroImage(designer: PersonEntity): PersonEntity = withContext(Dispatchers.IO) {
        val urlMap = imageRepository.getImageUrls(designer.thumbnailUrl.getImageId())
        val urls = urlMap[ImageRepository.ImageType.HERO]
        if (urls?.isNotEmpty() == true) {
            dao.updateHeroImageUrl(designer.id, urls.first())
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
        val collection = dao.loadCollection(designerId)
        val statsEntity = PersonStatsEntity.fromLinkedCollection(collection, context)
        val realOldScore = if (whitmoreScore == -1) dao.loadDesigner(designerId)?.whitmoreScore ?: 0 else whitmoreScore
        if (statsEntity.whitmoreScore != realOldScore) {
            dao.updateWhitmoreScore(designerId, statsEntity.whitmoreScore)
        }
        statsEntity
    }
}
