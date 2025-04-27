@file:JvmName("NotificationUtils")

package com.boardgamegeek.extensions

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.boardgamegeek.R
import com.boardgamegeek.model.Play
import com.boardgamegeek.model.PlayUploadResult
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import com.boardgamegeek.ui.*
import com.boardgamegeek.util.LargeIconLoader
import timber.log.Timber

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
    title: CharSequence?,
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
    vararg imageUrl: String,
) {
    val loader = LargeIconLoader(this, *imageUrl, callback = object : LargeIconLoader.Callback {
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

    var info = getString(R.string.playing)
    if (location.isNotBlank()) info += " ${getString(R.string.at)} $location "
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
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
        NotificationManagerCompat.from(this).notify(tag, id, builder.build())
        return
    }
}

fun Context.notifySyncError(contentText: String, bigText: String) {
    Timber.w("$contentText\n$bigText".trim())
    if (this.preferences()[KEY_SYNC_ERRORS, false] != true) return
    val builder = this
        .createNotificationBuilder(R.string.sync_notification_title_error, NotificationChannels.ERROR)
        .setContentText(contentText)
        .setCategory(NotificationCompat.CATEGORY_ERROR)
    if (bigText.trim().isNotBlank()) {
        builder.setStyle(NotificationCompat.BigTextStyle().bigText(bigText.trim()))
    }

    notify(builder, NotificationTags.SYNC_ERROR)
}

fun Context.notifyLoggedPlay(result: PlayUploadResult) {
    if (this.preferences()[KEY_SYNC_UPLOADS, true] != true) return
    if (result.status == PlayUploadResult.Status.NO_OP) return

    val imageUrls = listOf(result.play.thumbnailUrl, result.play.heroImageUrl, result.play.imageUrl)
    val message = when {
        result.status == PlayUploadResult.Status.DELETE -> getString(R.string.msg_play_deleted)
        result.status == PlayUploadResult.Status.UPDATE -> getString(R.string.msg_play_updated)
        result.play.quantity > 0 -> getText(
            R.string.msg_play_added_quantity,
            result.numberOfPlays.asRangeDescription(result.play.quantity),
        )
        else -> getString(R.string.msg_play_added)
    }

    val loader = LargeIconLoader(this, *imageUrls.toTypedArray(), callback = object : LargeIconLoader.Callback {
        override fun onSuccessfulIconLoad(bitmap: Bitmap) {
            buildAndNotify(this@notifyLoggedPlay, result.play.gameName, message, bitmap)
        }

        override fun onFailedIconLoad() {
            buildAndNotify(this@notifyLoggedPlay, result.play.gameName, message)
        }

        fun buildAndNotify(context: Context, title: CharSequence, message: CharSequence, largeIcon: Bitmap? = null) {
            val intent = if (result.status == PlayUploadResult.Status.DELETE || result.play.internalId == INVALID_ID.toLong())
                GamePlaysActivity.createIntent(
                    context,
                    result.play.gameId,
                    result.play.gameName,
                    result.play.heroImageUrl,
                    result.play.thumbnailUrl,
                )
            else
                PlayActivity.createIntent(context, result.play.internalId)

            val builder = context.createNotificationBuilder(title, NotificationChannels.SYNC_UPLOAD, intent)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentText(message)
                .setLargeIcon(largeIcon)
                .setOnlyAlertOnce(true)
                .setGroup(NotificationTags.UPLOAD_PLAY)
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            if (result.status == PlayUploadResult.Status.NEW)
                builder.addAction(createRematchAction(context, result.play))
            context.notify(builder, NotificationTags.UPLOAD_PLAY, result.play.internalId.toInt())
            showNotificationSummary(context)
        }
    })
    loader.executeInBackground()
}

private fun createRematchAction(context: Context, play: Play): NotificationCompat.Action? {
    return if (play.internalId != INVALID_ID.toLong()) {
        val intent = LogPlayActivity.createRematchIntent(
            context,
            play.internalId,
            play.gameId,
            play.gameName,
            play.robustHeroImageUrl,
            play.gameIsCustomSorted,
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )
        NotificationCompat.Action.Builder(R.drawable.ic_baseline_replay_24, context.getString(R.string.rematch), pendingIntent).build()
    } else null
}

private fun showNotificationSummary(context: Context) {
    val builder = context.createNotificationBuilder(
        R.string.sync_notification_title_play_upload,
        NotificationChannels.SYNC_UPLOAD,
        context.intentFor<PlaysActivity>()
    )
        .setGroup(NotificationTags.UPLOAD_PLAY)
        .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
        .setGroupSummary(true)
    context.notify(builder, NotificationTags.UPLOAD_PLAY, 0)
}

object NotificationChannels {
    const val SYNC_PROGRESS = "sync"
    const val ERROR = "sync_error"
    const val SYNC_UPLOAD = "sync_upload"
    const val PLAYING = "playing"
    const val STATS = "stats"
    const val FIREBASE_MESSAGES = "firebase_messages"

    @RequiresApi(Build.VERSION_CODES.O)
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
    const val SYNC_PROGRESS = TAG_PREFIX + "SYNC_PROGRESS"
    const val SYNC_ERROR = TAG_PREFIX + "SYNC_ERROR"
    const val UPLOAD_PLAY = TAG_PREFIX + "UPLOAD_PLAY"
    const val UPLOAD_COLLECTION = TAG_PREFIX + "UPLOAD_COLLECTION"
    const val UPLOAD_COLLECTION_ERROR = TAG_PREFIX + "UPLOAD_COLLECTION_ERROR"
    const val FIREBASE_MESSAGE = TAG_PREFIX + "FIREBASE_MESSAGE"
}
