package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
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
import kotlinx.coroutines.launch

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

    val buddy: LiveData<RefreshableResource<UserEntity>> = _user.switchMap { user ->
        liveData {
            try {
                emit(RefreshableResource.refreshing(null))
                val name = user.first
                val d = when {
                    name == null || name.isBlank() -> null
                    user.second == TYPE_USER -> userRepository.load(name)
                    else -> null
                }
                emit(RefreshableResource.success(d))
            } catch (e: Exception) {
                emit(RefreshableResource.error<UserEntity>(e, application))
            }
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

    val colors = _user.switchMap { user ->
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
