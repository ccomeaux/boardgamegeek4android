package com.boardgamegeek.repository

import androidx.lifecycle.LiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.db.ArtistDao
import com.boardgamegeek.entities.ArtistEntity
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.BggService
import com.boardgamegeek.livedata.RefreshableResourceLoader
import com.boardgamegeek.model.Person
import com.boardgamegeek.provider.BggContract
import retrofit2.Call
import java.util.concurrent.TimeUnit

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
}