package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.entities.UserEntity
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

@HiltViewModel
class SelfUserViewModel @Inject constructor(
    application: Application,
    private val userRepository: UserRepository,
) : AndroidViewModel(application) {
    private val username = MutableLiveData<String?>()

    init {
        username.value = if (Authenticator.isSignedIn(getApplication())) {
            Authenticator.getAccount(application)?.name.orEmpty()
        } else ""
    }

    fun setUsername(newUsername: String?) {
        if (username.value != newUsername) username.value = newUsername
    }

    val user: LiveData<RefreshableResource<UserEntity>> = username.switchMap { username ->
        liveData {
            when {
                !username.isNullOrBlank() -> {
                    try {
                        emit(RefreshableResource.refreshing(latestValue?.data))
                        val entity = userRepository.load(username)
                        if (entity == null || entity.updatedTimestamp.isOlderThan(1.days)) {
                            userRepository.refresh(username)
                            val refreshedUser = userRepository.load(username)
                            emit(RefreshableResource.success(refreshedUser))
                            if (username == Authenticator.getAccount(application)?.name) {
                                userRepository.updateSelf(refreshedUser)
                            }
                        } else {
                            emit(RefreshableResource.success(entity))
                        }
                    } catch (e: Exception) {
                        emit(RefreshableResource.error(e, application))
                    }
                }
                else -> {
                    userRepository.updateSelf(null)
                    emit(RefreshableResource.success(null))
                }
            }
        }
    }
}
