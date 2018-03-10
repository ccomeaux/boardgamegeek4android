package com.boardgamegeek.ui.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.GameRepository
import com.boardgamegeek.ui.model.Game

class GameDescriptionViewModel(application: Application) : AndroidViewModel(application) {
    private var game: LiveData<Game>? = null
    private var gameId = BggContract.INVALID_ID

    fun init(gameId: Int) {
        this.gameId = gameId
        if (game == null && gameId != BggContract.INVALID_ID) game = GameRepository(getApplication()).getGame(gameId)
    }

    fun getGame(): LiveData<Game> {
        return game ?: MutableLiveData()
    }
}
