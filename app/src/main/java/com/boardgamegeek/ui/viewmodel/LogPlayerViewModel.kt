package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.repository.GameRepository

class LogPlayerViewModel(application: Application) : AndroidViewModel(application) {
    val repository = GameRepository(getApplication())

    private val _gameId = MutableLiveData<Int>()

    fun setGameId(gameId: Int) {
        if (_gameId.value != gameId) _gameId.value = gameId
    }

    val colors: LiveData<List<String>> = _gameId.switchMap {
        liveData {
            repository.getColors(0)
        }
    }
}
