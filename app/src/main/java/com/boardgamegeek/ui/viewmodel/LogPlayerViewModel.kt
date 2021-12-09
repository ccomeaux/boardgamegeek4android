package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.db.PlayDao
import com.boardgamegeek.db.UserDao
import com.boardgamegeek.entities.PlayerEntity
import com.boardgamegeek.entities.UserEntity
import com.boardgamegeek.repository.GameRepository
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.repository.UserRepository

class LogPlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val gameRepository = GameRepository(getApplication())
    private val playRepository = PlayRepository(getApplication())
    private val userRepository = UserRepository(getApplication())

    private val _gameId = MutableLiveData<Int>()

    fun setGameId(gameId: Int) {
        if (_gameId.value != gameId) _gameId.value = gameId
    }

    val players: LiveData<List<PlayerEntity>> = _gameId.switchMap {
        liveData {
            emit(playRepository.loadPlayers(PlayDao.PlayerSortBy.PLAY_COUNT))
        }
    }.distinctUntilChanged()

    val buddies: LiveData<List<UserEntity>> = _gameId.switchMap {
        liveData {
            emit(userRepository.loadBuddies(UserDao.UsersSortBy.USERNAME))
        }
    }.distinctUntilChanged()

    val colors: LiveData<List<String>> = _gameId.switchMap {
        liveData {
            emit(gameRepository.getPlayColors(it))
        }
    }.distinctUntilChanged()
}
