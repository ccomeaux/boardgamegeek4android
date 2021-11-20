@file:JvmName("NotificationUtils")

package com.boardgamegeek.extensions

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.boardgamegeek.R
import com.boardgamegeek.ui.PlayActivity
import com.boardgamegeek.util.LargeIconLoader
import com.boardgamegeek.util.NotificationUtils

private const val TAG_PREFIX = "com.boardgamegeek."
const val TAG_PLAY_TIMER = TAG_PREFIX + "PLAY_TIMER"

fun Context.launchPlayingNotification(
        internalId: Long,
        gameName: String,
        location: String,
        playerCount: Int,
        startTime: Long,
        thumbnailUrl: String = "",
        imageUrl: String = "",
        heroImageUrl: String = "",
) {
    val loader = LargeIconLoader(this, imageUrl, thumbnailUrl, heroImageUrl, object : LargeIconLoader.Callback {
        override fun onSuccessfulIconLoad(bitmap: Bitmap) {
            buildAndNotifyPlaying(internalId, gameName, location, playerCount, startTime, largeIcon = bitmap)
        }

        override fun onFailedIconLoad() {
            buildAndNotifyPlaying(internalId, gameName, location, playerCount, startTime)
        }
    })
    loader.executeOnMainThread()
}

private fun Context.buildAndNotifyPlaying(
        internalId: Long,
        gameName: String,
        location: String,
        playerCount: Int,
        startTime: Long,
        largeIcon: Bitmap? = null) {
    val builder = NotificationUtils.createNotificationBuilder(this, gameName, NotificationUtils.CHANNEL_ID_PLAYING)
    val intent = PlayActivity.createIntent(this, internalId).clearTop().newTask()
    val pendingIntent = PendingIntent.getActivity(this,
            0,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    )

    var info = ""
    if (location.isNotBlank()) info += "${getString(R.string.at)} $location "
    if (playerCount > 0) info += resources.getQuantityString(R.plurals.player_description, playerCount, playerCount)

    builder
            .setContentText(info.trim())
            .setLargeIcon(largeIcon)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
    if (startTime > 0) builder.setWhen(startTime).setUsesChronometer(true)
    @Suppress("DEPRECATION")
    largeIcon?.let { builder.extend(NotificationCompat.WearableExtender().setBackground(it)) }

    notify(TAG_PLAY_TIMER, internalId.toInt(), builder)
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
