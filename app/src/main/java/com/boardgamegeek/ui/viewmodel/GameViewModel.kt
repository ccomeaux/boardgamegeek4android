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
    private var gameId = BggContract.INVALID_ID
    private var game: LiveData<RefreshableResource<Game>> = MutableLiveData<RefreshableResource<Game>>()

    fun getGame(gameId: Int): LiveData<RefreshableResource<Game>> {
        if (gameId != this.gameId) {
            game = gameRepository.getGame(gameId)
        }
        return game
    }

    fun refresh() {
        gameRepository.refreshGame()
    }

    fun updateLastViewed(lastViewed: Long = System.currentTimeMillis()) {
        gameRepository.updateLastViewed(lastViewed)
    }
}
