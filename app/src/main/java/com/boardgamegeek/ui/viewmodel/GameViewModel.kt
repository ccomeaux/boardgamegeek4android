package com.boardgamegeek.ui.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.GameRepository
import com.boardgamegeek.ui.model.Game
import com.boardgamegeek.ui.model.RefreshableResource

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val gameRepository = GameRepository(getApplication())
    private var game: LiveData<RefreshableResource<Game>>? = null
    private var gameId = BggContract.INVALID_ID

    fun init(gameId: Int) {
        this.gameId = gameId
        if (game == null && gameId != BggContract.INVALID_ID) {
            game = gameRepository.getGame(gameId)
        }
    }

    fun refresh() {
        if (gameId != BggContract.INVALID_ID)
            gameRepository.refreshGame(gameId)
    }

    fun getGame(): LiveData<RefreshableResource<Game>> {
        return game ?: MutableLiveData()
    }
}
