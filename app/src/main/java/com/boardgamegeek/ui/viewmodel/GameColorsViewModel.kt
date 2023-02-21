package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.GameRepository
import com.boardgamegeek.repository.PlayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameColorsViewModel @Inject constructor(
    application: Application,
    playRepository: PlayRepository,
) : AndroidViewModel(application) {
    private val gameRepository = GameRepository(getApplication(), playRepository)
    private val _gameId = MutableLiveData<Int>()

    fun setGameId(gameId: Int) {
        if (_gameId.value != gameId) _gameId.value = gameId
    }

    fun refresh() {
        _gameId.value?.let { _gameId.value = it }
    }

    val colors = _gameId.switchMap { gameId ->
        liveData {
            emit(if (gameId == BggContract.INVALID_ID) null else gameRepository.getPlayColors(gameId))
        }
    }

    fun addColor(color: String?) {
        viewModelScope.launch {
            gameRepository.addPlayColor(_gameId.value ?: BggContract.INVALID_ID, color)
            refresh()
        }
    }

    fun removeColor(color: String) {
        viewModelScope.launch {
            gameRepository.deletePlayColor(_gameId.value ?: BggContract.INVALID_ID, color)
            refresh()
        }
    }

    fun computeColors() {
        viewModelScope.launch {
            gameRepository.computePlayColors(_gameId.value ?: BggContract.INVALID_ID)
            refresh()
        }
    }
}
