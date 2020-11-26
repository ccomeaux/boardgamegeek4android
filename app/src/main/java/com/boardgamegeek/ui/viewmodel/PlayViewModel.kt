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
    private val _insertedId = MutableLiveData<Long>()

    val play: LiveData<RefreshableResource<PlayEntity>> = Transformations.switchMap(internalId) { id ->
        when (id) {
            null -> AbsentLiveData.create()
            else -> repository.getPlay(id)
        }
    }

    fun setId(id: Long) {
        if (internalId.value != id) internalId.value = id
    }

    fun discard() {
        play.value?.data?.let {
            val p = it.copy(
                    dirtyTimestamp = 0,
                    updateTimestamp = 0,
                    deleteTimestamp = 0,
            )
            it.players.forEach { player ->
                p.addPlayer(player)
            }
            repository.save(p, _insertedId)
            // TODO move this to fragment when insertedId changes?
            SyncPlaysByGameTask(getApplication() as BggApplication, p.gameId).executeAsyncTask()
        }
    }

    fun send() {
        play.value?.data?.let {
            val p = it.copy(
                    updateTimestamp = System.currentTimeMillis(),
            )
            it.players.forEach { player ->
                p.addPlayer(player)
            }
            repository.save(p, _insertedId)
            // TODO move this to fragment when insertedId changes?
            SyncService.sync(getApplication(), SyncService.FLAG_SYNC_PLAYS_UPLOAD)
        }
    }

    fun delete() {
        play.value?.data?.let {
            val p = it.copy(
                    length = 0,
                    startTime = 0,
                    deleteTimestamp = System.currentTimeMillis(),
            )
            it.players.forEach { player ->
                p.addPlayer(player)
            }
            repository.save(p, _insertedId)
            // TODO move this to fragment when insertedId changes?
            SyncService.sync(getApplication(), SyncService.FLAG_SYNC_PLAYS_UPLOAD)
        }
    }
}