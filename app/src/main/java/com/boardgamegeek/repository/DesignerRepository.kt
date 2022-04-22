package com.boardgamegeek.repository

import android.content.SharedPreferences
import androidx.core.content.contentValuesOf
import androidx.lifecycle.MutableLiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.db.DesignerDao
import com.boardgamegeek.entities.PersonEntity
import com.boardgamegeek.entities.PersonStatsEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.BggService
import com.boardgamegeek.mappers.mapToEntity
import com.boardgamegeek.provider.BggContract.Designers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DesignerRepository(val application: BggApplication) {
    private val dao = DesignerDao(application)
    private val prefs: SharedPreferences by lazy { application.preferences() }

    suspend fun loadDesigners(sortBy: DesignerDao.SortType) = dao.loadDesigners(sortBy)

    suspend fun loadDesigner(designerId: Int) = dao.loadDesigner(designerId)

    suspend fun loadCollection(id: Int, sortBy: CollectionDao.SortType) = dao.loadCollection(id, sortBy)

    suspend fun delete() = dao.delete()

    suspend fun refreshDesigner(designerId: Int): PersonEntity = withContext(Dispatchers.IO) {
        val response = Adapter.createForXml().person(BggService.PERSON_TYPE_DESIGNER, designerId)
        val missingDesignerMessage = "This page does not exist. You can edit this page to create it."
        dao.upsert(
            designerId, contentValuesOf(
                Designers.Columns.DESIGNER_NAME to response.name,
                Designers.Columns.DESIGNER_DESCRIPTION to (if (response.description == missingDesignerMessage) "" else response.description),
                Designers.Columns.UPDATED to System.currentTimeMillis()
            )
        )
        response.mapToEntity(designerId)
    }

    suspend fun refreshImages(designer: PersonEntity): PersonEntity = withContext(Dispatchers.IO) {
        val response = Adapter.createForXml().person(designer.id)
        response.items.firstOrNull()?.let {
            dao.upsert(
                designer.id, contentValuesOf(
                    Designers.Columns.DESIGNER_THUMBNAIL_URL to it.thumbnail,
                    Designers.Columns.DESIGNER_IMAGE_URL to it.image,
                    Designers.Columns.DESIGNER_IMAGES_UPDATED_TIMESTAMP to System.currentTimeMillis(),
                )
            )
            designer.copy(thumbnailUrl = it.thumbnail.orEmpty(), imageUrl = it.image.orEmpty())
        } ?: designer
    }

    suspend fun refreshHeroImage(designer: PersonEntity): PersonEntity = withContext(Dispatchers.IO) {
        val response = Adapter.createGeekdoApi().image(designer.thumbnailUrl.getImageId())
        val url = response.images.medium.url
        dao.upsert(designer.id, contentValuesOf(Designers.Columns.DESIGNER_HERO_IMAGE_URL to url))
        designer.copy(heroImageUrl = url)
    }

    suspend fun calculateWhitmoreScores(designers: List<PersonEntity>, progress: MutableLiveData<Pair<Int, Int>>) = withContext(Dispatchers.Default) {
        val sortedList = designers.sortedBy { it.statsUpdatedTimestamp }
        val maxProgress = sortedList.size
        sortedList.forEachIndexed { i, data ->
            progress.postValue(i to maxProgress)
            val collection = dao.loadCollection(data.id)
            val statsEntity = PersonStatsEntity.fromLinkedCollection(collection, application)
            updateWhitmoreScore(data.id, statsEntity.whitmoreScore, data.whitmoreScore)
        }
        prefs[PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_DESIGNERS] = System.currentTimeMillis()
        progress.postValue(0 to 0)
    }

    suspend fun calculateStats(designerId: Int): PersonStatsEntity = withContext(Dispatchers.Default) {
        val collection = dao.loadCollection(designerId)
        val linkedCollection = PersonStatsEntity.fromLinkedCollection(collection, application)
        updateWhitmoreScore(designerId, linkedCollection.whitmoreScore)
        linkedCollection
    }

    private suspend fun updateWhitmoreScore(id: Int, newScore: Int, oldScore: Int = -1) = withContext(Dispatchers.IO) {
        val realOldScore = if (oldScore == -1) dao.loadDesigner(id)?.whitmoreScore ?: 0 else oldScore
        if (newScore != realOldScore) {
            dao.upsert(
                id, contentValuesOf(
                    Designers.Columns.WHITMORE_SCORE to newScore,
                    Designers.Columns.DESIGNER_STATS_UPDATED_TIMESTAMP to System.currentTimeMillis(),
                )
            )
        }
    }
}
