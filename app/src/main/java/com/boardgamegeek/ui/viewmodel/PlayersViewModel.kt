package com.boardgamegeek.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import com.boardgamegeek.R
import com.boardgamegeek.db.PlayDao
import com.boardgamegeek.entities.PlayerEntity
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
    enum class SortType {
        NAME, PLAY_COUNT, WIN_COUNT
    }

    private val _sort = MutableLiveData<PlayersSort>()
    val sort: LiveData<PlayersSort>
        get() = _sort

    init {
        sort(SortType.NAME)
    }

    val players: LiveData<List<PlayerEntity>> = sort.switchMap {
        liveData {
            emit(playRepository.loadPlayers(it.sortBy))
        }
    }

    fun sort(sortType: SortType) {
        _sort.value = when (sortType) {
            SortType.NAME -> PlayersSort.ByName()
            SortType.PLAY_COUNT -> PlayersSort.ByPlayCount()
            SortType.WIN_COUNT -> PlayersSort.ByWinCount()
        }
    }

    fun getSectionHeader(player: PlayerEntity?): String {
        return sort.value?.getSectionHeader(player) ?: ""
    }

    fun getDisplayText(player: PlayerEntity?): String {
        return sort.value?.getDisplayText(getApplication(), player) ?: ""
    }

    sealed class PlayersSort {
        abstract val sortType: SortType
        abstract val sortBy: PlayDao.PlayerSortBy
        abstract fun getSectionHeader(player: PlayerEntity?): String
        open fun getDisplayText(context: Context, player: PlayerEntity?): String {
            val playCount = player?.playCount ?: 0
            return context.resources.getQuantityString(R.plurals.plays_suffix, playCount, playCount)
        }

        class ByName : PlayersSort() {
            override val sortType = SortType.NAME
            override val sortBy = PlayDao.PlayerSortBy.NAME
            override fun getSectionHeader(player: PlayerEntity?): String {
                return player?.name.firstChar()
            }
        }

        class ByPlayCount : PlayersSort() {
            override val sortType = SortType.PLAY_COUNT
            override val sortBy = PlayDao.PlayerSortBy.PLAY_COUNT
            override fun getSectionHeader(player: PlayerEntity?): String {
                return (player?.playCount ?: 0).orderOfMagnitude()
            }
        }

        class ByWinCount : PlayersSort() {
            override val sortType = SortType.WIN_COUNT
            override val sortBy = PlayDao.PlayerSortBy.WIN_COUNT
            override fun getSectionHeader(player: PlayerEntity?): String {
                return (player?.winCount ?: 0).orderOfMagnitude()
            }

            override fun getDisplayText(context: Context, player: PlayerEntity?): String {
                val winCount = player?.winCount ?: 0
                return context.resources.getQuantityString(R.plurals.wins_suffix, winCount, winCount)
            }
        }
    }
}
