package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.*
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.entities.*
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.livedata.AbsentLiveData
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.ArtistRepository
import com.boardgamegeek.repository.DesignerRepository
import com.boardgamegeek.repository.PublisherRepository
import retrofit2.HttpException
import java.util.concurrent.TimeUnit

class PersonViewModel(application: Application) : AndroidViewModel(application) {
    data class Person(
            val type: PersonType,
            val id: Int,
            val sort: CollectionSort = CollectionSort.RATING
    )

    enum class PersonType {
        ARTIST,
        DESIGNER,
        PUBLISHER
    }

    enum class CollectionSort {
        NAME, RATING
    }

    private val artistRepository = ArtistRepository(getApplication())
    private val designerRepository = DesignerRepository(getApplication())
    private val publisherRepository = PublisherRepository(getApplication())

    private val _person = MutableLiveData<Person>()
    val person: LiveData<Person>
        get() = _person

    fun setArtistId(artistId: Int) {
        if (_person.value?.type != PersonType.ARTIST && _person.value?.id != artistId) _person.value = Person(PersonType.ARTIST, artistId)
    }

    fun setDesignerId(designerId: Int) {
        if (_person.value?.type != PersonType.DESIGNER && _person.value?.id != designerId) _person.value = Person(PersonType.DESIGNER, designerId)
    }

    fun setPublisherId(publisherId: Int) {
        if (_person.value?.type != PersonType.PUBLISHER && _person.value?.id != publisherId) _person.value = Person(PersonType.PUBLISHER, publisherId)
    }

    fun sort(sortType: CollectionSort) {
        if (_person.value?.sort != sortType) {
            _person.value = Person(
                    _person.value?.type ?: PersonType.DESIGNER,
                    _person.value?.id ?: BggContract.INVALID_ID,
                    sortType)
        }
    }

    fun refresh() {
        _person.value?.let { _person.value = Person(it.type, it.id) }
    }

    val details = _person.switchMap { person ->
        when (person.id) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> {
                when (person.type) {
                    PersonType.ARTIST -> artistRepository.loadArtist(person.id)
                    PersonType.DESIGNER -> designerRepository.loadDesigner(person.id)
                    PersonType.PUBLISHER -> {
                        liveData {
                            loadPublisher(person, application)
                        }
                    }
                }
            }
        }
    }

    private suspend fun LiveDataScope<RefreshableResource<PersonEntity>>.loadPublisher(person: Person, application: Application) {
        val publisher = publisherRepository.loadPublisher(person.id)
        if (publisher == null || publisher.updatedTimestamp.isOlderThan(1, TimeUnit.DAYS)) {
            emit(RefreshableResource.refreshing(publisher.toPersonEntity()))
            try {
                publisherRepository.refreshPublisher(person.id)?.let { r ->
                    publisherRepository.refreshImages(r)
                    publisherRepository.loadPublisher(r.id)?.let {
                        emit(RefreshableResource.success(it.toPersonEntity()))
                    } ?: emit(RefreshableResource.success(r.toPersonEntity()))
                } ?: emit(RefreshableResource.error(application.getString(R.string.msg_update_invalid_response, application.getString(R.string.title_publisher)), null))
            } catch (e: Exception) {
                emit(RefreshableResource.error(getHttpErrorMessage(e, application), publisher.toPersonEntity()))
            }
        } else {
            emit(RefreshableResource.success(publisher.toPersonEntity()))
        }
    }

    private fun getHttpErrorMessage(exception: Exception, application: Application): String {
        return        when (exception){
            is HttpException ->{
                @StringRes val resId: Int = when {
                    exception.code() >= 500 -> R.string.msg_sync_response_500
                    exception.code() == 429 -> R.string.msg_sync_response_429
                    else -> R.string.msg_sync_error_http_code
                }
                return application.getString(resId, exception.code().toString())
            }
            else -> exception.message ?:application.getString(R.string.msg_sync_error)
        }
    }


    private fun CompanyEntity?.toPersonEntity(): PersonEntity? {
        return this?.let {
            PersonEntity(
                    it.id,
                    it.name,
                    it.description,
                    it.updatedTimestamp,
                    it.thumbnailUrl,
                    it.heroImageUrl,
            )
        }
    }

    val images = _person.switchMap { person ->
        when (person.id) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> {
                when (person.type) {
                    PersonType.ARTIST -> artistRepository.loadArtistImages(person.id)
                    PersonType.DESIGNER -> designerRepository.loadDesignerImages(person.id)
                    PersonType.PUBLISHER -> AbsentLiveData.create()
//                    PersonType.PUBLISHER -> {
//                        publisher.map { company ->
//                            RefreshableResource.map(company, company.data?.let {
//                                PersonImagesEntity(
//                                        it.id,
//                                        it.imageUrl,
//                                        it.thumbnailUrl,
//                                        it.heroImageUrl,
//                                        it.updatedTimestamp
//                                )
//                            })
//                        }
//                    }
                }
            }
        }
    }

    val collectionSort: LiveData<CollectionSort> = _person.map {
        it.sort
    }

    val collection: LiveData<List<BriefGameEntity>> = _person.switchMap { person ->
        when (person.id) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> {
                val sortBy = when (person.sort) {
                    CollectionSort.NAME -> CollectionDao.SortType.NAME
                    CollectionSort.RATING -> CollectionDao.SortType.RATING
                }
                liveData {
                    emit(when (person.type) {
                        PersonType.ARTIST -> artistRepository.loadCollection(person.id, sortBy)
                        PersonType.DESIGNER -> designerRepository.loadCollection(person.id, sortBy)
                        PersonType.PUBLISHER -> publisherRepository.loadCollection(person.id, sortBy)
                    })
                }
            }
        }
    }

    val stats: LiveData<PersonStatsEntity> = _person.switchMap { person ->
        when (person.id) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> {
                liveData {
                    emit(when (person.type) {
                        PersonType.ARTIST -> artistRepository.calculateStats(person.id)
                        PersonType.DESIGNER -> designerRepository.calculateStats(person.id)
                        PersonType.PUBLISHER -> publisherRepository.calculateStats(person.id)
                    })
                }
            }
        }
    }
}
