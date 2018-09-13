package com.boardgamegeek.repository

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Transformations
import com.boardgamegeek.BggApplication
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.db.GameDao
import com.boardgamegeek.db.PlayDao
import com.boardgamegeek.entities.GameForPlayStatEntity
import com.boardgamegeek.entities.PlayerEntity
import com.boardgamegeek.util.PreferencesUtils

class PlayRepository(val application: BggApplication) {
    private val playDao = PlayDao(application)
    private val gameDao = GameDao(application)
    private val collectionDao = CollectionDao(application)

    fun loadForStats(): LiveData<List<GameForPlayStatEntity>> {
        // TODO use PlayDao if either of these is false
        // val isOwnedSynced = PreferencesUtils.isStatusSetToSync(application, BggService.COLLECTION_QUERY_STATUS_OWN)
        // val isPlayedSynced = PreferencesUtils.isStatusSetToSync(application, BggService.COLLECTION_QUERY_STATUS_PLAYED)

        return Transformations.map(gameDao.loadPlayInfo(
                PreferencesUtils.logPlayStatsIncomplete(application),
                PreferencesUtils.logPlayStatsExpansions(application),
                PreferencesUtils.logPlayStatsAccessories(application)))
        { games ->
            val items = collectionDao.load()
            val g = mutableListOf<GameForPlayStatEntity>()
            games.forEach { game ->
                g += game.copy(isOwned = items.any { item -> item.gameId == game.id && item.own })
            }
            return@map g.toList()
        }
    }

    fun loadPlayersForStats(): LiveData<List<PlayerEntity>> {
        return playDao.loadPlayers(PreferencesUtils.logPlayStatsIncomplete(application))
    }

    fun updateGameHIndex(hIndex: Int) {
        PreferencesUtils.updateGameHIndex(application, hIndex)
    }

    fun updatePlayerHIndex(hIndex: Int) {
        PreferencesUtils.updatePlayerHIndex(application, hIndex)
    }
}
