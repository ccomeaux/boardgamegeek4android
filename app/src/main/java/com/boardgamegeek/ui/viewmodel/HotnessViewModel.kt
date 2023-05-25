package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.boardgamegeek.entities.HotGameEntity
import com.boardgamegeek.entities.PlayUploadResult
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.livedata.Event
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.HotnessRepository
import com.boardgamegeek.repository.PlayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HotnessViewModel @Inject constructor(
    application: Application,
    private val hotnessRepository: HotnessRepository,
    private val playRepository: PlayRepository,
) : AndroidViewModel(application) {

    private val _errorMessage = MediatorLiveData<Event<String>>()
    val errorMessage: LiveData<Event<String>>
        get() = _errorMessage

    private val _loggedPlayResult = MutableLiveData<Event<PlayUploadResult>>()
    val loggedPlayResult: LiveData<Event<PlayUploadResult>>
        get() = _loggedPlayResult

    val hotness: LiveData<RefreshableResource<List<HotGameEntity>>> = liveData {
        try {
            emit(RefreshableResource.refreshing(latestValue?.data))
            val games = hotnessRepository.getHotness()
            emit(RefreshableResource.success(games))
        } catch (e: Exception) {
            emit(RefreshableResource.error(e, application))
        }
    }

    fun logQuickPlay(gameId: Int, gameName: String) {
        viewModelScope.launch {
            val result = playRepository.logQuickPlay(gameId, gameName)
            if (result.errorMessage.isNotBlank())
                _errorMessage.value = Event(result.errorMessage)
            else if (result.play.playId != BggContract.INVALID_ID)
                _loggedPlayResult.value = Event(result)
        }
    }
}
