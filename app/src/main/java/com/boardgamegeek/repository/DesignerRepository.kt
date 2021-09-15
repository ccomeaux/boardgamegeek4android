package com.boardgamegeek.repository

import android.content.ContentValues
import android.content.SharedPreferences
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.db.DesignerDao
import com.boardgamegeek.entities.*
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.model.Person
import com.boardgamegeek.io.model.PersonResponse2
import com.boardgamegeek.livedata.CalculatingListLoader
import com.boardgamegeek.livedata.RefreshableResourceLoader
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.Designers
import retrofit2.Call
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class DesignerRepository(val application: BggApplication) {
    private val dao = DesignerDao(application)
    private var loader = getLoader(DesignerDao.SortType.NAME)
    private val sort = MutableLiveData<DesignerDao.SortType>()
    private val prefs: SharedPreferences by lazy { application.preferences() }

    fun loadDesigners(sortBy: DesignerDao.SortType): LiveData<List<PersonEntity>> {
        loader = getLoader(sortBy)
        sort.value = sortBy
        return loader.asLiveData()
    }

    val progress: LiveData<Pair<Int, Int>> = Transformations.switchMap(sort) {
        loader.progress
    }

    private fun getLoader(sortBy: DesignerDao.SortType): CalculatingListLoader<PersonEntity> {
        return object : CalculatingListLoader<PersonEntity>(application) {
            override fun loadFromDatabase() = dao.loadDesignersAsLiveData(sortBy)

            override fun shouldCalculate(data: List<PersonEntity>?): Boolean {
                val lastCalculated = prefs[PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_DESIGNERS, 0L]
                        ?: 0L
                return data != null && lastCalculated.isOlderThan(1, TimeUnit.HOURS)
            }

            override fun sortList(data: List<PersonEntity>?) = data?.sortedBy { it.statsUpdatedTimestamp }

            override fun calculate(data: PersonEntity) {
                if (data.statsUpdatedTimestamp > data.updatedTimestamp) return
                val collection = dao.loadCollection(data.id)
                val statsEntity = PersonStatsEntity.fromLinkedCollection(collection, application)
                updateWhitmoreScore(data.id, statsEntity.whitmoreScore, data.whitmoreScore)
            }

            override fun finishCalculating() {
                prefs[PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_DESIGNERS] = System.currentTimeMillis()
            }
        }
    }

    fun loadDesigner(id: Int): LiveData<RefreshableResource<PersonEntity>> {
        return object : RefreshableResourceLoader<PersonEntity, Person>(application) {
            override fun loadFromDatabase(): LiveData<PersonEntity> {
                return dao.loadDesignerAsLiveData(id)
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
                dao.saveDesigner(id, result)
            }
        }.asLiveData()
    }

    fun loadDesignerImages(id: Int): LiveData<RefreshableResource<PersonImagesEntity>> {
        val started = AtomicBoolean()
        val mediatorLiveData = MediatorLiveData<RefreshableResource<PersonImagesEntity>>()
        val liveData = object : RefreshableResourceLoader<PersonImagesEntity, PersonResponse2>(application) {
            override fun loadFromDatabase(): LiveData<PersonImagesEntity> {
                return dao.loadDesignerImagesAsLiveData(id)
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
                dao.saveDesignerImage(id, result)
            }
        }.asLiveData()
        mediatorLiveData.addSource(liveData) {
            it?.data?.maybeRefreshHeroImageUrl("designer", started) { url ->
                application.appExecutors.diskIO.execute {
                    dao.update(id, ContentValues().apply {
                        put(Designers.DESIGNER_HERO_IMAGE_URL, url)
                    })
                }
            }
            mediatorLiveData.value = it
        }
        return mediatorLiveData
    }

    fun loadCollection(id: Int, sortBy: CollectionDao.SortType): LiveData<List<BriefGameEntity>>? {
        return dao.loadCollectionAsLiveData(id, sortBy)
    }

    fun calculateStats(id: Int): LiveData<PersonStatsEntity> {
        val mediatorLiveData = MediatorLiveData<PersonStatsEntity>()
        mediatorLiveData.addSource(dao.loadCollectionAsLiveData(id)) { collection ->
            val linkedCollection = PersonStatsEntity.fromLinkedCollection(collection, application)
            mediatorLiveData.value = linkedCollection
            application.appExecutors.diskIO.execute {
                updateWhitmoreScore(id, linkedCollection.whitmoreScore, -1)
            }
        }
        return mediatorLiveData
    }

    @WorkerThread
    private fun updateWhitmoreScore(id: Int, newScore: Int, oldScore: Int) {
        val realOldScore = if (oldScore == -1)
            dao.loadDesigner(id)?.whitmoreScore ?: 0
        else oldScore
        if (newScore != realOldScore) {
            dao.update(id, ContentValues().apply {
                put(Designers.WHITMORE_SCORE, newScore)
                put(Designers.DESIGNER_STATS_UPDATED_TIMESTAMP, System.currentTimeMillis())
            })
        }
    }
}
