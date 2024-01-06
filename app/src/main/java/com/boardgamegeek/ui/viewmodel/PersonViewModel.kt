package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.model.Company
import com.boardgamegeek.model.Person
import com.boardgamegeek.model.RefreshableResource
import com.boardgamegeek.extensions.getImageId
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.ArtistRepository
import com.boardgamegeek.repository.DesignerRepository
import com.boardgamegeek.repository.PublisherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PersonViewModel @Inject constructor(
    application: Application,
    private val artistRepository: ArtistRepository,
    private val designerRepository: DesignerRepository,
    private val publisherRepository: PublisherRepository,
) : AndroidViewModel(application) {
    data class PersonInfo(
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

    private val _personInfo = MutableLiveData<PersonInfo>()

    fun setArtistId(artistId: Int) {
        if (_personInfo.value?.type != PersonType.ARTIST || _personInfo.value?.id != artistId) _personInfo.value = PersonInfo(PersonType.ARTIST, artistId)
    }

    fun setDesignerId(designerId: Int) {
        if (_personInfo.value?.type != PersonType.DESIGNER || _personInfo.value?.id != designerId) _personInfo.value = PersonInfo(PersonType.DESIGNER, designerId)
    }

    fun setPublisherId(publisherId: Int) {
        if (_personInfo.value?.type != PersonType.PUBLISHER || _personInfo.value?.id != publisherId) _personInfo.value = PersonInfo(PersonType.PUBLISHER, publisherId)
    }

    fun sort(sortType: CollectionSort) {
        if (_personInfo.value?.sort != sortType) {
            _personInfo.value = PersonInfo(
                _personInfo.value?.type ?: PersonType.DESIGNER,
                _personInfo.value?.id ?: BggContract.INVALID_ID,
                sortType
            )
        }
    }

    fun refresh() {
        _personInfo.value?.let { info ->
            val id = info.id
            viewModelScope.launch {
                when (info.type) {
                    // TODO - look at dates and optionally allow an override
                    PersonType.ARTIST -> {
                        artistRepository.refreshArtist(id)
                        artistRepository.loadArtist(id)?.let { artistRepository.refreshImages(it) }
                    }
                    PersonType.DESIGNER -> {
                        designerRepository.refreshDesigner(id)
                        designerRepository.loadDesigner(id)?.let { designerRepository.refreshImages(it) }
                    }
                    PersonType.PUBLISHER -> {
                        publisherRepository.refreshPublisher(id)
                        publisherRepository.loadPublisher(id)?.let {
                            val imageId = it.thumbnailUrl.getImageId()
                            if (imageId != it.heroImageUrl.getImageId()) {
                                publisherRepository.refreshImages(it)
                            }
                        }
                    }
                }
            }
        }
    }

    val details = _personInfo.switchMap { person ->
        liveData {
            when (person.id) {
                BggContract.INVALID_ID -> emit(RefreshableResource.success(null))
                else -> {
                    when (person.type) {
                        PersonType.ARTIST -> loadArtist(person.id, application)
                        PersonType.DESIGNER -> loadDesigner(person.id, application)
                        PersonType.PUBLISHER -> loadPublisher(person.id, application)
                    }
                }
            }
        }.distinctUntilChanged()
    }

    private suspend fun LiveDataScope<RefreshableResource<Person>>.loadArtist(artistId: Int, application: Application) {
        try {
            emit(RefreshableResource.refreshing(latestValue?.data))
            emitSource(artistRepository.loadArtistAsLiveData(artistId).map { RefreshableResource.success(it) })
        } catch (e: Exception) {
            emit(RefreshableResource.error(e, application, latestValue?.data))
        }
    }

    private suspend fun LiveDataScope<RefreshableResource<Person>>.loadDesigner(designerId: Int, application: Application) {
        try {
            emit(RefreshableResource.refreshing(latestValue?.data))
            emitSource(designerRepository.loadDesignerAsLiveData(designerId).map { RefreshableResource.success(it) })
        } catch (e: Exception) {
            emit(RefreshableResource.error(e, application, latestValue?.data))
        }
    }

    private suspend fun LiveDataScope<RefreshableResource<Person>>.loadPublisher(publisherId: Int, application: Application) {
        try {
            emit(RefreshableResource.refreshing(latestValue?.data))
            emitSource(publisherRepository.loadPublisherAsLiveData(publisherId).map { RefreshableResource.success(it.mapToPerson()) })
        } catch (e: Exception) {
            emit(RefreshableResource.error(e, application, latestValue?.data))
        }
    }

    private fun Company?.mapToPerson() = this?.let {
        Person(
            BggContract.INVALID_ID.toLong(),
            it.id,
            it.name,
            it.description,
            it.updatedTimestamp,
            it.thumbnailUrl,
            it.heroImageUrl,
        )
    }

    val type: LiveData<PersonType> = _personInfo.map { it.type }

    val id: LiveData<Int> = _personInfo.map { it.id }

    val collectionSort: LiveData<CollectionSort> = _personInfo.map { it.sort }

    val collection = _personInfo.switchMap { person ->
        liveData {
            emitSource(
                when (person.type) {
                    PersonType.ARTIST -> artistRepository.loadCollection(person.id, if (person.sort == CollectionSort.RATING) ArtistRepository.CollectionSortType.RATING else ArtistRepository.CollectionSortType.NAME)
                    PersonType.DESIGNER -> designerRepository.loadCollection(person.id, if (person.sort == CollectionSort.RATING) DesignerRepository.CollectionSortType.RATING else DesignerRepository.CollectionSortType.NAME)
                    PersonType.PUBLISHER -> publisherRepository.loadCollection(person.id, if (person.sort == CollectionSort.RATING) PublisherRepository.CollectionSortType.RATING else PublisherRepository.CollectionSortType.NAME)
                }
            )
        }
    }

    val stats = _personInfo.switchMap { person ->
        liveData {
            when (person.id) {
                BggContract.INVALID_ID -> emit(null)
                else -> {
                    emit(
                        when (person.type) {
                            PersonType.ARTIST -> artistRepository.calculateStats(person.id)
                            PersonType.DESIGNER -> designerRepository.calculateStats(person.id)
                            PersonType.PUBLISHER -> publisherRepository.calculateStats(person.id)
                        }
                    )
                }
            }
        }
    }
}
