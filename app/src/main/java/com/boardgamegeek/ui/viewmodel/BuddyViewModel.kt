package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.entities.PlayerEntity
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.entities.UserEntity
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

    private val _userTypeAndName = MutableLiveData<Pair<String?, Int>>()
    val user: LiveData<Pair<String?, Int>>
        get() = _userTypeAndName

    private val _updateMessage = MutableLiveData<Event<String>>()
    val updateMessage: LiveData<Event<String>>
        get() = _updateMessage

    private val _isUsernameValid = MutableLiveData<Event<Boolean>>()
    val isUsernameValid: LiveData<Event<Boolean>>
        get() = _isUsernameValid

    fun setUsername(name: String?) {
        if (_userTypeAndName.value?.first != name) _userTypeAndName.value = (name to TYPE_USER)
    }

    fun setPlayerName(name: String?) {
        if (_userTypeAndName.value?.first != name) _userTypeAndName.value = (name to TYPE_PLAYER)
    }

    fun refresh() {
        _userTypeAndName.value?.let { _userTypeAndName.value = it }
    }

    val buddy: LiveData<RefreshableResource<UserEntity>> = _userTypeAndName.switchMap { userTypeAndName ->
        liveData {
            try {
                when (userTypeAndName.second) {
                    TYPE_USER -> {
                        userTypeAndName.first?.let { username ->
                            val loadedUser = userRepository.load(username)
                            val refreshedUser = if ((loadedUser == null || loadedUser.updatedTimestamp.isOlderThan(0.days)) &&
                                isRefreshing.compareAndSet(false, true)
                            ) {
                                emit(RefreshableResource.refreshing(loadedUser))
                                val user = userRepository.refresh(username).also {
                                    isRefreshing.set(false)
                                }
                                loadedUser?.let { user.copy(playNickname = it.playNickname) } ?: user
                            } else loadedUser
                            emit(RefreshableResource.success(refreshedUser))
                        } ?: emit(RefreshableResource.success(null))
                    }
                    TYPE_PLAYER -> emit(RefreshableResource.success(null))
                }
            } catch (e: Exception) {
                isRefreshing.set(false)
                emit(RefreshableResource.error(e, application))
            }
        }
    }

    val player: LiveData<PlayerEntity?> = _userTypeAndName.switchMap { user ->
        liveData {
            val name = user.first
            emit(
                when {
                    name.isNullOrBlank() -> null
                    user.second == TYPE_USER -> playRepository.loadUserPlayer(name)
                    user.second == TYPE_PLAYER -> playRepository.loadNonUserPlayer(name)
                    else -> null
                }
            )
        }
    }

    val colors = _userTypeAndName.switchMap { user ->
        liveData {
            val name = user.first
            emit(
                when {
                    name.isNullOrBlank() -> emptyList()
                    user.second == TYPE_USER -> playRepository.loadUserColors(name)
                    user.second == TYPE_PLAYER -> playRepository.loadPlayerColors(name)
                    else -> emptyList()
                }
            )
        }
    }

    fun updateNickName(nickName: String, updatePlays: Boolean) {
        viewModelScope.launch {
            if (user.value?.second == TYPE_USER) {
                val username = user.value?.first
                if (!username.isNullOrBlank()) {
                    userRepository.updateNickName(username, nickName)
                    refresh()

                    val message = if (updatePlays) {
                        val newNickName = nickName.ifBlank { buddy.value?.data?.fullName }
                        if (newNickName.isNullOrBlank()) {
                            getApplication<BggApplication>().getString(R.string.msg_missing_nickname)
                        } else {
                            val count = playRepository.updatePlaysWithNickName(username, newNickName)
                            playRepository.enqueueUploadRequest()
                            getApplication<BggApplication>().resources.getQuantityString(R.plurals.msg_updated_plays_buddy_nickname, count, count, username, newNickName)
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
            val oldName = user.value?.first
            if (user.value?.second == TYPE_PLAYER && newName.isNotBlank() && !oldName.isNullOrBlank()) {
                playRepository.renamePlayer(oldName, newName)
                playRepository.enqueueUploadRequest()
                setUpdateMessage(
                    getApplication<BggApplication>().getString(
                        R.string.msg_play_player_change,
                        oldName,
                        newName
                    )
                )
                setPlayerName(newName)
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
            val playerName = user.value?.first
            if (user.value?.second == TYPE_PLAYER &&
                username.isNotBlank() && !playerName.isNullOrBlank()
            ) {
                playRepository.addUsernameToPlayer(playerName, username)
                playRepository.enqueueUploadRequest()
                setUpdateMessage(
                    getApplication<BggApplication>().getString(
                        R.string.msg_player_add_username,
                        username,
                        playerName
                    )
                )
                setUsername(username)
            }
        }
    }

    private fun setUpdateMessage(message: String) {
        _updateMessage.postValue(Event(message))
    }

    companion object {
        const val TYPE_USER = 1
        const val TYPE_PLAYER = 2
    }
}
