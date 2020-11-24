package com.boardgamegeek.extensions

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.boardgamegeek.R
import com.boardgamegeek.ui.PlayActivity
import com.boardgamegeek.util.LargeIconLoader
import com.boardgamegeek.util.NotificationUtils
import org.jetbrains.anko.clearTop
import org.jetbrains.anko.newTask

fun Context.launchPlayingNotification(
        internalId: Long,
        gameId: Int,
        gameName: String,
        location: String,
        playerCount: Int,
        startTime: Long,
        thumbnailUrl: String = "",
        imageUrl: String = "",
        heroImageUrl: String = "",
        customPlayerSort: Boolean = false,
) {
    val loader = LargeIconLoader(this, imageUrl, thumbnailUrl, heroImageUrl, object : LargeIconLoader.Callback {
        override fun onSuccessfulIconLoad(bitmap: Bitmap) {
            buildAndNotifyPlaying(internalId, gameId, gameName, location, playerCount, startTime, thumbnailUrl, imageUrl, heroImageUrl, customPlayerSort, largeIcon = bitmap)
        }

        override fun onFailedIconLoad() {
            buildAndNotifyPlaying(internalId, gameId, gameName, location, playerCount, startTime, thumbnailUrl, imageUrl, heroImageUrl,customPlayerSort)
        }
    })
    loader.executeOnMainThread()
}

private fun Context.buildAndNotifyPlaying(
        internalId: Long,
        gameId: Int,
        gameName: String,
        location: String,
        playerCount: Int,
        startTime: Long,
        thumbnailUrl: String,
        imageUrl: String,
        heroImageUrl: String,
        customPlayerSort: Boolean,
        largeIcon: Bitmap? = null) {
    val builder = NotificationUtils.createNotificationBuilder(this, gameName, NotificationUtils.CHANNEL_ID_PLAYING)
    val intent = PlayActivity.createIntent(this, internalId, gameId, gameName, thumbnailUrl, imageUrl, heroImageUrl, customPlayerSort)
            .clearTop().newTask()
    val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)

    var info = ""
    if (location.isNotBlank()) info += "${getString(R.string.at)} $location "
    if (playerCount > 0) info += resources.getQuantityString(R.plurals.player_description, playerCount, playerCount)

    builder
            .setContentText(info.trim())
            .setLargeIcon(largeIcon)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
    if (startTime > 0) builder.setWhen(startTime).setUsesChronometer(true)
    largeIcon?.let { builder.extend(NotificationCompat.WearableExtender().setBackground(it)) }

    notify(NotificationUtils.TAG_PLAY_TIMER, internalId.toInt(), builder)
}

/**
 * Cancel the notification by a unique ID.
 */
fun Context.cancel(tag: String?, id: Long) {
    NotificationManagerCompat.from(this).cancel(tag, id.toInt())
}

private fun Context.notify(tag: String?, id: Int, builder: NotificationCompat.Builder) {
    NotificationManagerCompat.from(this).notify(tag, id, builder.build())
}
