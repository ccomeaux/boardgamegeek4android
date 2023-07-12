package com.boardgamegeek.work

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import com.boardgamegeek.R
import com.boardgamegeek.extensions.NotificationChannels
import java.util.UUID

const val NOTIFICATION_ID_COLLECTION = 41
const val NOTIFICATION_ID_PLAYS = 42
const val NOTIFICATION_ID_USERS = 43

fun Context.createForegroundInfo(titleResId: Int, notificationId: Int, id: UUID, contentText: String): ForegroundInfo {
    val notification = NotificationCompat.Builder(applicationContext, NotificationChannels.SYNC_PROGRESS)
        .setContentTitle(applicationContext.getString(titleResId))
        .setTicker(applicationContext.getString(titleResId))
        .setContentText(contentText)
        .setSmallIcon(R.drawable.ic_stat_bgg)
        .setColor(ContextCompat.getColor(applicationContext, R.color.primary))
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .setOngoing(true)
        .setProgress(1, 0, true)
        .addAction(
            R.drawable.ic_baseline_clear_24,
            applicationContext.getString(R.string.cancel),
            WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)
        )
        .build()

    return ForegroundInfo(notificationId, notification)
}
