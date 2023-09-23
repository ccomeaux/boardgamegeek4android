package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.entities.PlayerEntity
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.entities.User
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.livedata.Event
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.repository.UserRepository
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import dagger.hilt.android.lifecycle.HiltViewModel
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

    fun setUsername(name: String?) {
        if (user.value?.first != name) user.value = (name to PlayRepository.PlayerType.USER)
    }

    fun setPlayerName(name: String?) {
        if (user.value?.first != name) user.value = (name to PlayRepository.PlayerType.NON_USER)
    }

    fun refresh() {
        user.value?.let { user.value = it }
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

    val buddy: LiveData<RefreshableResource<User>> = user.switchMap { (username, type) ->
        liveData {
            try {
                when (type) {
                    PlayRepository.PlayerType.USER -> {
                        if (username.isNullOrBlank()) {
                            emit(RefreshableResource.success(null))
                        } else {
                            val loadedUser = userRepository.load(username)
                            val refreshedUser = if ((loadedUser == null || loadedUser.updatedTimestamp.isOlderThan(0.days)) &&
                                isRefreshing.compareAndSet(false, true)
                            ) {
                                emit(RefreshableResource.refreshing(loadedUser))
                                userRepository.refresh(username)
                                val user = userRepository.load(username)
                                isRefreshing.set(false)
                                user
                            } else loadedUser
                            emit(RefreshableResource.success(refreshedUser))
                        }
                    }
                    PlayRepository.PlayerType.NON_USER -> emit(RefreshableResource.success(null))
                }
            } catch (e: Exception) {
                isRefreshing.set(false)
                emit(RefreshableResource.error(e, application))
            }
        }
    }

    val player: LiveData<PlayerEntity?> = user.switchMap { user ->
        liveData {
            emit(playRepository.loadPlayer(user.first, user.second))
        }
    }

    val colors = user.switchMap { user ->
        liveData {
            user.first?.let {
                emit(playRepository.loadPlayerColors(it, user.second))
            }
        }
    }

    fun updateNickName(nickName: String, updatePlays: Boolean) {
        viewModelScope.launch {
            user.value?.let { (username, type) ->
                if (type == PlayRepository.PlayerType.USER && !username.isNullOrBlank()) {
                    userRepository.updateNickName(username, nickName)
                    refresh()

                    val message = if (updatePlays) {
                        val newNickName = nickName.ifBlank { buddy.value?.data?.fullName }
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
                }
            }
        }
    }

    fun validateUsername(username: String) {
        viewModelScope.launch {
            if (username.isBlank())
                _isUsernameValid.value = Event(false)
            else {
                _isUsernameValid.value = Event(userRepository.validateUsername(username))
            }
        }
    }

    fun addUsernameToPlayer(username: String) {
        viewModelScope.launch {
            user.value?.let { (playerName, type) ->
                if (type == PlayRepository.PlayerType.NON_USER && username.isNotBlank() && !playerName.isNullOrBlank()) {
                    val internalIds = playRepository.addUsernameToPlayer(playerName, username)
                    playRepository.enqueueUploadRequest(internalIds)
                    setUpdateMessage(getApplication<BggApplication>().getString(R.string.msg_player_add_username, username, playerName))
                    setUsername(username)
                }
            }
        }
    }

    private fun setUpdateMessage(message: String) {
        _updateMessage.postValue(Event(message))
    }
}
