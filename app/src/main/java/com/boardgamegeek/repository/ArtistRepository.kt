package com.boardgamegeek.repository

import android.content.ContentValues
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.db.ArtistDao
import com.boardgamegeek.entities.*
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.model.PersonResponse2
import com.boardgamegeek.livedata.RefreshableResourceLoader
import com.boardgamegeek.model.Person
import com.boardgamegeek.provider.BggContract
import retrofit2.Call
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class ArtistRepository(val application: BggApplication) {
    private val artistDao = ArtistDao(application)

    fun loadArtist(id: Int): LiveData<RefreshableResource<ArtistEntity>> {
        return object : RefreshableResourceLoader<ArtistEntity, Person>(application) {
            override fun loadFromDatabase(): LiveData<ArtistEntity> {
                return artistDao.loadArtistAsLiveData(id)
            }

            override fun shouldRefresh(data: ArtistEntity?): Boolean {
                return data == null ||
                        data.id == 0 ||
                        data.id == BggContract.INVALID_ID ||
                        data.updatedTimestamp.isOlderThan(1, TimeUnit.DAYS)
            }

            override val typeDescriptionResId = R.string.title_buddy

            override fun createCall(page: Int): Call<Person> {
                return Adapter.createForXml().person(BggService.PERSON_TYPE_ARTIST, id)
            }

            override fun saveCallResult(result: Person) {
                artistDao.saveArtist(id, result)
            }
        }.asLiveData()
    }

    fun loadArtistImages(id: Int): LiveData<RefreshableResource<ArtistImagesEntity>> {
        val started = AtomicBoolean()
        val mediatorLiveData = MediatorLiveData<RefreshableResource<ArtistImagesEntity>>()
        val liveData = object : RefreshableResourceLoader<ArtistImagesEntity, PersonResponse2>(application) {
            override fun loadFromDatabase(): LiveData<ArtistImagesEntity> {
                return artistDao.loadArtistImagesAsLiveData(id)
            }

            override fun shouldRefresh(data: ArtistImagesEntity?): Boolean {
                return data == null ||
                        data.id == 0 ||
                        data.id == BggContract.INVALID_ID ||
                        data.updatedTimestamp.isOlderThan(1, TimeUnit.DAYS)
            }

            override val typeDescriptionResId = R.string.title_buddy

            override fun createCall(page: Int): Call<PersonResponse2> {
                return Adapter.createForXml().person(id)
            }

            override fun saveCallResult(result: PersonResponse2) {
                artistDao.saveArtistImage(id, result)
            }
        }.asLiveData()
        mediatorLiveData.addSource(liveData) {
            it?.data?.maybeRefreshHeroImageUrl("artist", started) { url ->
                application.appExecutors.diskIO.execute {
                    artistDao.update(id, ContentValues().apply {
                        put(BggContract.Artists.ARTIST_HERO_IMAGE_URL, url)
                    })
                }
            }
            mediatorLiveData.value = it
        }
        return mediatorLiveData
    }

    fun loadCollection(id: Int): LiveData<List<ArtistGameEntity>>? {
        return artistDao.loadCollectionAsLiveData(id)
    }
}
