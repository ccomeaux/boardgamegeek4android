package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.map
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.entities.*
import com.boardgamegeek.livedata.AbsentLiveData
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.ArtistRepository
import com.boardgamegeek.repository.DesignerRepository
import com.boardgamegeek.repository.PublisherRepository

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

    private val publisher: LiveData<RefreshableResource<CompanyEntity>> = _person.switchMap() { publisher ->
        if (publisher.type == PersonType.PUBLISHER && publisher.id != BggContract.INVALID_ID) {
            publisherRepository.loadPublisher(publisher.id)
        } else {
            AbsentLiveData.create()
        }
    }

    val details: LiveData<RefreshableResource<PersonEntity>> = _person.switchMap() { person ->
        when (person.id) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> {
                when (person.type) {
                    PersonType.ARTIST -> artistRepository.loadArtist(person.id)
                    PersonType.DESIGNER -> designerRepository.loadDesigner(person.id)
                    PersonType.PUBLISHER -> {
                        publisher.map(publisher) { company ->
                            RefreshableResource.map(company, company.data?.let {
                                PersonEntity(
                                        it.id,
                                        it.name,
                                        it.description,
                                        it.updatedTimestamp
                                )
                            })
                        }
                    }
                }
            }
        }
    }

    val images: LiveData<RefreshableResource<PersonImagesEntity>> = _person.switchMap() { person ->
        when (person.id) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> {
                when (person.type) {
                    PersonType.ARTIST -> artistRepository.loadArtistImages(person.id)
                    PersonType.DESIGNER -> designerRepository.loadDesignerImages(person.id)
                    PersonType.PUBLISHER -> {
                        publisher.map() { company ->
                            RefreshableResource.map(company, company.data?.let {
                                PersonImagesEntity(
                                        it.id,
                                        it.imageUrl,
                                        it.thumbnailUrl,
                                        it.heroImageUrl,
                                        it.updatedTimestamp
                                )
                            })
                        }
                    }
                }
            }
        }
    }

    val sort: LiveData<CollectionSort> = _person.map() {
        it.sort
    }

    val collection: LiveData<List<BriefGameEntity>> = _person.switchMap() { person ->
        when (person.id) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> {
                val sortBy = when (person.sort) {
                    CollectionSort.NAME -> CollectionDao.SortType.NAME
                    CollectionSort.RATING -> CollectionDao.SortType.RATING
                }
                when (person.type) {
                    PersonType.ARTIST -> artistRepository.loadCollection(person.id, sortBy)
                    PersonType.DESIGNER -> designerRepository.loadCollection(person.id, sortBy)
                    PersonType.PUBLISHER -> publisherRepository.loadCollection(person.id, sortBy)
                }
            }
        }
    }

    val stats: LiveData<PersonStatsEntity> = _person.switchMap() { person ->
        when (person.id) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> {
                when (person.type) {
                    PersonType.ARTIST -> artistRepository.calculateStats(person.id)
                    PersonType.DESIGNER -> designerRepository.calculateStats(person.id)
                    PersonType.PUBLISHER -> publisherRepository.calculateStats(person.id)
                }
            }
        }
    }

    fun refresh() {
        _person.value?.let { _person.value = Person(it.type, it.id) }
    }
}
