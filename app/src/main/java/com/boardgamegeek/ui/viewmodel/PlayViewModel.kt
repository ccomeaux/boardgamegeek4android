package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import androidx.work.WorkManager
import com.boardgamegeek.model.Play
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.livedata.Event
import com.boardgamegeek.livedata.EventLiveData
import com.boardgamegeek.repository.PlayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
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

    private val _isDownloading = MutableLiveData(false)
    private val _isUploading = WorkManager.getInstance(getApplication()).getWorkInfosByTagLiveData(workTag).map { list ->
        list.any { workInfo -> !workInfo.state.isFinished }
    }

    private val _isRefreshing = MediatorLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean>
        get() = _isRefreshing

    private val _errorMessage = EventLiveData()
    val errorMessage: LiveData<Event<String>>
        get() = _errorMessage

    init {
        _isRefreshing.addSource(_isDownloading) {
            _isRefreshing.value = it || (_isUploading.value ?: false)
        }
        _isRefreshing.addSource(_isUploading) {
            _isRefreshing.value = it || (_isDownloading.value ?: false)
        }
    }

    val play: LiveData<Play?> = internalId.switchMap { id ->
        liveData {
            emitSource(repository.loadPlayFlow(id).distinctUntilChanged().asLiveData().also {
                attemptRefresh()
            })
        }
    }

    fun setId(id: Long) {
        if (internalId.value != id) internalId.value = id
    }

    fun refresh() {
        forceRefresh.set(true)
        attemptRefresh()
    }

    private fun attemptRefresh() {
        viewModelScope.launch {
            if (arePlaysRefreshing.compareAndSet(false, true)) {
                play.value?.let { play ->
                    if (forceRefresh.compareAndSet(true, false) ||
                        play.syncTimestamp.isOlderThan(2.hours)) {
                        _isDownloading.value = true
                        repository.refreshPlay(play)?.let {
                            _errorMessage.postMessage(it)
                        }
                        _isDownloading.value = false
                    }
                }
                arePlaysRefreshing.set(false)
            }
        }
    }

    fun reload() {
        internalId.value = internalId.value
    }

    fun discard() {
        viewModelScope.launch {
            play.value?.let {
                if (repository.markAsDiscarded(it.internalId))
                    refresh()
            }
        }
    }

    fun send() {
        viewModelScope.launch {
            play.value?.let {
                if (repository.markAsUpdated(it.internalId)) {
                    repository.enqueueUploadRequest(it.internalId, workTag)
                }
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            play.value?.let {
                if (repository.markAsDeleted(it.internalId)) {
                    repository.enqueueUploadRequest(it.internalId, workTag)
                }
            }
        }
    }

    companion object {
        const val workTag = "PlayViewModel"
    }
}
