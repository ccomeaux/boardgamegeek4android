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
import com.boardgamegeek.livedata.Event
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.repository.UserRepository
import com.boardgamegeek.service.SyncService
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent

class BuddyViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepository = UserRepository(getApplication())
    private val playRepository = PlayRepository(getApplication())
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(getApplication())

    private val _user = MutableLiveData<Pair<String?, Int>>()
    val user: LiveData<Pair<String?, Int>>
        get() = _user

    private val _updateMessage = MutableLiveData<Event<String>>()
    val updateMessage: LiveData<Event<String>>
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
            user.second == TYPE_PLAYER -> playRepository.loadNonUserPlayer(name)
            else -> AbsentLiveData.create()
        }
    }

    val colors: LiveData<List<PlayerColorEntity>> = Transformations.switchMap(_user) { user ->
        val name = user.first
        when {
            name == null || name.isBlank() -> AbsentLiveData.create()
            user.second == TYPE_USER -> playRepository.loadUserColorsAsLiveData(name)
            user.second == TYPE_PLAYER -> playRepository.loadPlayerColorsAsLiveData(name)
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
        firebaseAnalytics.logEvent("DataManipulation") {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "BuddyNickname")
            param("Action", "Edit")
        }
    }

    fun renamePlayer(newName: String) {
        if (newName.isBlank()) return
        if (user.value?.second != TYPE_PLAYER) return
        val oldName = user.value?.first
        if (oldName == null || oldName.isBlank()) return
        playRepository.renamePlayer(oldName, newName)
        SyncService.sync(getApplication(), SyncService.FLAG_SYNC_PLAYS_UPLOAD)
        setUpdateMessage(getApplication<BggApplication>().getString(R.string.msg_play_player_change, oldName, newName))
        setPlayerName(newName)
    }

    fun addUsernameToPlayer(username: String) {
        if (username.isBlank()) return
        if (user.value?.second != TYPE_PLAYER) return
        val playerName = user.value?.first
        if (playerName == null || playerName.isBlank()) return
        playRepository.addUsernameToPlayer(playerName, username)
        SyncService.sync(getApplication(), SyncService.FLAG_SYNC_PLAYS_UPLOAD)
        setUpdateMessage(getApplication<BggApplication>().getString(R.string.msg_player_add_username, username, playerName))
        setUsername(username)
    }

    private fun setUpdateMessage(message: String) {
        _updateMessage.value = Event(message)
    }

    companion object {
        const val TYPE_USER = 1
        const val TYPE_PLAYER = 2
    }
}