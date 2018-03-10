package com.boardgamegeek.repository

import com.boardgamegeek.BggApplication
import com.boardgamegeek.livedata.GameDescriptionLiveData

class GameRepository(val application: BggApplication) {
    fun getGameDescription(gameId: Int): GameDescriptionLiveData {
        return GameDescriptionLiveData(application, gameId)
    }
}
