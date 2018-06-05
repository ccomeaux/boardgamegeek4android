package com.boardgamegeek.ui.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.support.v7.graphics.Palette
import com.boardgamegeek.entities.GamePlayerPollEntity
import com.boardgamegeek.entities.GamePollEntity
import com.boardgamegeek.entities.GameRankEntity
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.livedata.AbsentLiveData
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.GameCollectionRepository
import com.boardgamegeek.repository.GameRepository
import com.boardgamegeek.ui.model.Game
import com.boardgamegeek.ui.model.GameCollectionItem
import com.boardgamegeek.ui.model.PlaysByGame
import com.boardgamegeek.util.PaletteUtils

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val _gameId = MutableLiveData<Int>()
    val gameId: LiveData<Int>
        get() = _gameId

    private val gameRepository = GameRepository(getApplication())
    private val gameCollectionRepository = GameCollectionRepository(getApplication())

    fun setId(gameId: Int?) {
        if (_gameId.value != gameId) _gameId.value = gameId
    }

    val game: LiveData<RefreshableResource<Game>> = Transformations.switchMap(_gameId) { gameId ->
        when (gameId) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> gameRepository.getGame(gameId)
        }
    }

    val languagePoll: LiveData<GamePollEntity> = Transformations.switchMap(_gameId) { gameId ->
        when (gameId) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> gameRepository.getLanguagePoll(gameId)
        }
    }

    val agePoll: LiveData<GamePollEntity> = Transformations.switchMap(_gameId) { gameId ->
        when (gameId) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> gameRepository.getAgePoll(gameId)
        }
    }

    val ranks: LiveData<List<GameRankEntity>> = Transformations.switchMap(_gameId) { gameId ->
        when (gameId) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> gameRepository.getRanks(gameId)
        }
    }

    val playerPoll: LiveData<GamePlayerPollEntity> = Transformations.switchMap(_gameId) { gameId ->
        when (gameId) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> gameRepository.getPlayerPoll(gameId)
        }
    }

    val designers: LiveData<List<Pair<Int, String>>> = Transformations.switchMap(_gameId) { gameId ->
        when (gameId) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> gameRepository.getDesigners(gameId)
        }
    }

    val artists: LiveData<List<Pair<Int, String>>> = Transformations.switchMap(_gameId) { gameId ->
        when (gameId) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> gameRepository.getArtists(gameId)
        }
    }

    val publishers: LiveData<List<Pair<Int, String>>> = Transformations.switchMap(_gameId) { gameId ->
        when (gameId) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> gameRepository.getPublishers(gameId)
        }
    }

    val categories: LiveData<List<Pair<Int, String>>> = Transformations.switchMap(_gameId) { gameId ->
        when (gameId) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> gameRepository.getCategories(gameId)
        }
    }

    val mechanics: LiveData<List<Pair<Int, String>>> = Transformations.switchMap(_gameId) { gameId ->
        when (gameId) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> gameRepository.getMechanics(gameId)
        }
    }

    val expansions: LiveData<List<Pair<Int, String>>> = Transformations.switchMap(_gameId) { gameId ->
        when (gameId) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> gameRepository.getExpansions(gameId)
        }
    }

    val baseGames: LiveData<List<Pair<Int, String>>> = Transformations.switchMap(_gameId) { gameId ->
        when (gameId) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> gameRepository.getBaseGames(gameId)
        }
    }

    val collectionItems: LiveData<RefreshableResource<List<GameCollectionItem>>> = Transformations.switchMap(_gameId) { gameId ->
        when (gameId) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> gameCollectionRepository.getCollectionItems(gameId)
        }
    }

    val plays: LiveData<PlaysByGame> = Transformations.switchMap(_gameId) { gameId ->
        when (gameId) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> gameRepository.getPlays(gameId)
        }
    }

    val playColors: LiveData<List<String>> = Transformations.switchMap(_gameId) { gameId ->
        when (gameId) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> gameRepository.getPlayColors(gameId)
        }
    }

    fun refresh() {
        _gameId.value?.let { _gameId.value = it }
    }

    fun updateLastViewed(lastViewed: Long = System.currentTimeMillis()) {
        gameRepository.updateLastViewed(gameId.value ?: BggContract.INVALID_ID, lastViewed)
    }

    fun updateGameColors(palette: Palette?) {
        if (palette != null) {
            val iconColor = PaletteUtils.getIconSwatch(palette).rgb
            val darkColor = PaletteUtils.getDarkSwatch(palette).rgb
            val playCountColors = PaletteUtils.getPlayCountColors(palette, getApplication())
            gameRepository.updateGameColors(gameId.value
                    ?: BggContract.INVALID_ID, iconColor, darkColor, playCountColors[0], playCountColors[1], playCountColors[2])
        }
    }

    fun updateFavorite(isFavorite: Boolean) {
        gameRepository.updateFavorite(gameId.value ?: BggContract.INVALID_ID, isFavorite)
    }

    fun addCollectionItem(statuses: List<String>, wishListPriority: Int?) {
        gameCollectionRepository.addCollectionItem(gameId.value ?: BggContract.INVALID_ID, statuses, wishListPriority)
    }
}
