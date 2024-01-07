package com.boardgamegeek.repository

import android.content.Context
import androidx.lifecycle.LiveData
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

    suspend fun loadDesignersAsLiveData(sortBy: SortType): LiveData<List<Person>> = withContext(Dispatchers.Default) {
        designerDao.loadDesignersAsLiveData().map {
            it.map { entity -> entity.mapToModel() }
        }.map { list ->
            list.sortedWith(
                when (sortBy) {
                    SortType.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                    SortType.WHITMORE_SCORE -> compareByDescending<Person> { it.whitmoreScore }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                    SortType.ITEM_COUNT -> compareByDescending<Person> { it.itemCount }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                }
            )
        }
    }

    fun loadDesignerAsLiveData(designerId: Int) = designerDao.loadDesignerAsLiveData(designerId).map { it?.mapToModel() }

    suspend fun loadCollection(id: Int, sortBy: CollectionSortType): LiveData<List<CollectionItem>> = withContext(Dispatchers.Default) {
        if (id == BggContract.INVALID_ID) MutableLiveData(emptyList())
        else collectionDao.loadForDesignerAsLiveData(id).map {
            it.map { entity -> entity.mapToModel() }
        }.map { list ->
            list.filter { it.deleteTimestamp == 0L }
                .filter { it.filterBySyncedStatues(context) }
                .sortedWith(
                    if (sortBy == CollectionSortType.RATING)
                        compareByDescending<CollectionItem> { it.rating }
                            .thenByDescending { it.isFavorite }
                            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.sortName }
                    else
                        compareBy(String.CASE_INSENSITIVE_ORDER) { it.sortName }
                )
        }
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) { designerDao.deleteAll() }

    suspend fun refreshDesigner(designerId: Int) = withContext(Dispatchers.IO) {
        val timestamp = Date()
        val response = api.person(BggService.PersonType.DESIGNER, designerId)
        response.mapToModel(designerId, timestamp)?.let {
            val designer = designerDao.loadDesigner(it.id)
            if (designer == null) {
                designerDao.insert(it.mapDesignerForUpsert())
            } else {
                designerDao.update(it.mapDesignerForUpsert(designer.internalId))
            }
            refreshImages(it)
        }
    }

    suspend fun refreshMissingImages(limit: Int = 10) = withContext(Dispatchers.IO) {
        designerDao.loadDesigners()
            .filter { it.designerThumbnailUrl.isNullOrBlank() }
            .map { it.mapToModel() }
            .sortedByDescending { it.whitmoreScore }
            .take(limit.coerceIn(0, 25))
            .forEach {
                Timber.d("Refreshing missing images for designer $it")
                refreshImages(it)
            }
    }

    private suspend fun refreshImages(designer: Person) = withContext(Dispatchers.IO) {
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

    suspend fun calculateWhitmoreScores(progress: MutableLiveData<Pair<Int, Int>>) = withContext(Dispatchers.Default) {
        val designers = withContext(Dispatchers.IO) { designerDao.loadDesigners().map { it.mapToModel() } }
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
        val collection = withContext(Dispatchers.IO) { collectionDao.loadForDesigner(designerId).map { it.mapToModel() } }
        val stats = PersonStats.fromLinkedCollection(collection, context)
        val realOldScore = if (whitmoreScore == -1) designerDao.loadDesigner(designerId)?.whitmoreScore ?: 0 else whitmoreScore
        if (stats.whitmoreScore != realOldScore) {
            designerDao.updateWhitmoreScore(designerId, stats.whitmoreScore, timestamp)
        }
        stats
    }
}
