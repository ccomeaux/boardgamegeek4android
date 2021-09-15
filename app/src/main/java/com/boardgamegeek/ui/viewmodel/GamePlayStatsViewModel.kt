package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.boardgamegeek.entities.*
import com.boardgamegeek.livedata.AbsentLiveData
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.GameCollectionRepository
import com.boardgamegeek.repository.PlayRepository

class GamePlayStatsViewModel(application: Application) : AndroidViewModel(application) {
    private val gameRepository = GameCollectionRepository(getApplication())
    private val playRepository = PlayRepository(getApplication())

    private val _gameId = MutableLiveData<Int>()
    fun setGameId(gameId: Int?) {
        if (_gameId.value != gameId) _gameId.value = gameId
    }

    val game: LiveData<RefreshableResource<List<CollectionItemEntity>>> = Transformations.switchMap(_gameId) { gameId ->
        when (gameId) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> gameRepository.getCollectionItems(gameId)
        }
    }

    val plays: LiveData<RefreshableResource<List<PlayEntity>>> = Transformations.switchMap(_gameId) { gameId ->
        when (gameId) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> playRepository.loadPlaysByGame(gameId) // TODO sort by  ASC
        }
    }

    val players: LiveData<List<PlayPlayerEntity>> = Transformations.switchMap(_gameId) { gameId ->
        when (gameId) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> playRepository.loadPlayersByGame(gameId)
        }
    }
}
