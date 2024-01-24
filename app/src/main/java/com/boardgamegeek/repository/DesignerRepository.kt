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
import com.boardgamegeek.io.safeApiCall
import com.boardgamegeek.mappers.*
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.Person.Companion.applySort
import com.boardgamegeek.provider.BggContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
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
    enum class CollectionSortType {
        NAME, RATING
    }

    fun loadDesignersFlow(sortBy: Person.SortType): Flow<List<Person>> {
        return designerDao.loadDesignersFlow()
            .map { it.map { entity -> entity.mapToModel() } }
            .flowOn(Dispatchers.Default)
            .map { it.applySort(sortBy) }
            .flowOn(Dispatchers.Default)
            .conflate()
    }

    fun loadDesignerFlow(designerId: Int) = designerDao.loadDesignerFlow(designerId).map { it?.mapToModel() }.flowOn(Dispatchers.Default)

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
        if (designerId == BggContract.INVALID_ID) return@withContext
        val timestamp = Date()
        val response = safeApiCall(context) { api.person(BggService.PersonType.DESIGNER, designerId) }
        if (response.isSuccess) {
            response.getOrNull()?.mapToModel(designerId, timestamp)?.let {
                val designer = designerDao.loadDesigner(it.id)
                if (designer == null) {
                    designerDao.insert(it.mapDesignerForUpsert())
                } else {
                    designerDao.update(it.mapDesignerForUpsert(designer.internalId))
                }
                refreshImages(it)
            }
        }
    }

    suspend fun refreshMissingImages(limit: Int = 10) = withContext(Dispatchers.Default) {
        withContext(Dispatchers.IO) { designerDao.loadDesigners() }
            .filter { it.designerThumbnailUrl.isNullOrBlank() }
            .sortedByDescending { it.whitmoreScore }
            .take(limit.coerceIn(0, 25))
            .map { it.mapToModel() }
            .forEach {
                Timber.i("Refreshing missing images for designer $it")
                refreshImages(it)
                delay(2_000)
            }
    }

    private suspend fun refreshImages(designer: Person) = withContext(Dispatchers.IO) {
        val timestamp = Date()
        val response = safeApiCall(context) { api.person(designer.id) }
        if (response.isSuccess) {
            response.getOrNull()?.items?.firstOrNull()?.mapToModel(designer, timestamp)?.let { person ->
                designerDao.updateImageUrls(person.id, person.imageUrl, person.thumbnailUrl, person.imagesUpdatedTimestamp ?: timestamp)
                val newThumbnailId = person.thumbnailUrl.getImageId()
                if (newThumbnailId != designer.heroImageUrl.getImageId()) {
                    Timber.d("Designer $designer's hero URL doesn't match thumbnail ID $newThumbnailId")
                    val urlMap = imageRepository.getImageUrls(newThumbnailId)
                    urlMap[ImageRepository.ImageType.HERO]?.firstOrNull()?.let { heroUrl ->
                        Timber.d("Updating designer $designer with hero URL $heroUrl")
                        designerDao.updateHeroImageUrl(designer.id, heroUrl)
                    }
                }
            }
        }
    }

    suspend fun calculateStats(progress: MutableLiveData<Pair<Int, Int>>) = withContext(Dispatchers.Default) {
        val designers = withContext(Dispatchers.IO) { designerDao.loadDesigners() }
            .map { it.mapToModel() }
            .sortedWith(
                compareBy<Person> { it.statsUpdatedTimestamp}
                    .thenByDescending { it.itemCount }
            )
        val maxProgress = designers.size
        designers.forEachIndexed { i, designer ->
            progress.postValue(i to maxProgress)
            calculateStats(designer.id, designer.whitmoreScore)
            Timber.i("Updated stats for designer $designer")
        }
        context.preferences()[PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_DESIGNERS] = System.currentTimeMillis()
        progress.postValue(0 to 0)
    }

    suspend fun calculateStats(designerId: Int, whitmoreScore: Int? = null): PersonStats? = withContext(Dispatchers.Default) {
        if (designerId == BggContract.INVALID_ID) return@withContext null
        val timestamp = Date()
        val collection = withContext(Dispatchers.IO) { collectionDao.loadForDesigner(designerId) }.map { it.mapToModel() }
        val stats = PersonStats.fromLinkedCollection(collection, context)
        val currentWhitmoreScore = (whitmoreScore ?: withContext(Dispatchers.IO) { designerDao.loadDesigner(designerId) }?.whitmoreScore ?: 0).coerceAtLeast(0)
        if (stats.whitmoreScore != currentWhitmoreScore) {
            withContext(Dispatchers.IO) { designerDao.updateWhitmoreScore(designerId, stats.whitmoreScore, timestamp) }
        }
        stats
    }
}
