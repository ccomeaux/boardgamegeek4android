package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.model.User
import com.boardgamegeek.extensions.AccountPreferences
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.livedata.LiveSharedPreference
import com.boardgamegeek.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

@HiltViewModel
class SelfUserViewModel @Inject constructor(
    application: Application,
    private val userRepository: UserRepository,
) : AndroidViewModel(application) {
    val username: LiveData<String?> = LiveSharedPreference<String>(getApplication(), AccountPreferences.KEY_USERNAME).distinctUntilChanged()

    val user: LiveData<User?> = username.switchMap { username ->
        liveData {
            if (!username.isNullOrBlank()) {
                emitSource(userRepository.loadUserFlow(username).distinctUntilChanged().asLiveData().also {
                    if (it.value == null || it.value?.updatedTimestamp.isOlderThan(1.days)) {
                        val errorMessage = userRepository.refresh(username, true)
                        if (!errorMessage.isNullOrBlank()) {
                            Timber.w(errorMessage)
                        }
                    }
                })
            } else {
                emit(null)
                userRepository.updateSelf(null)
            }
        }
    }
}
