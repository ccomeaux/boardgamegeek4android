package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.boardgamegeek.entities.PlayerColorEntity
import com.boardgamegeek.entities.PlayerEntity
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.entities.UserEntity
import com.boardgamegeek.livedata.AbsentLiveData
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.repository.UserRepository

class BuddyViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepository = UserRepository(getApplication())
    private val playRepository = PlayRepository(getApplication())

    private val _user = MutableLiveData<Pair<String?, Int>>()
    val user: LiveData<Pair<String?, Int>>
        get() = _user

    private val _playerName = MutableLiveData<String>()
    val playerName: LiveData<String>
        get() = _playerName

    fun setUsername(name: String?) {
        if (_user.value?.first != name) _user.value = (name to TYPE_USER)
    }

    fun setPlayerName(name: String?) {
        if (_user.value?.first != name) _user.value = (name to TYPE_PLAYER)
    }

    fun refresh() {
        _user.value?.let { _user.value = it }
    }

    val buddy: LiveData<RefreshableResource<UserEntity>> = Transformations.switchMap(_user) { user ->
        val name = user.first
        when {
            name == null || name.isBlank() -> AbsentLiveData.create()
            user.second == TYPE_USER -> userRepository.loadUser(name)
            else -> AbsentLiveData.create()
        }
    }

    val player: LiveData<PlayerEntity> = Transformations.switchMap(_user) { user ->
        val name = user.first
        when {
            name == null || name.isBlank() -> AbsentLiveData.create()
            user.second == TYPE_USER -> playRepository.loadUserPlayer(name)
            user.second == TYPE_USER -> playRepository.loadNonUserPlayer(name)
            else -> AbsentLiveData.create()
        }
    }

    val colors: LiveData<List<PlayerColorEntity>> = Transformations.switchMap(_user) { user ->
        val name = user.first
        when {
            name == null || name.isBlank() -> AbsentLiveData.create()
            user.second == TYPE_USER -> playRepository.loadUserColors(name)
            user.second == TYPE_PLAYER -> playRepository.loadPlayerColors(name)
            else -> AbsentLiveData.create()
        }
    }

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_PLAYER = 2
    }
}