package com.boardgamegeek.repository

import android.content.ContentProviderOperation
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.boardgamegeek.BggApplication
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.db.GameDao
import com.boardgamegeek.db.PlayDao
import com.boardgamegeek.entities.GameForPlayStatEntity
import com.boardgamegeek.entities.PlayerColorEntity
import com.boardgamegeek.entities.PlayerEntity
import com.boardgamegeek.extensions.applyBatch
import com.boardgamegeek.util.PreferencesUtils

class PlayRepository(val application: BggApplication) {
    private val playDao = PlayDao(application)
    private val gameDao = GameDao(application)
    private val collectionDao = CollectionDao(application)

    fun loadForStatsAsLiveData(): LiveData<List<GameForPlayStatEntity>> {
        // TODO use PlayDao if either of these is false
        // val isOwnedSynced = PreferencesUtils.isStatusSetToSync(application, BggService.COLLECTION_QUERY_STATUS_OWN)
        // val isPlayedSynced = PreferencesUtils.isStatusSetToSync(application, BggService.COLLECTION_QUERY_STATUS_PLAYED)

        return Transformations.map(gameDao.loadPlayInfoAsLiveData(
                PreferencesUtils.logPlayStatsIncomplete(application),
                PreferencesUtils.logPlayStatsExpansions(application),
                PreferencesUtils.logPlayStatsAccessories(application)))
        {
            return@map filterGamesOwned(it)
        }
    }

    fun loadForStats(): List<GameForPlayStatEntity> {
        val playInfo = gameDao.loadPlayInfo(PreferencesUtils.logPlayStatsIncomplete(application),
                PreferencesUtils.logPlayStatsExpansions(application),
                PreferencesUtils.logPlayStatsAccessories(application))
        return filterGamesOwned(playInfo)
    }

    private fun filterGamesOwned(playInfo: List<GameForPlayStatEntity>): List<GameForPlayStatEntity> {
        val items = collectionDao.load()
        val games = mutableListOf<GameForPlayStatEntity>()
        playInfo.forEach { game ->
            games += game.copy(isOwned = items.any { item -> item.gameId == game.id && item.own })
        }
        return games.toList()
    }

    fun loadPlayersForStats(): List<PlayerEntity> {
        return playDao.loadPlayers(PreferencesUtils.logPlayStatsIncomplete(application))
    }

    fun loadPlayersForStatsAsLiveData(): LiveData<List<PlayerEntity>> {
        return playDao.loadPlayersAsLiveData(PreferencesUtils.logPlayStatsIncomplete(application))
    }

    fun loadUserPlayer(username: String): LiveData<PlayerEntity> {
        return playDao.loadUserPlayerAsLiveData(username)
    }

    fun loadNonUserPlayer(playerName: String): LiveData<PlayerEntity> {
        return playDao.loadNonUserPlayerAsLiveData(playerName)
    }

    fun loadUserColors(username: String): LiveData<List<PlayerColorEntity>> {
        return playDao.loadUserColors(username)
    }

    fun loadPlayerColors(username: String): LiveData<List<PlayerColorEntity>> {
        return playDao.loadPlayerColors(username)
    }

    fun updatePlaysWithNickName(username: String, nickName: String): Int {
        val count = playDao.countNickNameUpdatePlays(username, nickName)
        val batch = arrayListOf<ContentProviderOperation>()
        batch += playDao.createDirtyPlaysForNickNameOperations(username, nickName)
        batch += playDao.createNickNameUpdateOperation(username, nickName)
        application.appExecutors.diskIO.execute {
            application.contentResolver.applyBatch(application, batch)
        }
        return count
    }

    fun updatePlaysWithNewName(oldName: String, newName: String) {
        val batch = arrayListOf<ContentProviderOperation>()
        batch += playDao.createDirtyPlaysForRenameOperations(oldName)
        batch += playDao.createRenameUpdateOperation(oldName, newName)
        batch += playDao.createCopyPlayerColorsOperations(oldName, newName)
        batch += playDao.createDeletePlayerColorsOperation(oldName)
        application.appExecutors.diskIO.execute {
            application.contentResolver.applyBatch(application, batch)
        }
    }

    fun updateGameHIndex(hIndex: Int) {
        PreferencesUtils.updateGameHIndex(application, hIndex)
    }

    fun updatePlayerHIndex(hIndex: Int) {
        PreferencesUtils.updatePlayerHIndex(application, hIndex)
    }
}
