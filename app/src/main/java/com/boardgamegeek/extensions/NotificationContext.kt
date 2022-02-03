@file:JvmName("NotificationUtils")

package com.boardgamegeek.extensions

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.boardgamegeek.R
import com.boardgamegeek.ui.HomeActivity
import com.boardgamegeek.ui.PlayActivity
import com.boardgamegeek.util.LargeIconLoader

private const val TAG_PREFIX = "com.boardgamegeek."
const val TAG_PLAY_TIMER = TAG_PREFIX + "PLAY_TIMER"

/**
 * Creates a [androidx.core.app.NotificationCompat.Builder] with the correct icons, specified title, and pending intent that goes to the [com.boardgamegeek.ui.HomeActivity].
 */
fun Context.createNotificationBuilder(
    @StringRes titleResId: Int,
    channelId: String,
    cls: Class<*>? = HomeActivity::class.java
): NotificationCompat.Builder {
    return createNotificationBuilder(getString(titleResId), channelId, cls)
}

/**
 * Creates a [androidx.core.app.NotificationCompat.Builder] with the correct icons, specified title, and pending intent that goes to the [com.boardgamegeek.ui.HomeActivity].
 */
fun Context.createNotificationBuilder(title: String?, channelId: String, cls: Class<*>? = HomeActivity::class.java): NotificationCompat.Builder {
    return createNotificationBuilder(title, channelId, Intent(this, cls))
}

/**
 * Creates a [NotificationCompat.Builder] with the correct icons, specified title, and pending intent.
 */
fun Context.createNotificationBuilder(@StringRes titleResId: Int, channelId: String, intent: Intent?): NotificationCompat.Builder {
    return createNotificationBuilder(getString(titleResId), channelId, intent)
}

/**
 * Creates a [NotificationCompat.Builder] with the correct icons, specified title, and pending intent.
 */
fun Context.createNotificationBuilder(
    title: String?,
    channelId: String,
    intent: Intent?
): NotificationCompat.Builder {
    val builder = NotificationCompat.Builder(this, channelId)
        .setSmallIcon(R.drawable.ic_stat_bgg)
        .setColor(ContextCompat.getColor(this, R.color.primary))
        .setContentTitle(title)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    val resultPendingIntent = PendingIntent.getActivity(this, 0, intent, flags)
    builder.setContentIntent(resultPendingIntent)
    return builder
}

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
    largeIcon: Bitmap? = null
) {
    val builder = createNotificationBuilder(gameName, NotificationChannels.PLAYING)
    val intent = PlayActivity.createIntent(this, internalId).clearTop().newTask()
    val pendingIntent = PendingIntent.getActivity(
        this,
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

    notify(builder, TAG_PLAY_TIMER, internalId.toInt())
}

/**
 * Cancel the notification by a unique ID.
 */
fun Context.cancelNotification(tag: String?, id: Long = 0L) {
    NotificationManagerCompat.from(this).cancel(tag, id.toInt())
}

/**
 * Display the notification with a unique ID.
 */
fun Context.notify(builder: NotificationCompat.Builder, tag: String?, id: Int = 0) {
    NotificationManagerCompat.from(this).notify(tag, id, builder.build())
}

object NotificationChannels {
    const val SYNC_PROGRESS = "sync"
    const val ERROR = "sync_error"
    const val SYNC_UPLOAD = "sync_upload"
    const val PLAYING = "playing"
    const val STATS = "stats"
    const val FIREBASE_MESSAGES = "firebase_messages"

    @TargetApi(Build.VERSION_CODES.O)
    fun create(context: Context?) {
        val notificationManager = context?.getSystemService<NotificationManager>() ?: return

        notificationManager.createNotificationChannel(
            NotificationChannel(
                SYNC_PROGRESS,
                context.getString(R.string.channel_name_sync_progress),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.channel_description_sync_progress)
            }
        )

        notificationManager.createNotificationChannel(
            NotificationChannel(
                SYNC_UPLOAD,
                context.getString(R.string.channel_name_sync_upload),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.channel_description_sync_upload)
            })

        notificationManager.createNotificationChannel(
            NotificationChannel(
                ERROR,
                context.getString(R.string.channel_name_sync_error),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.channel_description_sync_error)
                lightColor = Color.RED
            }
        )

        notificationManager.createNotificationChannel(
            NotificationChannel(
                PLAYING,
                context.getString(R.string.channel_name_playing),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.channel_description_playing)
                lightColor = Color.BLUE
            }
        )

        notificationManager.createNotificationChannel(
            NotificationChannel(
                STATS,
                context.getString(R.string.channel_name_stats),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.channel_description_stats)
            }
        )

        notificationManager.createNotificationChannel(
            NotificationChannel(
                FIREBASE_MESSAGES,
                context.getString(R.string.channel_name_firebase_messages),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_description_firebase_messages)
            }
        )
    }
}

object NotificationTags {
    private const val TAG_PREFIX = "com.boardgamegeek."
    const val PLAY_STATS = TAG_PREFIX + "PLAY_STATS"
    const val PROVIDER_ERROR = TAG_PREFIX + "PROVIDER_ERROR"
    const val SYNC_PROGRESS = TAG_PREFIX + "SYNC_PROGRESS"
    const val SYNC_ERROR = TAG_PREFIX + "SYNC_ERROR"
    const val UPLOAD_PLAY = TAG_PREFIX + "UPLOAD_PLAY"
    const val UPLOAD_PLAY_ERROR = TAG_PREFIX + "UPLOAD_PLAY_ERROR"
    const val UPLOAD_COLLECTION = TAG_PREFIX + "UPLOAD_COLLECTION"
    const val UPLOAD_COLLECTION_ERROR = TAG_PREFIX + "UPLOAD_COLLECTION_ERROR"
    const val FIREBASE_MESSAGE = TAG_PREFIX + "FIREBASE_MESSAGE"
}
