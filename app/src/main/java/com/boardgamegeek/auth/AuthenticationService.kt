package com.boardgamegeek.auth

import android.app.Service
import timber.log.Timber
import android.content.Intent
import android.os.IBinder

class AuthenticationService : Service() {
    private var authenticator: Authenticator? = null

    override fun onCreate() {
        Timber.v("BoardGameGeek Authentication Service started.")
        authenticator = Authenticator(this)
    }

    override fun onDestroy() {
        Timber.v("BoardGameGeek Authentication Service stopped.")
    }

    override fun onBind(intent: Intent): IBinder? {
        Timber.v("getBinder: returning the AccountAuthenticator binder for intent %s", intent.toString())
        return authenticator?.iBinder
    }
}
