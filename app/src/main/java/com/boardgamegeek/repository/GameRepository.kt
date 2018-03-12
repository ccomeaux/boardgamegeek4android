package com.boardgamegeek.repository

import com.boardgamegeek.BggApplication
import com.boardgamegeek.livedata.GameLiveData
import com.boardgamegeek.tasks.sync.SyncGameTask
import com.boardgamegeek.util.DateTimeUtils
import com.boardgamegeek.util.TaskUtils

class GameRepository(val application: BggApplication) {
    companion object {
        private const val AGE_IN_DAYS_TO_REFRESH = 7
    }

    fun getGame(gameId: Int): GameLiveData {
        val game = GameLiveData(application, gameId)
        if (game.value == null) {
            TaskUtils.executeAsyncTask(SyncGameTask(application, gameId))
        } else {
            game.value?.apply {
                if (DateTimeUtils.howManyDaysOld(updated) > AGE_IN_DAYS_TO_REFRESH || pollsVoteCount == 0)
                    TaskUtils.executeAsyncTask(SyncGameTask(application, gameId))
            }
        }
        return game
    }
}
