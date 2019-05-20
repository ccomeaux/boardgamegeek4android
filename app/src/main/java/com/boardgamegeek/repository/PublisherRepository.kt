package com.boardgamegeek.repository

import android.content.ContentValues
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.db.PublisherDao
import com.boardgamegeek.entities.BriefGameEntity
import com.boardgamegeek.entities.CompanyEntity
import com.boardgamegeek.entities.PersonStatsEntity
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.model.CompanyResponse2
import com.boardgamegeek.livedata.RefreshableResourceLoader
import com.boardgamegeek.provider.BggContract
import retrofit2.Call
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class PublisherRepository(val application: BggApplication) {
    private val publisherDao = PublisherDao(application)

    fun loadPublishers(sortBy: PublisherDao.SortType = PublisherDao.SortType.NAME): LiveData<List<CompanyEntity>> {
        return publisherDao.loadPublishersAsLiveData(sortBy)
    }

    fun loadPublisher(id: Int): LiveData<RefreshableResource<CompanyEntity>> {
        val started = AtomicBoolean()
        val mediatorLiveData = MediatorLiveData<RefreshableResource<CompanyEntity>>()
        val liveData = object : RefreshableResourceLoader<CompanyEntity, CompanyResponse2>(application) {
            override fun loadFromDatabase(): LiveData<CompanyEntity> {
                return publisherDao.loadPublisherAsLiveData(id)
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
                publisherDao.savePublisher(result)
            }
        }.asLiveData()
        mediatorLiveData.addSource(liveData) {
            it?.data?.maybeRefreshHeroImageUrl("publisher", started) { url ->
                application.appExecutors.diskIO.execute {
                    publisherDao.update(id, ContentValues().apply {
                        put(BggContract.Publishers.PUBLISHER_HERO_IMAGE_URL, url)
                    })
                }
            }
            mediatorLiveData.value = it
        }
        return mediatorLiveData
    }

    fun loadCollection(id: Int, sortBy: CollectionDao.SortType): LiveData<List<BriefGameEntity>>? {
        return publisherDao.loadCollectionAsLiveData(id, sortBy)
    }

    fun calculateStats(id: Int): LiveData<PersonStatsEntity> {
        val mediatorLiveData = MediatorLiveData<PersonStatsEntity>()
        mediatorLiveData.addSource(publisherDao.loadCollectionAsLiveData(id)) { collection ->
            mediatorLiveData.value = PersonStatsEntity.fromLinkedCollection(collection, application)
        }
        return mediatorLiveData
    }
}
