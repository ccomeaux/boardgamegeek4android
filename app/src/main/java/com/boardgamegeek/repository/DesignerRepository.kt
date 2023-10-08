package com.boardgamegeek.repository

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.db.DesignerDao
import com.boardgamegeek.entities.CollectionItem.Companion.filterBySyncedStatues
import com.boardgamegeek.entities.Person
import com.boardgamegeek.entities.PersonStats
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

    suspend fun loadDesigners(sortBy: DesignerDao.SortType) = designerDao.loadDesigners(sortBy).map { it.mapToModel() }

    suspend fun loadDesigner(designerId: Int) = designerDao.loadDesigner(designerId)?.mapToModel()

    suspend fun loadCollection(id: Int, sortBy: CollectionDao.SortType) =
        collectionDao.loadCollectionForDesigner(id, sortBy)
            .map { it.mapToModel() }
            .filter { it.deleteTimestamp == 0L }
            .filter { it.filterBySyncedStatues(context) }

    suspend fun delete() = designerDao.delete()

    suspend fun refreshDesigner(designerId: Int): Int = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val response = api.person(BggService.PersonType.DESIGNER, designerId)
        response.mapToModel(designerId, timestamp)?.let {
            designerDao.upsert(it.mapToDesignerBasic())
        } ?: 0
    }

    suspend fun refreshImages(designer: Person): Person = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val response = api.person(designer.id)
        val person = response.items.firstOrNull()?.mapToModel(designer, timestamp)
        person?.let {
            designerDao.upsert(it.mapToDesignerImages())
        }
        person ?: designer
    }

    suspend fun refreshHeroImage(designer: Person): Person = withContext(Dispatchers.IO) {
        val urlMap = imageRepository.getImageUrls(designer.thumbnailUrl.getImageId())
        val urls = urlMap[ImageRepository.ImageType.HERO]
        if (urls?.isNotEmpty() == true) {
            designerDao.updateHeroImageUrl(designer.id, urls.first())
            designer.copy(heroImageUrl = urls.first())
        } else designer
    }

    suspend fun calculateWhitmoreScores(designers: List<Person>, progress: MutableLiveData<Pair<Int, Int>>) = withContext(Dispatchers.Default) {
        val sortedList = designers.sortedBy { it.statsUpdatedTimestamp }
        val maxProgress = sortedList.size
        sortedList.forEachIndexed { i, data ->
            progress.postValue(i to maxProgress)
            calculateStats(data.id, data.whitmoreScore)
        }
        context.preferences()[PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_DESIGNERS] = System.currentTimeMillis()
        progress.postValue(0 to 0)
    }

    suspend fun calculateStats(designerId: Int, whitmoreScore: Int = -1): PersonStats = withContext(Dispatchers.Default) {
        val collection = collectionDao.loadCollectionForDesigner(designerId)
            .map { it.second.mapToModel(it.first.mapToModel()) }
        val stats = PersonStats.fromLinkedCollection(collection, context)
        val realOldScore = if (whitmoreScore == -1) designerDao.loadDesigner(designerId)?.whitmoreScore ?: 0 else whitmoreScore
        if (stats.whitmoreScore != realOldScore) {
            designerDao.updateWhitmoreScore(designerId, stats.whitmoreScore)
        }
        stats
    }
}
