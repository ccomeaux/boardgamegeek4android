package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.model.Company
import com.boardgamegeek.model.Person
import com.boardgamegeek.model.RefreshableResource
import com.boardgamegeek.extensions.getImageId
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.ArtistRepository
import com.boardgamegeek.repository.DesignerRepository
import com.boardgamegeek.repository.PublisherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

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
        _personInfo.value?.let { _personInfo.value = PersonInfo(it.type, it.id) }
    }

    val details = _personInfo.switchMap { person ->
        liveData {
            when (person.id) {
                BggContract.INVALID_ID -> emit(null)
                else -> {
                    when (person.type) {
                        PersonType.ARTIST -> loadArtist(person.id, application)
                        PersonType.DESIGNER -> loadDesigner(person.id, application)
                        PersonType.PUBLISHER -> loadPublisher(person.id, application)
                    }
                }
            }
        }
    }

    private suspend fun LiveDataScope<RefreshableResource<Person>?>.loadArtist(artistId: Int, application: Application) {
        emit(RefreshableResource.refreshing(latestValue?.data))
        val artist = artistRepository.loadArtist(artistId)
        try {
            val refreshedArtist = if (artist?.updatedTimestamp == null || artist.updatedTimestamp.time.isOlderThan(1.days)) {
                emit(RefreshableResource.refreshing(artist))
                artistRepository.refreshArtist(artistId)
                artistRepository.loadArtist(artistId)
            } else artist
            refreshedArtist?.let {
                val artistWithImages = if (it.imagesUpdatedTimestamp == null || it.imagesUpdatedTimestamp.time.isOlderThan(1.days)) {
                    emit(RefreshableResource.refreshing(it))
                    artistRepository.refreshImages(it)
                } else it
                val completeArtist = if (artistWithImages.heroImageUrl.getImageId() != artistWithImages.thumbnailUrl.getImageId()) {
                    emit(RefreshableResource.refreshing(artistWithImages))
                    artistRepository.refreshHeroImage(artistWithImages)
                } else artistWithImages
                emit(RefreshableResource.success(artistRepository.loadArtist(artistId) ?: completeArtist))
            } ?: emit(RefreshableResource.success(null))
        } catch (e: Exception) {
            emit(RefreshableResource.error(e, application, artist))
        }
    }

    private suspend fun LiveDataScope<RefreshableResource<Person>?>.loadDesigner(designerId: Int, application: Application) {
        emit(RefreshableResource.refreshing(latestValue?.data))
        val designer = designerRepository.loadDesigner(designerId)
        try {
            val refreshedDesigner = if (designer?.updatedTimestamp == null || designer.updatedTimestamp.time.isOlderThan(1.days)) {
                emit(RefreshableResource.refreshing(designer))
                designerRepository.refreshDesigner(designerId)
                designerRepository.loadDesigner(designerId)
            } else designer
            refreshedDesigner?.let {
                val designerWithImages = if (it.imagesUpdatedTimestamp == null || it.imagesUpdatedTimestamp.time.isOlderThan(1.days)) {
                    emit(RefreshableResource.refreshing(it))
                    designerRepository.refreshImages(it)
                } else it
                val completeDesigner = if (designerWithImages.heroImageUrl.getImageId() != designerWithImages.thumbnailUrl.getImageId()) {
                    emit(RefreshableResource.refreshing(designerWithImages))
                    designerRepository.refreshHeroImage(designerWithImages)
                } else designerWithImages
                emit(RefreshableResource.success(designerRepository.loadDesigner(designerId) ?: completeDesigner))
            } ?: emit(RefreshableResource.success(null))
        } catch (e: Exception) {
            emit(RefreshableResource.error(e, application, designer))
        }
    }

    private suspend fun LiveDataScope<RefreshableResource<Person>?>.loadPublisher(publisherId: Int, application: Application) {
        emit(RefreshableResource.refreshing(latestValue?.data))
        val publisher = publisherRepository.loadPublisher(publisherId)
        try {
            val refreshedPublisher = if (publisher?.updatedTimestamp == null || publisher.updatedTimestamp.time.isOlderThan(0.days)) {
                emit(RefreshableResource.refreshing(publisher.toPerson()))
                publisherRepository.refreshPublisher(publisherId)
                publisherRepository.loadPublisher(publisherId)
            } else publisher
            val completePublisher = refreshedPublisher?.let {
                emit(RefreshableResource.refreshing(it.toPerson()))
                if (it.heroImageUrl.getImageId() != it.thumbnailUrl.getImageId()) {
                    publisherRepository.refreshImages(it)
                } else refreshedPublisher
            }
            emit(RefreshableResource.success((publisherRepository.loadPublisher(publisherId) ?: completePublisher).toPerson()))
        } catch (e: Exception) {
            emit(RefreshableResource.error(e, application, publisher.toPerson()))
        }
    }

    private fun Company?.toPerson() = this?.let {
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

    val type: LiveData<PersonType> = _personInfo.map {
        it.type
    }

    val id: LiveData<Int> = _personInfo.map {
        it.id
    }

    val collectionSort: LiveData<CollectionSort> = _personInfo.map {
        it.sort
    }

    val collection = _personInfo.switchMap { person ->
        liveData {
            emit(
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
