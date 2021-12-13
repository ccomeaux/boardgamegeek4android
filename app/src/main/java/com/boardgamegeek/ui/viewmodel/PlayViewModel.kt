package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.entities.PlayEntity
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.service.SyncService
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class PlayViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PlayRepository(getApplication())
    private val arePlaysRefreshing = AtomicBoolean()
    private val forceRefresh = AtomicBoolean()

    private val internalId = MutableLiveData<Long>()
    private val _updatedId = MutableLiveData<Long>()
    val updatedId: LiveData<Long>
        get() = _updatedId

    val play: LiveData<RefreshableResource<PlayEntity>> = internalId.switchMap { id ->
        liveData {
            try {
                latestValue?.data?.let { emit(RefreshableResource.refreshing(it)) }
                val play = repository.loadPlay(id)
                emit(RefreshableResource.success(play))
                if (arePlaysRefreshing.compareAndSet(false, true)) {
                    play?.let {
                        val canRefresh = it.playId != BggContract.INVALID_ID && it.gameId != BggContract.INVALID_ID
                        val shouldRefresh = it.syncTimestamp.isOlderThan(2, TimeUnit.HOURS)
                        if (canRefresh && (shouldRefresh || forceRefresh.compareAndSet(true, false))) {
                            emit(RefreshableResource.refreshing(it))
                            repository.refreshPlay(id, it.playId, it.gameId)
                            val refreshedPlay = repository.loadPlay(id)
                            emit(RefreshableResource.success(refreshedPlay))
                        }
                    }
                }
            } catch (e: Exception) {
                emit(RefreshableResource.error(e, application))
            } finally {
                arePlaysRefreshing.set(false)
            }
        }
    }

    fun setId(id: Long) {
        if (internalId.value != id) internalId.value = id
    }

    fun refresh() {
        play.value?.data?.let {
            if (it.updateTimestamp > 0 || it.deleteTimestamp > 0)
                SyncService.sync(getApplication(), SyncService.FLAG_SYNC_PLAYS_UPLOAD)
        }
        forceRefresh.set(true)
        reload()
    }

    fun reload() {
        internalId.value = internalId.value
    }

    fun discard() {
        viewModelScope.launch {
            play.value?.data?.let {
                repository.markAsDiscarded(it.internalId)
                _updatedId.postValue(it.internalId)
            }
            refresh() // pull down the data from BGG
        }
    }

    fun send() {
        viewModelScope.launch {
            play.value?.data?.let {
                repository.markAsUpdated(it.internalId)
                _updatedId.postValue(it.internalId)
            }
            SyncService.sync(getApplication(), SyncService.FLAG_SYNC_PLAYS_UPLOAD)
        }
    }

    fun delete() {
        viewModelScope.launch {
            play.value?.data?.let {
                repository.markAsDeleted(it.internalId)
                _updatedId.postValue(it.internalId)
            }
            SyncService.sync(getApplication(), SyncService.FLAG_SYNC_PLAYS_UPLOAD)
        }
    }
}
