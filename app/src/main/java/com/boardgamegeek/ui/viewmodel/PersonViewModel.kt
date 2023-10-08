package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.db.CollectionDao
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

    private val _person = MutableLiveData<Person>()
    val person: LiveData<Person>
        get() = _person

    fun setArtistId(artistId: Int) {
        if (_person.value?.type != PersonType.ARTIST || _person.value?.id != artistId) _person.value = Person(PersonType.ARTIST, artistId)
    }

    fun setDesignerId(designerId: Int) {
        if (_person.value?.type != PersonType.DESIGNER || _person.value?.id != designerId) _person.value = Person(PersonType.DESIGNER, designerId)
    }

    fun setPublisherId(publisherId: Int) {
        if (_person.value?.type != PersonType.PUBLISHER || _person.value?.id != publisherId) _person.value = Person(PersonType.PUBLISHER, publisherId)
    }

    fun sort(sortType: CollectionSort) {
        if (_person.value?.sort != sortType) {
            _person.value = Person(
                _person.value?.type ?: PersonType.DESIGNER,
                _person.value?.id ?: BggContract.INVALID_ID,
                sortType
            )
        }
    }

    fun refresh() {
        _person.value?.let { _person.value = Person(it.type, it.id) }
    }

    val details = _person.switchMap { person ->
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

    private suspend fun LiveDataScope<RefreshableResource<com.boardgamegeek.model.Person>?>.loadArtist(artistId: Int, application: Application) {
        emit(RefreshableResource.refreshing(latestValue?.data))
        val artist = artistRepository.loadArtist(artistId)
        try {
            val refreshedArtist = if (artist == null || artist.updatedTimestamp.isOlderThan(1.days)) {
                emit(RefreshableResource.refreshing(artist))
                artistRepository.refreshArtist(artistId)
                artistRepository.loadArtist(artistId)
            } else artist
            refreshedArtist?.let {
                val artistWithImages = if (it.imagesUpdatedTimestamp.isOlderThan(1.days)) {
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

    private suspend fun LiveDataScope<RefreshableResource<com.boardgamegeek.model.Person>?>.loadDesigner(designerId: Int, application: Application) {
        emit(RefreshableResource.refreshing(latestValue?.data))
        val designer = designerRepository.loadDesigner(designerId)
        try {
            val refreshedDesigner = if (designer == null || designer.updatedTimestamp.isOlderThan(1.days)) {
                emit(RefreshableResource.refreshing(designer))
                designerRepository.refreshDesigner(designerId)
                designerRepository.loadDesigner(designerId)
            } else designer
            refreshedDesigner?.let {
                val designerWithImages = if (it.imagesUpdatedTimestamp.isOlderThan(1.days)) {
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

    private suspend fun LiveDataScope<RefreshableResource<com.boardgamegeek.model.Person>?>.loadPublisher(publisherId: Int, application: Application) {
        emit(RefreshableResource.refreshing(latestValue?.data))
        val publisher = publisherRepository.loadPublisher(publisherId)
        try {
            val refreshedPublisher = if (publisher == null || publisher.updatedTimestamp.isOlderThan(1.days)) {
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
            BggContract.INVALID_ID,
            it.id,
            it.name,
            it.description,
            it.updatedTimestamp,
            it.thumbnailUrl,
            it.heroImageUrl,
        )
    }

    val collectionSort: LiveData<CollectionSort> = _person.map {
        it.sort
    }

    val collection = _person.switchMap { person ->
        liveData {
            when (person.id) {
                BggContract.INVALID_ID -> emit(null)
                else -> {
                    val sortBy = when (person.sort) {
                        CollectionSort.NAME -> CollectionDao.SortType.NAME
                        CollectionSort.RATING -> CollectionDao.SortType.RATING
                    }
                    emit(
                        when (person.type) {
                            PersonType.ARTIST -> artistRepository.loadCollection(person.id, sortBy)
                            PersonType.DESIGNER -> designerRepository.loadCollection(person.id, sortBy)
                            PersonType.PUBLISHER -> publisherRepository.loadCollection(person.id, sortBy)
                        }
                    )
                }
            }
        }
    }

    val stats = _person.switchMap { person ->
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
