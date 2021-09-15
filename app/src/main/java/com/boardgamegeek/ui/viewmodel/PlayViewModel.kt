package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.boardgamegeek.BggApplication
import com.boardgamegeek.entities.PlayEntity
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.extensions.executeAsyncTask
import com.boardgamegeek.livedata.AbsentLiveData
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.service.SyncService
import com.boardgamegeek.tasks.sync.SyncPlaysByGameTask

class PlayViewModel(application: Application) : AndroidViewModel(application) {
    val repository = PlayRepository(getApplication())

    private val internalId = MutableLiveData<Long>()
    private val _updatedId = MutableLiveData<Long>()
    val updatedId: LiveData<Long>
        get() = _updatedId

    val play: LiveData<RefreshableResource<PlayEntity>> = Transformations.switchMap(internalId) { id ->
        when (id) {
            null -> AbsentLiveData.create()
            else -> repository.getPlay(id)
        }
    }

    fun setId(id: Long) {
        if (internalId.value != id) internalId.value = id
    }

    fun refresh() {
        internalId.value = internalId.value
    }

    fun discard() {
        play.value?.data?.let {
            repository.markAsDiscarded(it.internalId, _updatedId)
        }
    }

    fun send() {
        play.value?.data?.let {
            repository.markAsUpdated(it.internalId, _updatedId)
        }
    }

    fun delete() {
        play.value?.data?.let {
            repository.markAsDeleted(it.internalId, _updatedId)
        }
    }
}