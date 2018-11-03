package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.auth.AccountUtils
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.entities.Status
import com.boardgamegeek.entities.UserEntity
import com.boardgamegeek.livedata.AbsentLiveData
import com.boardgamegeek.repository.UserRepository

class SelfUserViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepository = UserRepository(getApplication())
    private val username = MutableLiveData<String>()

    init {
        username.value = Authenticator.getAccount(application)?.name ?: ""
    }

    val user: LiveData<RefreshableResource<UserEntity>> = Transformations.switchMap(username) { username ->
        when {
            username.isNotBlank() -> {
                val mediatorLiveData = MediatorLiveData<RefreshableResource<UserEntity>>()
                mediatorLiveData.addSource(userRepository.loadUser(username)) {
                    if (it.status == Status.SUCCESS) {
                        if (username == Authenticator.getAccount(application)?.name) {
                            it.data?.let { user ->
                                Authenticator.putUserId(application, user.id)
                                AccountUtils.setUsername(application, user.userName)
                                AccountUtils.setFullName(application, user.fullName)
                                AccountUtils.setAvatarUrl(application, user.avatarUrl)
                            }
                        }
                    }
                    mediatorLiveData.value = it
                }
                mediatorLiveData
            }
            else -> AbsentLiveData.create<RefreshableResource<UserEntity>>()
        }
    }
}