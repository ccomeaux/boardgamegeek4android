package com.boardgamegeek.firebase

import android.content.Intent
import android.net.Uri
import android.support.v4.app.NotificationCompat
import com.boardgamegeek.R
import com.boardgamegeek.ui.HomeActivity
import com.boardgamegeek.util.NotificationUtils
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.jetbrains.anko.intentFor

class BggFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        super.onMessageReceived(remoteMessage)
        remoteMessage?.notification?.let {
            val urlString = remoteMessage.data?.get("URL")
            val intent = if (urlString != null) {
                Intent(Intent.ACTION_VIEW, Uri.parse(urlString))
            } else {
                intentFor<HomeActivity>()
            }
            val message = it.body ?: ""
            val builder = NotificationUtils.createNotificationBuilder(applicationContext,
                    it.title ?: applicationContext.getString(R.string.title_firebase_message),
                    NotificationUtils.CHANNEL_ID_FIREBASE_MESSAGES,
                    intent)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setContentText(message)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            NotificationUtils.notify(applicationContext, it.tag ?: NotificationUtils.TAG_FIREBASE_MESSAGE, 0, builder)
        }
    }
}
