package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.boardgamegeek.entities.HotGameEntity
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.repository.HotnessRepository
import com.boardgamegeek.repository.PlayRepository
import kotlinx.coroutines.launch
import java.lang.Exception

class HotnessViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HotnessRepository(getApplication())
    private val playRepository = PlayRepository(getApplication())

    val hotness: LiveData<RefreshableResource<List<HotGameEntity>>> = liveData {
        try {
            emit(RefreshableResource.refreshing(null))
            val games = repository.getHotness()
            emit(RefreshableResource.success(games))
        } catch (e: Exception) {
            emit(RefreshableResource.error<List<HotGameEntity>>(e, application))
        }
    }

    fun logQuickPlay(gameId: Int, gameName: String) {
        viewModelScope.launch {
            playRepository.logQuickPlay(gameId, gameName)
        }
    }
}
