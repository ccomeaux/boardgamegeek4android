package com.boardgamegeek.service

import android.content.Intent
import android.content.SyncResult
import android.graphics.Bitmap
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.Action
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.util.LargeIconLoader
import com.boardgamegeek.util.LargeIconLoader.Callback
import timber.log.Timber

abstract class SyncUploadTask(application: BggApplication, syncResult: SyncResult) : SyncTask(application, syncResult) {
    private val notificationMessages = ArrayList<CharSequence>()

    @get:StringRes
    protected abstract val notificationTitleResId: Int

    protected abstract val notificationSummaryIntent: Intent

    protected open val notificationIntent: Intent?
        get() = notificationSummaryIntent

    protected abstract val notificationMessageTag: String

    protected abstract val notificationErrorTag: String

    @get:PluralsRes
    protected abstract val summarySuffixResId: Int

    protected fun notifyUser(title: CharSequence, message: CharSequence, id: Int, vararg imageUrl: String) {
        if (prefs[KEY_SYNC_UPLOADS, true] != true) return

        notificationMessages.add(context.getText(R.string.msg_play_upload, title, message))

        val loader = LargeIconLoader(context, *imageUrl, callback = object : Callback {
            override fun onSuccessfulIconLoad(bitmap: Bitmap) {
                buildAndNotify(title, message, id, bitmap)
            }

            override fun onFailedIconLoad() {
                buildAndNotify(title, message, id)
            }
        })
        loader.executeInBackground()
    }

    private fun buildAndNotify(title: CharSequence, message: CharSequence, id: Int, largeIcon: Bitmap? = null) {
        val builder = context.createNotificationBuilder(
            notificationTitleResId,
            NotificationChannels.SYNC_UPLOAD,
            notificationIntent
        )
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentTitle(title)
            .setContentText(message)
            .setLargeIcon(largeIcon)
            .setOnlyAlertOnce(true)
            .setGroup(notificationMessageTag)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
        val action = createMessageAction()
        if (action != null) {
            builder.addAction(action)
        }
        context.notify(builder, notificationMessageTag, id)
        showNotificationSummary()
    }

    private fun showNotificationSummary() {
        val builder = context.createNotificationBuilder(
            notificationTitleResId,
            NotificationChannels.SYNC_UPLOAD,
            notificationSummaryIntent
        )
            .setGroup(notificationMessageTag)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setGroupSummary(true)
        when (val messageCount = notificationMessages.size) {
            0 -> return
            1 -> builder.setContentText(notificationMessages[0])
            else -> {
                val message = context.resources.getQuantityString(summarySuffixResId, notificationMessages.size, notificationMessages.size)
                builder.setContentText(message)
                val detail = NotificationCompat.InboxStyle(builder).setSummaryText(message)
                for (i in messageCount - 1 downTo 0) {
                    detail.addLine(notificationMessages[i])
                }
            }
        }
        context.notify(builder, notificationMessageTag)
    }

    protected open fun createMessageAction(): Action? {
        return null
    }

    protected fun notifyUploadError(errorMessage: CharSequence) {
        if (errorMessage.isBlank()) return
        Timber.e(errorMessage.toString())
        val builder = context.createNotificationBuilder(notificationTitleResId, NotificationChannels.ERROR, notificationSummaryIntent)
            .setContentText(errorMessage)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
        val detail = NotificationCompat.BigTextStyle(builder)
        detail.bigText(errorMessage)
        context.notify(builder, notificationErrorTag)
    }
}
