package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.model.RefreshableResource
import com.boardgamegeek.model.User
import com.boardgamegeek.extensions.AccountPreferences
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.livedata.LiveSharedPreference
import com.boardgamegeek.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

@HiltViewModel
class SelfUserViewModel @Inject constructor(
    application: Application,
    private val userRepository: UserRepository,
) : AndroidViewModel(application) {
    val username: LiveSharedPreference<String> = LiveSharedPreference(getApplication(), AccountPreferences.KEY_USERNAME)

    val user: LiveData<RefreshableResource<User>> = username.switchMap { username ->
        liveData {
            when {
                !username.isNullOrBlank() -> {
                    try {
                        emit(RefreshableResource.refreshing(latestValue?.data))
                        val localUser = userRepository.load(username)
                        if (localUser == null || localUser.updatedTimestamp.isOlderThan(1.days)) {
                            userRepository.refresh(username)
                            val refreshedUser = userRepository.load(username)
                            emit(RefreshableResource.success(refreshedUser))
                            if (username == Authenticator.getAccount(application)?.name) {
                                userRepository.updateSelf(refreshedUser)
                            }
                        } else {
                            emit(RefreshableResource.success(localUser))
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
