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
import com.boardgamegeek.service.SyncService
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class BuddyViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepository = UserRepository(getApplication())
    private val playRepository = PlayRepository(getApplication())
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(getApplication())
    private val isRefreshing = AtomicBoolean()

    private val _userTypeAndName = MutableLiveData<Pair<String?, Int>>()
    val user: LiveData<Pair<String?, Int>>
        get() = _userTypeAndName

    private val _updateMessage = MutableLiveData<Event<String>>()
    val updateMessage: LiveData<Event<String>>
        get() = _updateMessage

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
                        userTypeAndName.first?.let {
                            val loadedUser = when {
                                it.isBlank() -> null
                                else -> userRepository.load(it)
                            }
                            val refreshedUser =
                                if ((loadedUser == null || loadedUser.updatedTimestamp.isOlderThan(0, TimeUnit.DAYS)) &&
                                    isRefreshing.compareAndSet(false, true)
                                ) {
                                    emit(RefreshableResource.refreshing(loadedUser))
                                    userRepository.refresh(it).also {
                                        isRefreshing.set(false)
                                    }
                                } else loadedUser
                            emit(RefreshableResource.success(refreshedUser))
                        } ?: emit(RefreshableResource.success(null))
                    }
                    TYPE_PLAYER -> emit(RefreshableResource.success(null))
                }
            } catch (e: Exception) {
                isRefreshing.set(false)
                emit(RefreshableResource.error<UserEntity>(e, application))
            }
        }
    }

    val player: LiveData<PlayerEntity?> = _userTypeAndName.switchMap { user ->
        liveData {
            val name = user.first
            val p = when {
                name == null || name.isBlank() -> null
                user.second == TYPE_USER -> playRepository.loadUserPlayer(name)
                user.second == TYPE_PLAYER -> playRepository.loadNonUserPlayer(name)
                else -> null
            }
            emit(p)
        }
    }

    val colors = _userTypeAndName.switchMap { user ->
        liveData {
            val name = user.first
            emit(
                when {
                    name == null || name.isBlank() -> emptyList()
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
                if (username != null && username.isNotBlank()) {
                    userRepository.updateNickName(username, nickName)
                    refresh()

                    if (updatePlays) {
                        if (nickName.isBlank()) {
                            // TODO default to user full name instead
                            setUpdateMessage(getApplication<BggApplication>().getString(R.string.msg_missing_nickname))
                        } else {
                            val count = playRepository.updatePlaysWithNickName(username, nickName)
                            setUpdateMessage(
                                getApplication<BggApplication>().resources.getQuantityString(
                                    R.plurals.msg_updated_plays_buddy_nickname,
                                    count,
                                    count,
                                    username,
                                    nickName
                                )
                            )
                            SyncService.sync(getApplication(), SyncService.FLAG_SYNC_PLAYS_UPLOAD)
                        }
                    } else {
                        setUpdateMessage(
                            getApplication<BggApplication>().getString(
                                R.string.msg_updated_nickname,
                                nickName
                            )
                        )
                    }
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
            if (user.value?.second == TYPE_PLAYER &&
                newName.isNotBlank() &&
                oldName != null && oldName.isNotBlank()
            ) {
                playRepository.renamePlayer(oldName, newName)
                SyncService.sync(getApplication(), SyncService.FLAG_SYNC_PLAYS_UPLOAD)
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

    fun addUsernameToPlayer(username: String) {
        viewModelScope.launch {
            val playerName = user.value?.first
            if (user.value?.second == TYPE_PLAYER &&
                username.isNotBlank() &&
                playerName != null && playerName.isNotBlank()
            ) {
                playRepository.addUsernameToPlayer(playerName, username)
                SyncService.sync(getApplication(), SyncService.FLAG_SYNC_PLAYS_UPLOAD)
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
