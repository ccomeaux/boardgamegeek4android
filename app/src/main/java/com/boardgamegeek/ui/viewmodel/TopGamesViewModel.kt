package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.entities.TopGameEntity
import com.boardgamegeek.repository.TopGameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TopGamesViewModel @Inject constructor(
    application: Application,
    private val repository: TopGameRepository,
) : AndroidViewModel(application) {
    val topGames: LiveData<RefreshableResource<List<TopGameEntity>>> = liveData {
        emit(RefreshableResource.refreshing(latestValue?.data))
        emit(
            try {
                val topGames = repository.findTopGames()
                RefreshableResource.success(topGames)
            } catch (e: Exception) {
                RefreshableResource.error(e, application)
            }
        )
    }
}
