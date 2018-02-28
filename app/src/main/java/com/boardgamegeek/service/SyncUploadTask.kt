package com.boardgamegeek.service

import android.content.Context
import android.content.Intent
import android.content.SyncResult
import android.graphics.Bitmap
import android.os.Build
import android.support.annotation.StringRes
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationCompat.Action
import android.text.TextUtils
import com.boardgamegeek.R
import com.boardgamegeek.io.BggService
import com.boardgamegeek.util.LargeIconLoader
import com.boardgamegeek.util.LargeIconLoader.Callback
import com.boardgamegeek.util.NotificationUtils
import com.boardgamegeek.util.PreferencesUtils
import com.boardgamegeek.util.PresentationUtils
import hugo.weaving.DebugLog
import timber.log.Timber
import java.util.*

abstract class SyncUploadTask @DebugLog
constructor(context: Context, service: BggService, syncResult: SyncResult) : SyncTask(context, service, syncResult) {
    private val notificationMessages = ArrayList<CharSequence>()

    @get:StringRes
    protected abstract val notificationTitleResId: Int

    protected abstract val notificationSummaryIntent: Intent

    protected open val notificationIntent: Intent?
        get() = notificationSummaryIntent

    protected abstract val notificationMessageTag: String

    protected abstract val notificationErrorTag: String

    @DebugLog
    protected fun notifyUser(title: CharSequence, message: CharSequence, id: Int, imageUrl: String, thumbnailUrl: String) {
        if (!PreferencesUtils.getPlayUploadNotifications(context)) return

        val loader = LargeIconLoader(context, imageUrl, thumbnailUrl, object : Callback {
            override fun onSuccessfulIconLoad(bitmap: Bitmap) {
                buildAndNotify(title, message, id, bitmap)
            }

            override fun onFailedIconLoad() {
                buildAndNotify(title, message, id, null)
            }
        })
        loader.executeInBackground()

        notificationMessages.add(PresentationUtils.getText(context, R.string.msg_play_upload, title, message))
        showNotificationSummary()
    }

    private fun buildAndNotify(title: CharSequence, message: CharSequence, id: Int, largeIcon: Bitmap?) {
        val builder = NotificationUtils
                .createNotificationBuilder(context,
                        notificationTitleResId,
                        NotificationUtils.CHANNEL_ID_SYNC_UPLOAD,
                        notificationIntent)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentTitle(title)
                .setContentText(message)
                .setLargeIcon(largeIcon)
                .setOnlyAlertOnce(true)
                .setGroup(notificationMessageTag)
        val detail = NotificationCompat.BigTextStyle(builder)
        detail.bigText(message)
        val action = createMessageAction()
        if (action != null) {
            builder.addAction(action)
        }
        if (largeIcon != null) {
            builder.extend(NotificationCompat.WearableExtender().setBackground(largeIcon))
        }
        NotificationUtils.notify(context, notificationMessageTag, id, builder)
    }

    @DebugLog
    private fun showNotificationSummary() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) return
        val builder = NotificationUtils
                .createNotificationBuilder(context,
                        notificationTitleResId,
                        NotificationUtils.CHANNEL_ID_SYNC_UPLOAD,
                        notificationSummaryIntent)
                .setGroup(notificationMessageTag)
                .setGroupSummary(true)
        val messageCount = notificationMessages.size
        if (messageCount == 1) {
            builder.setContentText(notificationMessages[0])
        } else {
            val detail = NotificationCompat.InboxStyle(builder)
            for (i in messageCount - 1 downTo 0) {
                detail.addLine(notificationMessages[i])
            }
        }
        NotificationUtils.notify(context, notificationMessageTag, 0, builder)
    }

    @DebugLog
    protected open fun createMessageAction(): Action? {
        return null
    }

    @DebugLog
    protected fun notifyUploadError(errorMessage: CharSequence) {
        if (TextUtils.isEmpty(errorMessage)) return
        Timber.e(errorMessage.toString())
        val builder = NotificationUtils
                .createNotificationBuilder(context,
                        notificationTitleResId,
                        NotificationUtils.CHANNEL_ID_ERROR, notificationSummaryIntent)
                .setContentText(errorMessage)
                .setCategory(NotificationCompat.CATEGORY_ERROR)
        val detail = NotificationCompat.BigTextStyle(builder)
        detail.bigText(errorMessage)
        NotificationUtils.notify(context, notificationErrorTag, 0, builder)
    }
}
