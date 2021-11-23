package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.entities.*
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.GameCollectionRepository
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.util.RemoteConfig
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class GamePlayStatsViewModel(application: Application) : AndroidViewModel(application) {
    private val gameRepository = GameCollectionRepository(getApplication())
    private val playRepository = PlayRepository(getApplication())
    private val areItemsRefreshing = AtomicBoolean()
    private val refreshItemsMinutes = RemoteConfig.getInt(RemoteConfig.KEY_REFRESH_GAME_COLLECTION_MINUTES)

    private val _gameId = MutableLiveData<Int>()
    fun setGameId(gameId: Int) {
        if (_gameId.value != gameId) _gameId.value = gameId
    }

    val game = _gameId.switchMap { gameId ->
        liveData {
            val items =
                if (gameId == BggContract.INVALID_ID) emptyList()
                else gameRepository.loadCollectionItems(gameId)
            val refreshedItems =
                if (areItemsRefreshing.compareAndSet(false, true)) {
                    val lastUpdated = items.minByOrNull { it.syncTimestamp }?.syncTimestamp ?: 0L
                    when {
                        lastUpdated.isOlderThan(refreshItemsMinutes, TimeUnit.MINUTES) -> {
                            emit(RefreshableResource.refreshing(items))
                            gameRepository.refreshCollectionItems(gameId)
                        }
                        else -> items
                    }.also { areItemsRefreshing.set(false) }
                } else items
            emit(RefreshableResource.success(refreshedItems))
        }
    }

    val plays: LiveData<RefreshableResource<List<PlayEntity>>> = _gameId.switchMap { gameId ->
        liveData {
            val list = when (gameId) {
                BggContract.INVALID_ID -> null
                else -> playRepository.loadPlaysByGame(gameId) // TODO sort by  ASC
            }
            emit(RefreshableResource.success(list))
        }
    }

    val players: LiveData<List<PlayPlayerEntity>> = _gameId.switchMap { gameId ->
        liveData {
            val list = when (gameId) {
                BggContract.INVALID_ID -> emptyList()
                else -> playRepository.loadPlayersByGame(gameId)
            }
            emit(list)
        }
    }
}
