package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.GameRepository

class GameColorsViewModel(application: Application) : AndroidViewModel(application) {
    private val gameRepository = GameRepository(getApplication())
    private val _gameId = MutableLiveData<Int>()

    fun setGameId(gameId: Int) {
        if (_gameId.value != gameId) _gameId.value = gameId
    }

    val colors = _gameId.switchMap { gameId ->
        liveData {
            emit(if (gameId == BggContract.INVALID_ID) null else gameRepository.getPlayColors(gameId))
        }
    }

    fun addColor(color: String?) {
        if (color.isNullOrBlank()) return
        gameRepository.addPlayColor(_gameId.value ?: BggContract.INVALID_ID, color)
    }

    fun removeColor(color: String): Int {
        if (color.isBlank()) return 0
        return gameRepository.deletePlayColor(_gameId.value ?: BggContract.INVALID_ID, color)
    }

    fun computeColors() {
        gameRepository.computePlayColors(_gameId.value ?: BggContract.INVALID_ID)
    }
}
