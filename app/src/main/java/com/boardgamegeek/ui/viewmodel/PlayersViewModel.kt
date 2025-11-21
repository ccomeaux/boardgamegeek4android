package com.boardgamegeek.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.boardgamegeek.R
import com.boardgamegeek.db.PlayDao
import com.boardgamegeek.entities.PlayerEntity
import com.boardgamegeek.extensions.firstChar
import com.boardgamegeek.extensions.orderOfMagnitude
import com.boardgamegeek.repository.PlayRepository

class PlayersViewModel(application: Application) : AndroidViewModel(application) {
    enum class SortType {
        NAME, PLAY_COUNT, WIN_COUNT
    }

    private val playRepository = PlayRepository(getApplication())

    private val _sort = MutableLiveData<PlayersSort>()
    val sort: LiveData<PlayersSort>
        get() = _sort

    init {
        sort(SortType.NAME)
    }

    val players: LiveData<List<PlayerEntity>> = sort.switchMap() {
        playRepository.loadPlayers(it.sortBy)
    }

    fun sort(sortType: PlayersViewModel.SortType) {
        _sort.value = when (sortType) {
            SortType.NAME -> PlayersSortByName()
            SortType.PLAY_COUNT -> PlayersSortByPlayCount()
            SortType.WIN_COUNT -> PlayersSortByWinCount()
        }
    }

    fun getSectionHeader(player: PlayerEntity?): String {
        return sort.value?.getSectionHeader(player) ?: ""
    }

    fun getDisplayText(player: PlayerEntity?): String {
        return sort.value?.getDisplayText(getApplication(), player) ?: ""
    }
}

sealed class PlayersSort {
    abstract val sortType: PlayersViewModel.SortType
    abstract val sortBy: PlayDao.PlayerSortBy
    abstract fun getSectionHeader(player: PlayerEntity?): String
    open fun getDisplayText(context: Context, player: PlayerEntity?): String {
        val playCount = player?.playCount ?: 0
        return context.resources.getQuantityString(R.plurals.plays_suffix, playCount, playCount)
    }
}

class PlayersSortByName : PlayersSort() {
    override val sortType = PlayersViewModel.SortType.NAME
    override val sortBy = PlayDao.PlayerSortBy.NAME
    override fun getSectionHeader(player: PlayerEntity?): String {
        return player?.name.firstChar()
    }
}

class PlayersSortByPlayCount : PlayersSort() {
    override val sortType = PlayersViewModel.SortType.PLAY_COUNT
    override val sortBy = PlayDao.PlayerSortBy.PLAY_COUNT
    override fun getSectionHeader(player: PlayerEntity?): String {
        return (player?.playCount ?: 0).orderOfMagnitude()
    }
}

class PlayersSortByWinCount : PlayersSort() {
    override val sortType = PlayersViewModel.SortType.WIN_COUNT
    override val sortBy = PlayDao.PlayerSortBy.WIN_COUNT
    override fun getSectionHeader(player: PlayerEntity?): String {
        return (player?.winCount ?: 0).orderOfMagnitude()
    }

    override fun getDisplayText(context: Context, player: PlayerEntity?): String {
        val winCount = player?.winCount ?: 0
        return context.resources.getQuantityString(R.plurals.wins_suffix, winCount, winCount)
    }
}
