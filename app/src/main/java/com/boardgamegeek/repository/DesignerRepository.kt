package com.boardgamegeek.repository

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.db.DesignerDao
import com.boardgamegeek.model.CollectionItem.Companion.filterBySyncedStatues
import com.boardgamegeek.model.Person
import com.boardgamegeek.model.PersonStats
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.BggService
import com.boardgamegeek.mappers.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

class DesignerRepository(
    val context: Context,
    private val api: BggService,
    private val designerDao: DesignerDao,
    private val imageRepository: ImageRepository,
) {
    private val collectionDao = CollectionDao(context)

    enum class SortType {
        NAME, ITEM_COUNT, WHITMORE_SCORE
    }

    suspend fun loadDesigners(sortBy: SortType): List<Person> {
        return designerDao.loadDesigners()
            .sortedWith(
                compareBy(
                    String.CASE_INSENSITIVE_ORDER
                ) {
                    when (sortBy) {
                        SortType.NAME -> it.designerName
                        SortType.WHITMORE_SCORE -> it.whitmoreScore
                        SortType.ITEM_COUNT -> 0 // TODO
                    }.toString()
                }
            )
            .map { it.mapToModel() }
    }

    suspend fun loadDesigner(designerId: Int) = designerDao.loadDesigner(designerId)?.mapToModel()

    suspend fun loadCollection(id: Int, sortBy: CollectionDao.SortType) =
        collectionDao.loadCollectionForDesigner(id, sortBy)
            .map { it.mapToModel() }
            .filter { it.deleteTimestamp == 0L }
            .filter { it.filterBySyncedStatues(context) }

    suspend fun deleteAll() = designerDao.deleteAll()

    suspend fun refreshDesigner(designerId: Int) = withContext(Dispatchers.IO) {
        val timestamp = Date()
        val response = api.person(BggService.PersonType.DESIGNER, designerId)
        response.mapToModel(designerId, timestamp)?.let {
            val designer = designerDao.loadDesigner(it.id)
            if (designer == null) {
                designerDao.insert(it.mapDesignerForUpsert())
                // TODO update item count
            } else {
                designerDao.update(it.mapDesignerForUpsert(designer.internalId))
            }
        }
    }

    suspend fun refreshImages(designer: Person): Person = withContext(Dispatchers.IO) {
        val timestamp = Date()
        val response = api.person(designer.id)
        val person = response.items.firstOrNull()?.mapToModel(designer, timestamp)
        person?.let {
            designerDao.updateImageUrls(it.id, it.imageUrl, it.thumbnailUrl, it.imagesUpdatedTimestamp ?: Date())
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
        val timestamp = Date()
        val collection = collectionDao.loadCollectionForDesigner(designerId).map { it.second.mapToModel(it.first.mapToModel()) }
        val stats = PersonStats.fromLinkedCollection(collection, context)
        val realOldScore = if (whitmoreScore == -1) designerDao.loadDesigner(designerId)?.whitmoreScore ?: 0 else whitmoreScore
        if (stats.whitmoreScore != realOldScore) {
            designerDao.updateWhitmoreScore(designerId, stats.whitmoreScore, timestamp)
        }
        stats
    }
}
