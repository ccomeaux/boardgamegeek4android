package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.model.Player
import com.boardgamegeek.model.User
import com.boardgamegeek.repository.GameRepository
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LogPlayerViewModel @Inject constructor(
    application: Application,
    private val gameRepository: GameRepository,
    private val playRepository: PlayRepository,
    private val userRepository: UserRepository,
) : AndroidViewModel(application) {
    private val _gameId = MutableLiveData<Int>()

    fun setGameId(gameId: Int) {
        if (_gameId.value != gameId) _gameId.value = gameId
    }

    val players: LiveData<List<Player>> = _gameId.switchMap {
        liveData {
            emit(playRepository.loadPlayers())
        }
    }.distinctUntilChanged()

    val buddies: LiveData<List<User>> = _gameId.switchMap {
        liveData {
            emit(userRepository.loadBuddies())
        }
    }.distinctUntilChanged()

    val colors: LiveData<List<String>> = _gameId.switchMap {
        liveData {
            emit(gameRepository.getPlayColors(it))
        }
    }.distinctUntilChanged()
}
