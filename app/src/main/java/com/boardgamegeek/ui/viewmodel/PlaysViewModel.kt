package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.boardgamegeek.repository.PlayRepository

class PlaysViewModel(application: Application) : AndroidViewModel(application) {
    val playRepository = PlayRepository(getApplication())

//    val locations: LiveData<RefreshableResource<GameEntity>> = Transformations.switchMap(_gameId) { gameId ->
//        when (gameId) {
//            BggContract.INVALID_ID -> AbsentLiveData.create()
//            else -> playRepository.getGame(gameId)
//        }
//    }
}