package com.boardgamegeek.repository

import android.content.ContentValues
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.db.DesignerDao
import com.boardgamegeek.entities.*
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.model.Person
import com.boardgamegeek.io.model.PersonResponse2
import com.boardgamegeek.livedata.RefreshableResourceLoader
import com.boardgamegeek.provider.BggContract
import retrofit2.Call
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class DesignerRepository(val application: BggApplication) {
    private val designerDao = DesignerDao(application)

    fun loadDesigners(sortBy: DesignerDao.SortType = DesignerDao.SortType.NAME): LiveData<List<PersonEntity>> {
        return designerDao.loadDesignersAsLiveData(sortBy)
    }

    fun loadDesigner(id: Int): LiveData<RefreshableResource<PersonEntity>> {
        return object : RefreshableResourceLoader<PersonEntity, Person>(application) {
            override fun loadFromDatabase(): LiveData<PersonEntity> {
                return designerDao.loadDesignerAsLiveData(id)
            }

            override fun shouldRefresh(data: PersonEntity?): Boolean {
                return data == null ||
                        data.id == 0 ||
                        data.id == BggContract.INVALID_ID ||
                        data.updatedTimestamp.isOlderThan(1, TimeUnit.DAYS)
            }

            override val typeDescriptionResId = R.string.title_designer

            override fun createCall(page: Int): Call<Person> {
                return Adapter.createForXml().person(BggService.PERSON_TYPE_DESIGNER, id)
            }

            override fun saveCallResult(result: Person) {
                designerDao.saveDesigner(id, result)
            }
        }.asLiveData()
    }

    fun loadDesignerImages(id: Int): LiveData<RefreshableResource<PersonImagesEntity>> {
        val started = AtomicBoolean()
        val mediatorLiveData = MediatorLiveData<RefreshableResource<PersonImagesEntity>>()
        val liveData = object : RefreshableResourceLoader<PersonImagesEntity, PersonResponse2>(application) {
            override fun loadFromDatabase(): LiveData<PersonImagesEntity> {
                return designerDao.loadDesignerImagesAsLiveData(id)
            }

            override fun shouldRefresh(data: PersonImagesEntity?): Boolean {
                return data == null ||
                        data.id == 0 ||
                        data.id == BggContract.INVALID_ID ||
                        data.updatedTimestamp.isOlderThan(1, TimeUnit.DAYS)
            }

            override val typeDescriptionResId = R.string.title_designer

            override fun createCall(page: Int): Call<PersonResponse2> {
                return Adapter.createForXml().person(id)
            }

            override fun saveCallResult(result: PersonResponse2) {
                designerDao.saveDesignerImage(id, result)
            }
        }.asLiveData()
        mediatorLiveData.addSource(liveData) {
            it?.data?.maybeRefreshHeroImageUrl("designer", started) { url ->
                application.appExecutors.diskIO.execute {
                    designerDao.update(id, ContentValues().apply {
                        put(BggContract.Designers.DESIGNER_HERO_IMAGE_URL, url)
                    })
                }
            }
            mediatorLiveData.value = it
        }
        return mediatorLiveData
    }

    fun loadCollection(id: Int, sortBy: CollectionDao.SortType = CollectionDao.SortType.RATING): LiveData<List<BriefGameEntity>>? {
        return designerDao.loadCollectionAsLiveData(id, sortBy)
    }

    fun calculateStats(id: Int): LiveData<PersonStatsEntity> {
        val mediatorLiveData = MediatorLiveData<PersonStatsEntity>()
        mediatorLiveData.addSource(designerDao.loadCollectionAsLiveData(id)) { collection ->
            mediatorLiveData.value = PersonStatsEntity.fromLinkedCollection(collection, application)
        }
        return mediatorLiveData
    }
}
