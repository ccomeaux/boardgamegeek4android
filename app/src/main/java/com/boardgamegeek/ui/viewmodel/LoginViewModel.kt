package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.boardgamegeek.auth.BggCookieJar
import com.boardgamegeek.auth.NetworkAuthenticator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private var authenticationJob: Job? = null

    private val _isAuthenticating = MutableLiveData<Boolean>()
    val isAuthenticating: LiveData<Boolean>
        get() = _isAuthenticating

    private val _authenticationResult = MutableLiveData<BggCookieJar?>()
    val authenticationResult: LiveData<BggCookieJar?>
        get() = _authenticationResult

    fun login(username: String?, password: String?) {
        _isAuthenticating.value = true
        authenticationJob = viewModelScope.launch(Dispatchers.IO) {
            val bggCookieJar = NetworkAuthenticator.authenticate(
                username.orEmpty(), password.orEmpty(), "Dialog", getApplication()
            )
            _authenticationResult.postValue(bggCookieJar)
            _isAuthenticating.postValue(false)
        }
    }

    fun cancel() {
        authenticationJob?.cancel()
        _isAuthenticating.value = false
    }
}
