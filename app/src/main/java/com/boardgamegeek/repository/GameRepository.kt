package com.boardgamegeek.repository

import android.arch.lifecycle.LiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.livedata.GameLoader
import com.boardgamegeek.tasks.sync.SyncGameTask
import com.boardgamegeek.ui.model.Game
import com.boardgamegeek.ui.model.RefreshableResource
import com.boardgamegeek.util.TaskUtils

class GameRepository(val application: BggApplication) {
    fun getGame(gameId: Int): LiveData<RefreshableResource<Game>> {
        return GameLoader(application, gameId).load()
    }

    fun refreshGame(gameId: Int) {
        TaskUtils.executeAsyncTask(SyncGameTask(application, gameId))
    }
}
