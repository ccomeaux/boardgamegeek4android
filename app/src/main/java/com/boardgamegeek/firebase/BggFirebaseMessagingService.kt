package com.boardgamegeek.firebase

import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.HomeActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber

class BggFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.i("Refreshed Firebase token to $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        remoteMessage.notification?.let {
            val urlString = remoteMessage.data["URL"]
            val intent = if (urlString != null) {
                Intent(Intent.ACTION_VIEW, urlString.toUri())
            } else {
                intentFor<HomeActivity>()
            }
            val message = it.body.orEmpty()
            val builder = applicationContext.createNotificationBuilder(
                it.title ?: applicationContext.getString(R.string.title_firebase_message),
                NotificationChannels.FIREBASE_MESSAGES,
                intent
            )
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            applicationContext.notify(builder, it.tag ?: NotificationTags.FIREBASE_MESSAGE)
        }
    }
}
