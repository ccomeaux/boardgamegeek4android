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
import com.boardgamegeek.db.PublisherDao
import com.boardgamegeek.entities.BriefGameEntity
import com.boardgamegeek.entities.CompanyEntity
import com.boardgamegeek.entities.PersonStatsEntity
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.model.CompanyResponse2
import com.boardgamegeek.livedata.CalculatingListLoader
import com.boardgamegeek.livedata.RefreshableResourceLoader
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.Publishers
import retrofit2.Call
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class PublisherRepository(val application: BggApplication) {
    private val dao = PublisherDao(application)
    private var loader = getLoader(PublisherDao.SortType.NAME)
    private val sort = MutableLiveData<PublisherDao.SortType>()
    private val prefs: SharedPreferences by lazy { application.preferences() }

    fun loadPublishers(sortBy: PublisherDao.SortType): LiveData<List<CompanyEntity>> {
        loader = getLoader(sortBy)
        sort.value = sortBy
        return loader.asLiveData()
    }

    val progress: LiveData<Pair<Int, Int>> = Transformations.switchMap(sort) {
        loader.progress
    }

    private fun getLoader(sortBy: PublisherDao.SortType): CalculatingListLoader<CompanyEntity> {
        return object : CalculatingListLoader<CompanyEntity>(application) {
            override fun loadFromDatabase() = dao.loadPublishersAsLiveData(sortBy)

            override fun shouldCalculate(data: List<CompanyEntity>?): Boolean {
                val lastCalculated = prefs[PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_PUBLISHERS, 0L]
                        ?: 0L
                return data != null && lastCalculated.isOlderThan(1, TimeUnit.HOURS)
            }

            override fun sortList(data: List<CompanyEntity>?) = data?.sortedBy { it.statsUpdatedTimestamp }

            override fun calculate(data: CompanyEntity) {
                if (data.statsUpdatedTimestamp > data.updatedTimestamp) return
                val collection = dao.loadCollection(data.id)
                val statsEntity = PersonStatsEntity.fromLinkedCollection(collection, application)
                updateWhitmoreScore(data.id, statsEntity.whitmoreScore, data.whitmoreScore)
            }

            override fun finishCalculating() {
                prefs[PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_PUBLISHERS] = System.currentTimeMillis()
            }
        }
    }

    fun loadPublisher(id: Int): LiveData<RefreshableResource<CompanyEntity>> {
        val started = AtomicBoolean()
        val mediatorLiveData = MediatorLiveData<RefreshableResource<CompanyEntity>>()
        val liveData = object : RefreshableResourceLoader<CompanyEntity, CompanyResponse2>(application) {
            override fun loadFromDatabase(): LiveData<CompanyEntity> {
                return dao.loadPublisherAsLiveData(id)
            }

            override fun shouldRefresh(data: CompanyEntity?): Boolean {
                return data == null ||
                        data.id == 0 ||
                        data.id == BggContract.INVALID_ID ||
                        data.updatedTimestamp.isOlderThan(1, TimeUnit.DAYS)
            }

            override val typeDescriptionResId = R.string.title_publisher

            override fun createCall(page: Int): Call<CompanyResponse2> {
                return Adapter.createForXml().company(id)
            }

            override fun saveCallResult(result: CompanyResponse2) {
                dao.savePublisher(result)
            }
        }.asLiveData()
        mediatorLiveData.addSource(liveData) {
            it?.data?.maybeRefreshHeroImageUrl("publisher", started) { url ->
                application.appExecutors.diskIO.execute {
                    dao.update(id, ContentValues().apply {
                        put(Publishers.PUBLISHER_HERO_IMAGE_URL, url)
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
        val realOldScore = if (oldScore == -1) dao.loadPublisher(id)?.whitmoreScore
                ?: 0 else oldScore
        if (newScore != realOldScore) {
            dao.update(id, ContentValues().apply {
                put(Publishers.WHITMORE_SCORE, newScore)
                put(Publishers.PUBLISHER_STATS_UPDATED_TIMESTAMP, System.currentTimeMillis())
            })
        }
    }
}
