package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.entities.UserEntity
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.livedata.AbsentLiveData
import com.boardgamegeek.repository.UserRepository
import java.util.concurrent.TimeUnit

class SelfUserViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepository = UserRepository(getApplication())
    private val username = MutableLiveData<String>()

    init {
        username.value = Authenticator.getAccount(application)?.name.orEmpty()
    }

    fun setUsername(newUsername: String?) {
        newUsername?.let {
            if (username.value != it) username.value = it
        }
    }

    val user: LiveData<RefreshableResource<UserEntity>> = username.switchMap { username ->
        when {
            username.isNotBlank() -> {
                liveData {
                    try {
                        emit(RefreshableResource.refreshing(null))
                        val entity = userRepository.load(username)
                        if (entity != null && entity.updatedTimestamp.isOlderThan(1, TimeUnit.DAYS)) {
                            val refreshedUser = userRepository.refresh(username)
                            emit(RefreshableResource.success(refreshedUser))
                            if (username == Authenticator.getAccount(application)?.name) {
                                userRepository.updateSelf(refreshedUser)
                            }
                        } else {
                            emit(RefreshableResource.success(entity))
                        }
                    } catch (e: Exception) {
                        emit(RefreshableResource.error<UserEntity>(e, application))
                    }
                }
            }
            else -> AbsentLiveData.create()
        }
    }
}
