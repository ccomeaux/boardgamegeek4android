package com.boardgamegeek.repository

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.db.DesignerDao
import com.boardgamegeek.model.CollectionItem.Companion.filterBySyncedStatues
import com.boardgamegeek.model.Person
import com.boardgamegeek.model.PersonStats
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.BggService
import com.boardgamegeek.mappers.*
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.provider.BggContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Date

class DesignerRepository(
    val context: Context,
    private val api: BggService,
    private val designerDao: DesignerDao,
    private val collectionDao: CollectionDao,
    private val imageRepository: ImageRepository,
) {
    enum class SortType {
        NAME, ITEM_COUNT, WHITMORE_SCORE
    }

    enum class CollectionSortType {
        NAME, RATING
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

    fun loadDesignerAsLiveData(designerId: Int) = designerDao.loadDesignerAsLiveData(designerId).map { it?.mapToModel() }

    suspend fun loadCollection(id: Int, sortBy: CollectionSortType): List<CollectionItem> {
        if (id == BggContract.INVALID_ID) return emptyList()
        return collectionDao.loadForDesigner(id)
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

    suspend fun refreshImages(designer: Person) = withContext(Dispatchers.IO) {
        val timestamp = Date()
        val response = api.person(designer.id)
        val person = response.items.firstOrNull()?.mapToModel(designer, timestamp)
        person?.let {
            designerDao.updateImageUrls(it.id, it.imageUrl, it.thumbnailUrl, it.imagesUpdatedTimestamp ?: timestamp)
            val imageId = it.thumbnailUrl.getImageId()
            if (imageId != designer.heroImageUrl.getImageId()) {
                Timber.d("Designer $designer's hero URL doesn't match image ID $imageId")
                val urlMap = imageRepository.getImageUrls(imageId)
                urlMap[ImageRepository.ImageType.HERO]?.firstOrNull()?.let { heroUrl ->
                    Timber.d("Updating designer $designer with hero URL $heroUrl")
                    designerDao.updateHeroImageUrl(designer.id, heroUrl)
                }
            }
        }
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
        val collection = collectionDao.loadForDesigner(designerId).map { it.mapToModel() }
        val stats = PersonStats.fromLinkedCollection(collection, context)
        val realOldScore = if (whitmoreScore == -1) designerDao.loadDesigner(designerId)?.whitmoreScore ?: 0 else whitmoreScore
        if (stats.whitmoreScore != realOldScore) {
            designerDao.updateWhitmoreScore(designerId, stats.whitmoreScore, timestamp)
        }
        stats
    }
}
