package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.boardgamegeek.entities.AuthToken
import com.boardgamegeek.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    application: Application,
    private val authRepository: AuthRepository,
) : AndroidViewModel(application) {
    private var authenticationJob: Job? = null

    private val _isAuthenticating = MutableLiveData<Boolean>()
    val isAuthenticating: LiveData<Boolean>
        get() = _isAuthenticating

    private val _authenticationResult = MutableLiveData<AuthToken?>()
    val authenticationResult: LiveData<AuthToken?>
        get() = _authenticationResult

    fun login(username: String?, password: String?) {
        _isAuthenticating.value = true
        authenticationJob = viewModelScope.launch(Dispatchers.IO) {
            val authEntity = authRepository.authenticate(username.orEmpty(), password.orEmpty(), "Dialog")
            _authenticationResult.postValue(authEntity)
            _isAuthenticating.postValue(false)
        }
    }

    fun cancel() {
        authenticationJob?.cancel()
        _isAuthenticating.value = false
    }
}
