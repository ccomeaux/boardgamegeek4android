package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.entities.PlayerColorEntity
import com.boardgamegeek.entities.PlayerEntity
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.entities.UserEntity
import com.boardgamegeek.livedata.AbsentLiveData
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.repository.UserRepository
import com.boardgamegeek.service.SyncService
import com.boardgamegeek.util.fabric.DataManipulationEvent

class BuddyViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepository = UserRepository(getApplication())
    private val playRepository = PlayRepository(getApplication())

    private val _user = MutableLiveData<Pair<String?, Int>>()
    val user: LiveData<Pair<String?, Int>>
        get() = _user

    private val _playerName = MutableLiveData<String>()
    val playerName: LiveData<String>
        get() = _playerName

    private val _updateMessage = MutableLiveData<String>()
    val updateMessage: LiveData<String>
        get() = _updateMessage

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

    fun updateNickName(nickName: String, updatePlays: Boolean) {
        if (user.value?.second != TYPE_USER) return
        val username = user.value?.first
        if (username == null || username.isBlank()) return

        userRepository.updateNickName(username, nickName)

        if (updatePlays) {
            if (nickName.isBlank()) {
                // TODO default to user full name instead
                setUpdateMessage(getApplication<BggApplication>().getString(R.string.msg_missing_nickname))
            } else {
                val count = playRepository.updatePlaysWithNickName(username, nickName)
                setUpdateMessage(getApplication<BggApplication>().resources.getQuantityString(R.plurals.msg_updated_plays_buddy_nickname, count, count, username, nickName))
                SyncService.sync(getApplication(), SyncService.FLAG_SYNC_PLAYS_UPLOAD)
            }
        } else {
            setUpdateMessage(getApplication<BggApplication>().getString(R.string.msg_updated_nickname, nickName))
        }
        DataManipulationEvent.log("BuddyNickname", "Edit")
    }

    private fun setUpdateMessage(message: String) {
        _updateMessage.value = message
    }

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_PLAYER = 2
    }
}