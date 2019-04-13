package com.boardgamegeek.repository

import android.content.ContentValues
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.db.ArtistDao
import com.boardgamegeek.entities.ArtistEntity
import com.boardgamegeek.entities.ArtistImagesEntity
import com.boardgamegeek.entities.ImagesEntity
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.model.Image
import com.boardgamegeek.io.model.PersonResponse2
import com.boardgamegeek.livedata.RefreshableResourceLoader
import com.boardgamegeek.model.Person
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.util.ImageUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
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
            maybeRefreshHeroImageUrl(it?.data, "artist", started) { url ->
                artistDao.update(id, ContentValues().apply {
                    put(BggContract.Artists.ARTIST_HERO_IMAGE_URL, url)
                })
            }
            mediatorLiveData.value = it
        }
        return mediatorLiveData
    }

    private fun maybeRefreshHeroImageUrl(entity: ImagesEntity?, description: String, started: AtomicBoolean, successListener: (String) -> Unit = {}) {
        if (entity == null) return
        val heroImageId = ImageUtils.getImageId(entity.heroImageUrl)
        val thumbnailId = ImageUtils.getImageId(entity.thumbnailUrl)
        if (heroImageId != thumbnailId && started.compareAndSet(false, true)) {
            val call = Adapter.createGeekdoApi().image(thumbnailId)
            call.enqueue(object : Callback<Image> {
                override fun onResponse(call: Call<Image>?, response: Response<Image>?) {
                    if (response?.isSuccessful == true) {
                        val body = response.body()
                        if (body != null) {
                            application.appExecutors.diskIO.execute {
                                successListener(body.images.medium.url)
                            }
                        } else {
                            Timber.w("Empty body while fetching image $thumbnailId for $description ${entity.id}")
                        }
                    } else {
                        val message = response?.message() ?: response?.code().toString()
                        Timber.w("Unsuccessful response of '$message' while fetching image $thumbnailId for $description ${entity.id}")
                    }
                    started.set(false)
                }

                override fun onFailure(call: Call<Image>?, t: Throwable?) {
                    val message = t?.localizedMessage ?: "Unknown error"
                    Timber.w("Unsuccessful response of '$message' while fetching image $thumbnailId for $description ${entity.id}")
                    started.set(false)
                }
            })
        }
    }
}
