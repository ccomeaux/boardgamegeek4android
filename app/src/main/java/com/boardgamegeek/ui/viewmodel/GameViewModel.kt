package com.boardgamegeek.ui.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.support.v7.graphics.Palette
import com.boardgamegeek.entities.GameRankEntity
import com.boardgamegeek.entities.GameSuggestedLanguagePollEntity
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.GameCollectionRepository
import com.boardgamegeek.repository.GamePollRepository
import com.boardgamegeek.repository.GameRankRepository
import com.boardgamegeek.repository.GameRepository
import com.boardgamegeek.ui.model.Game
import com.boardgamegeek.ui.model.GameCollectionItem
import com.boardgamegeek.ui.model.RefreshableResource
import com.boardgamegeek.util.PaletteUtils

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private var gameId = BggContract.INVALID_ID

    private val gameRepository = GameRepository(getApplication())
    private val gameRanksRepository = GameRankRepository(getApplication())
    private val gamePollRepository = GamePollRepository(getApplication())
    private val gameCollectionRepository = GameCollectionRepository(getApplication())

    private var game: LiveData<RefreshableResource<Game>> = MutableLiveData<RefreshableResource<Game>>()
    private var gameRanks: LiveData<List<GameRankEntity>> = MutableLiveData<List<GameRankEntity>>()
    private var languagePoll: LiveData<GameSuggestedLanguagePollEntity> = MutableLiveData<GameSuggestedLanguagePollEntity>()
    private var gameCollectionItems: LiveData<RefreshableResource<List<GameCollectionItem>>> = MutableLiveData<RefreshableResource<List<GameCollectionItem>>>()

    fun getGame(gameId: Int): LiveData<RefreshableResource<Game>> {
        if (gameId != this.gameId) {
            this.gameId = gameId
            game = gameRepository.getGame(gameId)
        }
        return game
    }

    fun getGameRanks(): LiveData<List<GameRankEntity>> {
        if (this.gameId != BggContract.INVALID_ID) {
            gameRanks = gameRanksRepository.getRanks(gameId)
        }
        return gameRanks
    }

    fun getLanguagePoll(): LiveData<GameSuggestedLanguagePollEntity> {
        if (this.gameId != BggContract.INVALID_ID) {
            languagePoll = gamePollRepository.getLanguagePoll(gameId)
        }
        return languagePoll
    }

    fun getGameCollection(): LiveData<RefreshableResource<List<GameCollectionItem>>> {
        if (this.gameId != BggContract.INVALID_ID) {
            gameCollectionItems = gameCollectionRepository.getCollectionItems(gameId)
        }
        return gameCollectionItems
    }

    fun refreshGame() {
        gameRepository.refreshGame()
    }

    fun refreshCollectionItems() {
        gameCollectionRepository.refresh()
    }

    fun updateLastViewed(lastViewed: Long = System.currentTimeMillis()) {
        gameRepository.updateLastViewed(lastViewed)
    }

    fun updateHeroImageUrl(url: String) {
        val data = game.value?.data ?: return
        gameRepository.updateHeroImageUrl(url, data.imageUrl, data.thumbnailUrl, data.heroImageUrl)
    }

    fun updateColors(palette: Palette?) {
        if (palette != null) {
            val iconColor = PaletteUtils.getIconSwatch(palette).rgb
            val darkColor = PaletteUtils.getDarkSwatch(palette).rgb
            val playCountColors = PaletteUtils.getPlayCountColors(palette, getApplication())
            gameRepository.updateColors(iconColor, darkColor, playCountColors[0], playCountColors[1], playCountColors[2])
        }
    }

    fun updateFavorite(isFavorite: Boolean) {
        gameRepository.updateFavorite(isFavorite)
    }
}
