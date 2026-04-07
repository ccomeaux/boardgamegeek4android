package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.extensions.PREFERENCES_KEY_SYNC_STATUSES
import com.boardgamegeek.extensions.addSyncStatus
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.livedata.LiveSharedPreference
import com.boardgamegeek.mappers.mapToEnum
import com.boardgamegeek.mappers.mapToPerson
import com.boardgamegeek.model.*
import com.boardgamegeek.model.Person.Type
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.ArtistRepository
import com.boardgamegeek.repository.DesignerRepository
import com.boardgamegeek.repository.PublisherRepository
import com.boardgamegeek.work.SyncCollectionWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

@HiltViewModel
class PersonViewModel @Inject constructor(
    application: Application,
    private val artistRepository: ArtistRepository,
    private val designerRepository: DesignerRepository,
    private val publisherRepository: PublisherRepository,
) : AndroidViewModel(application) {
    private data class PersonInfo(
        val type: Type,
        val id: Int,
        val sort: CollectionItem.SortType = CollectionItem.SortType.RATING
    )

    private val _syncCollectionPreference: LiveData<Set<String>?> =
        LiveSharedPreference(getApplication(), PREFERENCES_KEY_SYNC_STATUSES, defaultValue = null)
    val syncCollectionPreference: LiveData<Set<CollectionStatus>?> = _syncCollectionPreference.map { set ->
        set.orEmpty().map { it.mapToEnum() }.toSet()
    }

    private val _personInfo = MutableLiveData<PersonInfo>()

    fun setArtistId(artistId: Int) {
        if (_personInfo.value?.type != Type.Artist || _personInfo.value?.id != artistId) _personInfo.value = PersonInfo(Type.Artist, artistId)
    }

    fun setDesignerId(designerId: Int) {
        if (_personInfo.value?.type != Type.Designer || _personInfo.value?.id != designerId) _personInfo.value = PersonInfo(Type.Designer, designerId)
    }

    fun setPublisherId(publisherId: Int) {
        if (_personInfo.value?.type != Type.Publisher || _personInfo.value?.id != publisherId) _personInfo.value =
            PersonInfo(Type.Publisher, publisherId)
    }

    fun sort(sortType: CollectionItem.SortType) {
        if (_personInfo.value?.sort != sortType) {
            _personInfo.value = PersonInfo(
                _personInfo.value?.type ?: Type.Designer,
                _personInfo.value?.id ?: BggContract.INVALID_ID,
                sortType
            )
        }
    }

    fun refresh() {
        _personInfo.value?.let { info ->
            viewModelScope.launch {
                when (info.type) {
                    Type.Artist -> artistRepository.refreshArtist(info.id)
                    Type.Designer -> designerRepository.refreshDesigner(info.id)
                    Type.Publisher -> publisherRepository.refreshPublisher(info.id)
                }
            }
        }
    }

    private var automaticRefreshTimestamp = 0L
    fun refreshIfStale() {
        if (details.value?.status == Status.SUCCESS && automaticRefreshTimestamp.isOlderThan(3.hours)) {
            details.value?.data?.let { person ->
                automaticRefreshTimestamp = System.currentTimeMillis()
                if (person.updatedTimestamp?.time.isOlderThan(1.days) ||
                    person.imagesUpdatedTimestamp?.time.isOlderThan(1.days)
                ) {
                    refresh()
                }
            }
        }
    }

    fun enableCollectionStatuses() {
        val prefs = getApplication<Application>().preferences()
        prefs.addSyncStatus(CollectionStatus.Played)
        prefs.addSyncStatus(CollectionStatus.Rated)
        SyncCollectionWorker.requestSync(getApplication()) // TODO ensure the newly added status are completely synced
    }

    val details = _personInfo.switchMap { person ->
        liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
            when (person.id) {
                BggContract.INVALID_ID -> emit(RefreshableResource.success(null))
                else -> {
                    when (person.type) {
                        Type.Artist -> loadArtist(person.id, application)
                        Type.Designer -> loadDesigner(person.id, application)
                        Type.Publisher -> loadPublisher(person.id, application)
                    }
                }
            }
        }.distinctUntilChanged()
    }

    private suspend fun LiveDataScope<RefreshableResource<Person>>.loadArtist(artistId: Int, application: Application) {
        try {
            emit(RefreshableResource.refreshing(latestValue?.data))
            emitSource(artistRepository.loadArtistFlow(artistId).distinctUntilChanged().asLiveData().map { RefreshableResource.success(it) })
        } catch (e: Exception) {
            emit(RefreshableResource.error(e, application, latestValue?.data))
        }
    }

    private suspend fun LiveDataScope<RefreshableResource<Person>>.loadDesigner(designerId: Int, application: Application) {
        try {
            emit(RefreshableResource.refreshing(latestValue?.data))
            emitSource(designerRepository.loadDesignerFlow(designerId).distinctUntilChanged().asLiveData().map { RefreshableResource.success(it) })
        } catch (e: Exception) {
            emit(RefreshableResource.error(e, application, latestValue?.data))
        }
    }

    private suspend fun LiveDataScope<RefreshableResource<Person>>.loadPublisher(publisherId: Int, application: Application) {
        try {
            emit(RefreshableResource.refreshing(latestValue?.data))
            emitSource(publisherRepository.loadPublisherFlow(publisherId).distinctUntilChanged().asLiveData().map { RefreshableResource.success(it?.mapToPerson()) })
        } catch (e: Exception) {
            emit(RefreshableResource.error(e, application, latestValue?.data))
        }
    }

    val type: LiveData<Type> = _personInfo.map { it.type }

    val id: LiveData<Int> = _personInfo.map { it.id }

    val collectionSort: LiveData<CollectionItem.SortType> = _personInfo.map { it.sort }

    val collection = _personInfo.switchMap { person ->
        liveData {
            emitSource(
                when (person.type) {
                    Type.Artist -> artistRepository.loadCollectionFlow(person.id, person.sort)
                    Type.Designer -> designerRepository.loadCollectionFlow(person.id, person.sort)
                    Type.Publisher -> publisherRepository.loadCollectionFlow(person.id, person.sort)
                }.asLiveData()
            )
        }
    }

    val stats = _personInfo.switchMap { person ->
        liveData {
            emit(
                when (person.type) {
                    Type.Artist -> artistRepository.calculateStats(person.id)
                    Type.Designer -> designerRepository.calculateStats(person.id)
                    Type.Publisher -> publisherRepository.calculateStats(person.id)
                }
            )
        }
    }
}
