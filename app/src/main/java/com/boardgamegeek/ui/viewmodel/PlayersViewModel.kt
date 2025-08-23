package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.extensions.firstChar
import com.boardgamegeek.extensions.orderOfMagnitude
import com.boardgamegeek.model.Player
import com.boardgamegeek.repository.PlayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

@HiltViewModel
class PlayersViewModel @Inject constructor(
    application: Application,
    private val playRepository: PlayRepository,
) : AndroidViewModel(application) {
    val players = MediatorLiveData<List<Player>>()

    private val _sortType = MutableLiveData<Player.SortType>()
    val sortType: LiveData<Player.SortType>
        get() = _sortType

    private val _filter = MutableLiveData<String>()
    val filter: LiveData<String>
        get() = _filter

    private val _allPlayers: LiveData<List<Player>> = _sortType.switchMap {
        liveData {
            emitSource(
                playRepository.loadPlayersFlow(it)
                    .distinctUntilChanged()
                    .asLiveData()
            )
        }
    }

    init {
        players.addSource(_allPlayers) { result ->
            result?.let {
                players.value = assembleAvailablePlayers(allPlayers = result)
            }
        }
        players.addSource(_filter) { result ->
            result?.let {
                players.value = assembleAvailablePlayers(filter = result)
            }
        }

        sort(Player.SortType.NAME)
        filter("")
    }

    fun sort(sortType: Player.SortType) {
        if (_sortType.value != sortType) _sortType.value = sortType
    }

    fun filter(filter: String) {
        if (_filter.value != filter) _filter.value = filter
    }

    fun getSectionHeader(player: Player?): String {
        return when (_sortType.value) {
            Player.SortType.NAME -> player?.name.firstChar()
            Player.SortType.PLAY_COUNT -> (player?.playCount ?: 0).orderOfMagnitude()
            Player.SortType.WIN_COUNT -> (player?.winCount ?: 0).orderOfMagnitude()
            else -> ""
        }
    }

    private fun assembleAvailablePlayers(
        allPlayers: List<Player>? = _allPlayers.value,
        filter: String? = _filter.value,
    ): List<Player> {
        return filter?.let { filterText ->
            allPlayers?.filter {
                it.name.contains(filterText, true) ||
                        it.username.contains(filterText, true) ||
                        (it.userFullName?.contains(filterText, true) == true)
            }
        } ?: emptyList()
    }
}
