package com.boardgamegeek.auth

import android.app.Service
import timber.log.Timber
import android.content.Intent
import android.os.IBinder
import com.boardgamegeek.repository.AuthRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AuthenticationService : Service() {
    @Inject lateinit var authRepository: AuthRepository
    private var authenticator: Authenticator? = null

    override fun onCreate() {
        super.onCreate()
        Timber.v("BoardGameGeek Authentication Service started.")
        authenticator = Authenticator(this, authRepository)
    }

    override fun onDestroy() {
        Timber.v("BoardGameGeek Authentication Service stopped.")
    }

    override fun onBind(intent: Intent): IBinder? {
        Timber.v("getBinder: returning the AccountAuthenticator binder for intent %s", intent.toString())
        return authenticator?.iBinder
    }
}
