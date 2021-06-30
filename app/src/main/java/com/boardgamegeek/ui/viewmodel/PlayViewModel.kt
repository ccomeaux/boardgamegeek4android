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
                val play = repository.loadPlay(id)
                val refreshedPlay = play?.let {
                    val shouldRefresh =
                        it.playId != BggContract.INVALID_ID &&
                                it.gameId != BggContract.INVALID_ID &&
                                it.syncTimestamp.isOlderThan(2, TimeUnit.HOURS)
                    if (arePlaysRefreshing.compareAndSet(false, true)) {
                        when {
                            shouldRefresh || forceRefresh.compareAndSet(true, false) -> {
                                emit(RefreshableResource.refreshing(it))
                                repository.refreshPlay(id, it.playId, it.gameId)
                            }
                            else -> it
                        }.also { arePlaysRefreshing.set(false) }
                    } else it
                }
                emit(RefreshableResource.success(refreshedPlay))
            } catch (e: Exception) {
                forceRefresh.set(false)
                arePlaysRefreshing.set(false)
                emit(RefreshableResource.error<PlayEntity>(e, application))
            }
        }
    }

    fun setId(id: Long) {
        if (internalId.value != id) internalId.value = id
    }

    fun refresh() {
        forceRefresh.set(true)
        SyncService.sync(getApplication(), SyncService.FLAG_SYNC_PLAYS_UPLOAD)
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
