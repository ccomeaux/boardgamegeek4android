package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.boardgamegeek.entities.ForumEntity
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.livedata.AbsentLiveData
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.ForumRepository

class ForumsViewModel(application: Application) : AndroidViewModel(application) {
    enum class ForumType {
        GAME,
        REGION
    }

    private val _gameId = MutableLiveData<Pair<ForumType, Int>>()
    val gameId: LiveData<Pair<ForumType, Int>>
        get() = _gameId

    private val repository = ForumRepository(getApplication())

    fun setRegion() {
        if (_gameId.value?.first != ForumType.REGION) _gameId.value = (ForumType.REGION to BggContract.INVALID_ID)
    }

    fun setGameId(gameId: Int) {
        if (_gameId.value != ForumType.GAME to gameId) _gameId.value = (ForumType.GAME to gameId)
    }

    val forums: LiveData<RefreshableResource<List<ForumEntity>>> = Transformations.switchMap(_gameId) { pair ->
        when {
            pair.first == ForumType.REGION -> repository.getForums()
            pair.first == ForumType.GAME && pair.second != BggContract.INVALID_ID -> repository.getForumsForGame(pair.second)
            else -> AbsentLiveData.create()
        }
    }

}