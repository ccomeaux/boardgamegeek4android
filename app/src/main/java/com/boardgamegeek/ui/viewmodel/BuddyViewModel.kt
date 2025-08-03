package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.livedata.Event
import com.boardgamegeek.model.Player
import com.boardgamegeek.model.User
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.repository.UserRepository
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

@HiltViewModel
class BuddyViewModel @Inject constructor(
    application: Application,
    private val userRepository: UserRepository,
    private val playRepository: PlayRepository,
) : AndroidViewModel(application) {
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(getApplication())
    private val isRefreshing = AtomicBoolean()

    private val user = MutableLiveData<Pair<String?, PlayRepository.PlayerType>>()

    private val _updateMessage = MutableLiveData<Event<String>>()
    val updateMessage: LiveData<Event<String>>
        get() = _updateMessage

    private val _isUsernameValid = MutableLiveData<Event<Boolean>>()
    val isUsernameValid: LiveData<Event<Boolean>>
        get() = _isUsernameValid

    private val _refreshing = MutableLiveData<Boolean>()
    val refreshing: LiveData<Boolean>
        get() = _refreshing

    private val _error = MutableLiveData<String>()
    val error: LiveData<String>
        get() = _error

    fun setUsername(name: String?) {
        if (user.value?.first != name) user.value = (name to PlayRepository.PlayerType.USER)
    }

    fun setPlayerName(name: String?) {
        if (user.value?.first != name) user.value = (name to PlayRepository.PlayerType.NON_USER)
    }

    val username = user.map {
        when (it.second) {
            PlayRepository.PlayerType.USER -> it.first
            PlayRepository.PlayerType.NON_USER -> null
        }
    }.distinctUntilChanged()

    val playerName = user.map {
        when (it.second) {
            PlayRepository.PlayerType.USER -> null
            PlayRepository.PlayerType.NON_USER -> it.first
        }
    }.distinctUntilChanged()

    fun refresh() {
        viewModelScope.launch {
            try {
                _refreshing.value = true
                if (isRefreshing.compareAndSet(false, true)) {
                    userRepository.refresh(user.value?.first.orEmpty())?.let {
                        _error.value = it
                    }
                }
            } finally {
                _refreshing.value = false
                isRefreshing.set(false)
            }
        }
    }

    val buddy: LiveData<User?> = user.switchMap { (username, type) ->
        liveData {
            try {
                if (type == PlayRepository.PlayerType.USER && !username.isNullOrBlank()) {
                    emitSource(userRepository.loadUserFlow(username).distinctUntilChanged().asLiveData().also {
                        if (it.value?.updatedTimestamp.isOlderThan(1.days)) {
                            refresh()
                        }
                    })
                } else emit(null)
            } catch (e: Exception) {
                _error.value = e.localizedMessage
                isRefreshing.set(false)
            }
        }
    }

    val player: LiveData<Player?> = user.switchMap { user ->
        liveData {
            emit(playRepository.loadPlayer(user.first, user.second))
        }
    }

    fun generateColors() {
        viewModelScope.launch {
            user.value?.let { (name, type) ->
                val colors = playRepository.generatePlayerColors(name to type)
                playRepository.savePlayerColors(name, type, colors)
            }
        }
    }

    val colors = user.switchMap { user ->
        liveData {
            user.first?.let {
                emitSource(playRepository.loadPlayerColorsAsFlow(it, user.second).asLiveData())
            }
        }
    }

    fun updateNickName(nickName: String, updatePlays: Boolean) {
        viewModelScope.launch {
            user.value?.let { (username, type) ->
                if (type == PlayRepository.PlayerType.USER && !username.isNullOrBlank()) {
                    userRepository.updateNickName(username, nickName)

                    val message = if (updatePlays) {
                        val newNickName = nickName.ifBlank { buddy.value?.fullName }
                        if (newNickName.isNullOrBlank()) {
                            getApplication<BggApplication>().getString(R.string.msg_missing_nickname)
                        } else {
                            val internalIds = playRepository.updatePlaysWithNickName(username, newNickName)
                            playRepository.enqueueUploadRequest(internalIds)
                            getApplication<BggApplication>().resources.getQuantityString(
                                R.plurals.msg_updated_plays_buddy_nickname,
                                internalIds.size,
                                internalIds.size,
                                username,
                                newNickName
                            )
                        }
                    } else {
                        getApplication<BggApplication>().getString(R.string.msg_updated_nickname, nickName)
                    }
                    setUpdateMessage(message)
                    firebaseAnalytics.logEvent("DataManipulation") {
                        param(FirebaseAnalytics.Param.CONTENT_TYPE, "BuddyNickname")
                        param("Username", username)
                        param("NickName", nickName)
                        param("Action", "Edit")
                    }
                }
            }
        }
    }

    fun renamePlayer(newName: String) {
        viewModelScope.launch {
            user.value?.let { (oldName, type) ->
                if (type == PlayRepository.PlayerType.NON_USER && newName.isNotBlank() && !oldName.isNullOrBlank()) {
                    val internalIds = playRepository.renamePlayer(oldName, newName)
                    playRepository.enqueueUploadRequest(internalIds)
                    setUpdateMessage(getApplication<BggApplication>().getString(R.string.msg_play_player_change, oldName, newName))
                    setPlayerName(newName)
                    firebaseAnalytics.logEvent("DataManipulation") {
                        param(FirebaseAnalytics.Param.CONTENT_TYPE, "RenamePlayer")
                        param("OldName", oldName)
                        param("NewName", newName)
                        param("Action", "Edit")
                    }
                }
            }
        }
    }

    fun addUsernameToPlayer(username: String) {
        viewModelScope.launch {
            user.value?.let { (playerName, type) ->
                if (type == PlayRepository.PlayerType.NON_USER && username.isNotBlank() && !playerName.isNullOrBlank()) {
                    val error = userRepository.refresh(username)
                    if (error.isNullOrEmpty()) {
                        userRepository.updateNickName(username, playerName)

                        val internalIds = playRepository.addUsernameToPlayer(playerName, username)
                        playRepository.enqueueUploadRequest(internalIds)
                        setUpdateMessage(getApplication<BggApplication>().getString(R.string.msg_player_add_username, username, playerName))
                        setUsername(username)
                        firebaseAnalytics.logEvent("DataManipulation") {
                            param(FirebaseAnalytics.Param.CONTENT_TYPE, "AddUserName")
                            param("PlayerName", playerName)
                            param("Username", username)
                            param("Action", "Edit")
                        }
                    }
                }
            }
        }
    }

    fun validateUsername(username: String) {
        viewModelScope.launch {
            delay(200)
            _isUsernameValid.value = Event(userRepository.validateUsername(username))
        }
    }

    private fun setUpdateMessage(message: String) {
        _updateMessage.postValue(Event(message))
    }
}
