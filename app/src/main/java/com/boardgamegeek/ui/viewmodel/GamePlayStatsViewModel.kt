package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.model.*
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.repository.GameCollectionRepository
import com.boardgamegeek.repository.GameRepository
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.util.RemoteConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

@HiltViewModel
class GamePlayStatsViewModel @Inject constructor(
    application: Application,
    private val gameRepository: GameRepository,
    private val gameCollectionRepository: GameCollectionRepository,
    private val playRepository: PlayRepository,
) : AndroidViewModel(application) {
    private val arePlaysRefreshing = AtomicBoolean(false)
    private val areItemsRefreshing = AtomicBoolean(false)
    private val refreshItemsMinutes = RemoteConfig.getInt(RemoteConfig.KEY_REFRESH_GAME_COLLECTION_MINUTES)

    private val _gameId = MutableLiveData<Int>()
    fun setGameId(gameId: Int) {
        if (_gameId.value != gameId) _gameId.value = gameId
    }

    val collectionItems: LiveData<List<CollectionItem>> = _gameId.switchMap { gameId ->
        liveData {
            emitSource(
                gameCollectionRepository.loadCollectionItemsForGameFlow(gameId)
                    .distinctUntilChanged()
                    .onEach { list ->
                        if (areItemsRefreshing.compareAndSet(false, true)) {
                            val lastUpdated = list.minOfOrNull { it.syncTimestamp } ?: 0L
                            if (lastUpdated.isOlderThan(refreshItemsMinutes.minutes)) {
                                gameCollectionRepository.refreshCollectionItems(gameId)
                            }
                            areItemsRefreshing.set(false)
                        }
                    }
                    .asLiveData()
            )
        }
    }

    val plays: LiveData<List<Play>> = _gameId.switchMap { gameId ->
        liveData {
            emitSource(
                playRepository
                    .loadPlaysByGameFlow(gameId)
                    .distinctUntilChanged { old, new ->
                        old.size == new.size && old.map { it.copy(updateTimestamp = 0) }.toSet() == new.map { it.copy(updateTimestamp = 0) }.toSet()
                    }
                    .onEach {
                        if (arePlaysRefreshing.compareAndSet(false, true)) {
                            val game = gameRepository.loadGame(gameId)
                            if (game == null || game.updatedPlays.isOlderThan(10.minutes)) {
                                playRepository.refreshPlaysForGame(gameId)
                            }
                            arePlaysRefreshing.set(false)
                        }
                    }
                    .asLiveData()
            )
        }
    }

    val players: LiveData<List<PlayPlayer>> = _gameId.switchMap { gameId ->
        liveData {
            emitSource(playRepository.loadPlayersByGameFlow(gameId).asLiveData())
        }
    }
}
