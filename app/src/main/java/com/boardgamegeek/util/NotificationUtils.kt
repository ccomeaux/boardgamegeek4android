package com.boardgamegeek.util

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.boardgamegeek.R
import com.boardgamegeek.ui.HomeActivity

object NotificationUtils {
    const val CHANNEL_ID_SYNC_PROGRESS = "sync"
    const val CHANNEL_ID_ERROR = "sync_error"
    const val CHANNEL_ID_SYNC_UPLOAD = "sync_upload"
    const val CHANNEL_ID_PLAYING = "playing"
    const val CHANNEL_ID_STATS = "stats"
    const val CHANNEL_ID_FIREBASE_MESSAGES = "firebase_messages"
    private const val TAG_PREFIX = "com.boardgamegeek."
    const val TAG_PLAY_STATS = TAG_PREFIX + "PLAY_STATS"
    const val TAG_PROVIDER_ERROR = TAG_PREFIX + "PROVIDER_ERROR"
    const val TAG_SYNC_PROGRESS = TAG_PREFIX + "SYNC_PROGRESS"
    const val TAG_SYNC_ERROR = TAG_PREFIX + "SYNC_ERROR"
    const val TAG_UPLOAD_PLAY = TAG_PREFIX + "UPLOAD_PLAY"
    const val TAG_UPLOAD_PLAY_ERROR = TAG_PREFIX + "UPLOAD_PLAY_ERROR"
    const val TAG_UPLOAD_COLLECTION = TAG_PREFIX + "UPLOAD_COLLECTION"
    const val TAG_UPLOAD_COLLECTION_ERROR = TAG_PREFIX + "UPLOAD_COLLECTION_ERROR"
    const val TAG_FIREBASE_MESSAGE = TAG_PREFIX + "FIREBASE_MESSAGE"

    @TargetApi(VERSION_CODES.O)
    fun createNotificationChannels(context: Context?) {
        val notificationManager = context?.getSystemService<NotificationManager>() ?: return

        notificationManager.createNotificationChannel(
                NotificationChannel(
                        CHANNEL_ID_SYNC_PROGRESS,
                        context.getString(R.string.channel_name_sync_progress),
                        NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = context.getString(R.string.channel_description_sync_progress)
                }
        )

        notificationManager.createNotificationChannel(NotificationChannel(
                CHANNEL_ID_SYNC_UPLOAD,
                context.getString(R.string.channel_name_sync_upload),
                NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = context.getString(R.string.channel_description_sync_upload)
        })

        notificationManager.createNotificationChannel(
                NotificationChannel(
                        CHANNEL_ID_ERROR,
                        context.getString(R.string.channel_name_sync_error),
                        NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(R.string.channel_description_sync_error)
                    lightColor = Color.RED
                }
        )

        notificationManager.createNotificationChannel(
                NotificationChannel(
                        CHANNEL_ID_PLAYING,
                        context.getString(R.string.channel_name_playing),
                        NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(R.string.channel_description_playing)
                    lightColor = Color.BLUE
                }
        )

        notificationManager.createNotificationChannel(
                NotificationChannel(
                        CHANNEL_ID_STATS,
                        context.getString(R.string.channel_name_stats),
                        NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(R.string.channel_description_stats)
                }
        )

        notificationManager.createNotificationChannel(
                NotificationChannel(
                        CHANNEL_ID_FIREBASE_MESSAGES,
                        context.getString(R.string.channel_name_firebase_messages),
                        NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.channel_description_firebase_messages)
                }
        )
    }

    /**
     * Creates a [androidx.core.app.NotificationCompat.Builder] with the correct icons, specified title, and pending intent that goes to the [com.boardgamegeek.ui.HomeActivity].
     */
    fun createNotificationBuilder(context: Context, @StringRes titleResId: Int, channelId: String?, cls: Class<*>? = HomeActivity::class.java): NotificationCompat.Builder {
        return createNotificationBuilder(context, context.getString(titleResId), channelId, cls)
    }

    /**
     * Creates a [androidx.core.app.NotificationCompat.Builder] with the correct icons, specified title, and pending intent that goes to the [com.boardgamegeek.ui.HomeActivity].
     */
    fun createNotificationBuilder(context: Context, title: String?, channelId: String?, cls: Class<*>? = HomeActivity::class.java): NotificationCompat.Builder {
        return createNotificationBuilder(context, title, channelId, Intent(context, cls))
    }

    /**
     * Creates a [NotificationCompat.Builder] with the correct icons, specified title, and pending intent.
     */
    fun createNotificationBuilder(context: Context, @StringRes titleResId: Int, channelId: String?, intent: Intent?): NotificationCompat.Builder {
        return createNotificationBuilder(context, context.getString(titleResId), channelId, intent)
    }

    /**
     * Creates a [NotificationCompat.Builder] with the correct icons, specified title, and pending intent.
     */
    fun createNotificationBuilder(
            context: Context,
            title: String?,
            channelId: String?,
            intent: Intent?
    ): NotificationCompat.Builder {
        val builder = NotificationCompat.Builder(context, channelId!!)
                .setSmallIcon(R.drawable.ic_stat_bgg)
                .setColor(ContextCompat.getColor(context, R.color.primary))
                .setContentTitle(title)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (VERSION.SDK_INT >= VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val resultPendingIntent = PendingIntent.getActivity(context, 0, intent, flags)
        builder.setContentIntent(resultPendingIntent)
        return builder
    }

    /**
     * Display the notification with a unique ID.
     */
    fun notify(context: Context, tag: String?, id: Int, builder: NotificationCompat.Builder) {
        NotificationManagerCompat.from(context).notify(tag, id, builder.build())
    }

    /**
     * Cancel the notification by a unique ID.
     */
    /**
     * Cancel the notification by a unique ID.
     */
    fun cancel(context: Context, tag: String?, id: Long = 0L) {
        NotificationManagerCompat.from(context).cancel(tag, getIntegerId(id))
    }

    fun getIntegerId(id: Long): Int = if (id < Int.MAX_VALUE) id.toInt() else (id % Int.MAX_VALUE).toInt()
}
