package com.boardgamegeek.service

import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.content.SyncResult
import android.os.Build
import androidx.annotation.PluralsRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.BigTextStyle
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.BggService
import com.boardgamegeek.pref.SyncPrefs
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber
import java.util.concurrent.TimeUnit

abstract class SyncTask(protected val application: BggApplication, protected val service: BggService, protected val syncResult: SyncResult) {
    protected val context = application.applicationContext!!
    protected val prefs: SharedPreferences by lazy { context.preferences() }
    protected val syncPrefs: SharedPreferences by lazy { SyncPrefs.getPrefs(context) }

    /**
     * Returns whether this task has been cancelled. It may still be running, but will stop soon.
     */
    var isCancelled = false
        private set

    /**
     * Unique ID for this sync class.
     */
    abstract val syncType: Int

    /***
     * The resource ID of the context text to display in syncing progress and error notifications. It should describe
     * the entire task.
     */
    open val notificationSummaryMessageId: Int
        get() = NO_NOTIFICATION

    /**
     * Perform the sync operation.
     */
    abstract fun execute()

    /**
     * Call this to cancel the task. If the task is running, it will cancel it's process at the earliest convenient
     * time, as determined by the service.
     */
    fun cancel() {
        isCancelled = true
    }

    protected fun updateProgressNotificationAsPlural(@PluralsRes detailResId: Int, quantity: Int, vararg formatArgs: Any) {
        updateProgressNotification(application.resources.getQuantityString(detailResId, quantity, *formatArgs))
    }

    /**
     * If the user's preferences are set, show a notification with the current progress of the sync status. The content
     * text is set by the sync task, while the detail message is displayed in BigTextStyle.
     */
    fun updateProgressNotification(detail: String? = null) {
        Timber.i(detail)
        FirebaseCrashlytics.getInstance().setCustomKey(CrashKeys.SYNC_DETAIL, detail ?: "")
        if (prefs[KEY_SYNC_NOTIFICATIONS, false] != true) return

        val message = if (notificationSummaryMessageId == NO_NOTIFICATION) "" else context.getString(notificationSummaryMessageId)

        val intent = Intent(context, CancelReceiver::class.java)
        intent.action = SyncService.ACTION_CANCEL_SYNC
        val cancelIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )
        val builder = context
            .createNotificationBuilder(R.string.sync_notification_title, NotificationChannels.SYNC_PROGRESS)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setProgress(1, 0, true)
            .addAction(R.drawable.ic_baseline_clear_24, context.getString(R.string.cancel), cancelIntent)
        if (!detail.isNullOrBlank()) {
            builder.setStyle(BigTextStyle().bigText(detail))
        }
        context.notify(builder, NotificationTags.SYNC_PROGRESS)
    }

    /**
     * If the user's preferences are set, show a notification message with the error message. This will replace any
     * existing error notification.
     */
    protected fun showError(detailMessage: String, t: Throwable) {
        showError(detailMessage, t.localizedMessage ?: t.toString())
    }

    /**
     * If the user's preferences are set, show a notification message with the error message. This will replace any
     * existing error notification.
     */
    protected fun showError(detailMessage: String, httpCode: Int) {
        showError(detailMessage, httpCode.asHttpErrorMessage(context))
    }

    /**
     * If the user's preferences are set, show a notification message with the error message. This will replace any
     * existing error notification.
     */
    fun showError(detailMessage: String, errorMessage: String) {
        Timber.w("$detailMessage\n$errorMessage".trim())

        if (prefs[KEY_SYNC_ERRORS, false] != true) return

        val contentMessage = if (notificationSummaryMessageId == NO_NOTIFICATION) detailMessage
        else context.getString(notificationSummaryMessageId)

        val bigText = if (notificationSummaryMessageId == NO_NOTIFICATION) errorMessage
        else (detailMessage + "\n" + errorMessage).trim()

        val builder = context
            .createNotificationBuilder(R.string.sync_notification_title_error, NotificationChannels.ERROR)
            .setContentText(contentMessage)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
        if (bigText.isNotBlank()) {
            builder.setStyle(BigTextStyle().bigText(bigText))
        }

        context.notify(builder, NotificationTags.SYNC_ERROR)
    }

    /**
     * Sleep for the specified number of milliseconds. Returns true if thread was interrupted. This typically means the
     * task should stop processing.
     */
    protected fun wasSleepInterrupted(duration: Long, timeUnit: TimeUnit = TimeUnit.MILLISECONDS, showNotification: Boolean = true): Boolean {
        try {
            Timber.d("Sleeping for %,d millis", timeUnit.toMillis(duration))
            if (showNotification) {
                val durationSeconds = timeUnit.toSeconds(duration).toInt()
                updateProgressNotification(
                    context.resources.getQuantityString(
                        R.plurals.sync_notification_collection_sleeping,
                        durationSeconds,
                        durationSeconds
                    )
                )
            }
            timeUnit.sleep(duration)
        } catch (e: InterruptedException) {
            Timber.w(e, "Sleeping interrupted during sync.")
            context.cancelNotification(NotificationTags.SYNC_PROGRESS)
            return true
        }

        return false
    }

    companion object {
        const val NO_NOTIFICATION = 0
    }

    object CrashKeys {
        const val SYNC_DETAIL = "SYNC_DETAIL"
    }
}
