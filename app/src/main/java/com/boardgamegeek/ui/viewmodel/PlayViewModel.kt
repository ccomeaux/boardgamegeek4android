package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import androidx.work.WorkManager
import com.boardgamegeek.model.Play
import com.boardgamegeek.model.RefreshableResource
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

    val play: LiveData<RefreshableResource<Play>> = internalId.switchMap { id ->
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
        forceRefresh.set(true)
        reload()
    }

    fun reload() {
        internalId.value = internalId.value
    }

    private var wasRefreshing = false
    val isRefreshing = WorkManager.getInstance(getApplication()).getWorkInfosByTagLiveData(workTag).map { list ->
        if (wasRefreshing && list.all { it.state.isFinished }) {
            wasRefreshing = false
            reload()
        }
        list.any { workInfo -> !workInfo.state.isFinished }
    }

    fun discard() {
        viewModelScope.launch {
            play.value?.data?.let {
                if (repository.markAsDiscarded(it.internalId))
                    refresh()
            }
        }
    }

    fun send() {
        viewModelScope.launch {
            play.value?.data?.let {
                if (repository.markAsUpdated(it.internalId)) {
                    wasRefreshing = true
                    repository.enqueueUploadRequest(it.internalId, workTag)
                }
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            play.value?.data?.let {
                if (repository.markAsDeleted(it.internalId)) {
                    wasRefreshing = true
                    repository.enqueueUploadRequest(it.internalId, workTag)
                }
            }
        }
    }

    companion object {
        const val workTag = "PlayViewModel"
    }
}
