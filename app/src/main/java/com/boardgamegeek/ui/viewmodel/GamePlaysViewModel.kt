package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.livedata.Event
import com.boardgamegeek.livedata.EventLiveData
import com.boardgamegeek.model.Play
import com.boardgamegeek.model.PlayUploadResult
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.GameRepository
import com.boardgamegeek.repository.PlayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

@HiltViewModel
class GamePlaysViewModel @Inject constructor(
    application: Application,
    private val playRepository: PlayRepository,
    private val gameRepository: GameRepository,
) : AndroidViewModel(application) {
    private val gameId = MutableLiveData<Int>()

    private val refreshing = AtomicBoolean(false)

    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean>
        get() = _isRefreshing

    private val _errorMessage = EventLiveData()
    val errorMessage: LiveData<Event<String>>
        get() = _errorMessage

    private val _loggedPlayResult = MutableLiveData<Event<PlayUploadResult>>()
    val loggedPlayResult: LiveData<Event<PlayUploadResult>>
        get() = _loggedPlayResult

    fun setGame(id: Int) {
        gameId.value = id
    }

    val plays = gameId.switchMap {
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        playRepository.loadPlaysByGameFlow(it).distinctUntilChanged().asLiveData().map { list ->
            list.groupBy { play ->
                if (play.dateInMillis == Play.UNKNOWN_DATE)
                    DEFAULT_HEADER
                else
                    dateFormat.format(play.dateInMillis)!!
            }
        }
    }

    val refreshTimestamp = gameId.switchMap { id ->
        gameRepository.loadGameFlow(id).map { game ->
            game?.updatedPlays ?: 0
        }.asLiveData().distinctUntilChanged()
    }

    fun refresh() {
        refreshTimestamp.value?.let {
            if (!it.isOlderThan(10.minutes)) return
        }
        gameId.value?.let {
            if (refreshing.compareAndSet(false, true)) {
                _isRefreshing.value = true
                viewModelScope.launch {
                    try {
                        playRepository.refreshPlaysForGame(it)
                    } catch (exception: Exception) {
                        _errorMessage.postMessage(exception)
                    } finally {
                        _isRefreshing.value = false
                        refreshing.set(false)
                    }
                }
            }
        }
    }

    fun logQuickPlay(gameId: Int, gameName: String) {
        viewModelScope.launch {
            val result = playRepository.logQuickPlay(gameId, gameName)
            if (result.isFailure)
                result.exceptionOrNull()?.let { _errorMessage.setMessage(it) }
            else {
                result.getOrNull()?.let {
                    if (it.play.playId != BggContract.INVALID_ID)
                        _loggedPlayResult.value = Event(it)
                }
            }
        }
    }

    companion object {
        private const val DEFAULT_HEADER = "?"
    }
}