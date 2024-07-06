package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.model.Player
import com.boardgamegeek.extensions.firstChar
import com.boardgamegeek.extensions.orderOfMagnitude
import com.boardgamegeek.repository.PlayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PlayersViewModel @Inject constructor(
    application: Application,
    private val playRepository: PlayRepository,
) : AndroidViewModel(application) {
    private val _sortType = MutableLiveData<Player.SortType>()
    val sortType: LiveData<Player.SortType>
        get() = _sortType

    init {
        sort(Player.SortType.NAME)
    }

    val players: LiveData<List<Player>> = sortType.switchMap {
        liveData {
            emit(playRepository.loadPlayers(it))
        }
    }

    fun sort(sortType: Player.SortType) {
        if (_sortType.value != sortType) _sortType.value = sortType
    }

    fun getSectionHeader(player: Player?): String {
        return when (_sortType.value) {
            Player.SortType.NAME -> player?.name.firstChar()
            Player.SortType.PLAY_COUNT -> (player?.playCount ?: 0).orderOfMagnitude()
            Player.SortType.WIN_COUNT -> (player?.winCount ?: 0).orderOfMagnitude()
            else -> ""
        }
    }

    fun getDisplayText(player: Player?): String {
        val context = getApplication<BggApplication>()
        return when (_sortType.value) {
            Player.SortType.WIN_COUNT -> {
                val winCount = player?.winCount ?: 0
                context.resources.getQuantityString(R.plurals.wins_suffix, winCount, winCount)
            }
            else -> {
                val playCount = player?.playCount ?: 0
                context.resources.getQuantityString(R.plurals.plays_suffix, playCount, playCount)
            }
        }
    }
}
