package com.boardgamegeek.tasks

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.Observer
import android.os.AsyncTask
import com.boardgamegeek.BggApplication
import com.boardgamegeek.entities.PlayStatsEntity
import com.boardgamegeek.entities.PlayerStatsEntity
import com.boardgamegeek.io.BggService
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.util.PreferencesUtils

class CalculatePlayStatsTask(private val application: BggApplication) : AsyncTask<Void, Void, Void?>(), LifecycleOwner {
    private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
    private val playRepository: PlayRepository = PlayRepository(application)

    init {
        lifecycleRegistry.markState(State.CREATED)
    }

    override fun doInBackground(vararg params: Void): Void? {
        lifecycleRegistry.markState(State.STARTED)
        if (SyncPrefs.isPlaysSyncUpToDate(application)) {
            playRepository.loadForStats().observe(this, Observer {
                if (it != null) {
                    val entity = PlayStatsEntity(it, PreferencesUtils.isStatusSetToSync(application, BggService.COLLECTION_QUERY_STATUS_OWN))
                    playRepository.updateGameHIndex(entity.hIndex)
                }
            })

            playRepository.loadPlayersForStats().observe(this, Observer {
                if (it != null) {
                    val entity = PlayerStatsEntity(it)
                    playRepository.updatePlayerHIndex(entity.hIndex)
                }
            })
        }
        return null
    }

    override fun onPostExecute(nothing: Void?) {
        lifecycleRegistry.markState(State.DESTROYED)
    }

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }
}
