package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.entities.PlayEntity
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.PlayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours

@HiltViewModel
class PlayViewModel @Inject constructor(
    application: Application,
    private val repository: PlayRepository,
) : AndroidViewModel(application) {
    private val arePlaysRefreshing = AtomicBoolean()
    private val forceRefresh = AtomicBoolean()
    private val internalId = MutableLiveData<Long>()

    val play: LiveData<RefreshableResource<PlayEntity>> = internalId.switchMap { id ->
        liveData {
            try {
                latestValue?.data?.let { emit(RefreshableResource.refreshing(it)) }
                val play = repository.loadPlay(id)
                emit(RefreshableResource.success(play))
                if (arePlaysRefreshing.compareAndSet(false, true)) {
                    play?.let {
                        val canRefresh = it.playId != BggContract.INVALID_ID && it.gameId != BggContract.INVALID_ID
                        val shouldRefresh = it.syncTimestamp.isOlderThan(2.hours)
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
        viewModelScope.launch {
            play.value?.data?.let {
                repository.uploadPlay(it)
            }
            forceRefresh.set(true)
            reload()
        }
    }

    fun reload() {
        internalId.value = internalId.value
    }

    fun discard() {
        viewModelScope.launch {
            play.value?.data?.let {
                repository.markAsDiscarded(it.internalId)
            }
            refresh() // pull down the data from BGG
        }
    }

    fun send() {
        viewModelScope.launch {
            play.value?.data?.let {
                repository.markAsUpdated(it.internalId)?.let { play ->
                    repository.enqueueUploadRequest(play.internalId)
                }
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            play.value?.data?.let {
                repository.markAsDeleted(it.internalId)?.let { play ->
                    repository.enqueueUploadRequest(play.internalId)
                }
            }
        }
    }
}
