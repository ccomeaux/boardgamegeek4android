package com.boardgamegeek.repository

import com.boardgamegeek.BggApplication
import com.boardgamegeek.livedata.GameLiveData

class GameRepository(val application: BggApplication) {
    fun getGame(gameId: Int): GameLiveData {
        return GameLiveData(application, gameId)
    }
}
