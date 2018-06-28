package com.boardgamegeek.firebase

import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService

import timber.log.Timber

class BggFirebaseInstanceIDService : FirebaseInstanceIdService() {
    override fun onTokenRefresh() {
        super.onTokenRefresh()
        val refreshedToken = FirebaseInstanceId.getInstance().token ?: ""
        Timber.i("Refreshed Firebase token to $refreshedToken")
    }
}
