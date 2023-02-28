package com.boardgamegeek.service

import android.accounts.Account
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import com.boardgamegeek.BggApplication
import com.boardgamegeek.BuildConfig
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.pref.setCurrentTimestamp
import com.boardgamegeek.repository.CollectionItemRepository
import com.boardgamegeek.repository.GameRepository
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.repository.UserRepository
import com.boardgamegeek.util.HttpUtils
import com.boardgamegeek.util.RemoteConfig
import com.google.firebase.crashlytics.FirebaseCrashlytics
import okhttp3.Request
import timber.log.Timber
import java.io.IOException
import java.net.SocketTimeoutException

class SyncAdapter(
    private val application: BggApplication,
    private val collectionItemRepository: CollectionItemRepository,
    private val gameRepository: GameRepository,
    private val playRepository: PlayRepository,
    private val userRepository: UserRepository,
) : AbstractThreadedSyncAdapter(application.applicationContext, false) {
    private var currentTask: SyncTask? = null
    private var isCancelled = false
    private val cancelReceiver = CancelReceiver()
    private val syncPrefs: SharedPreferences = SyncPrefs.getPrefs(application)
    private val prefs: SharedPreferences by lazy { context.preferences() }

    init {
        if (!BuildConfig.DEBUG) {
            Thread.setDefaultUncaughtExceptionHandler { _: Thread?, throwable: Throwable? ->
                Timber.e(throwable, "Uncaught sync exception, suppressing UI in release build.")
            }
        }
        application.registerReceiver(cancelReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    internal object CrashKeys {
        const val SYNC_TYPES = "SYNC_TYPES"
        const val SYNC_TYPE = "SYNC_TYPE"
        const val SYNC_SETTINGS = "SYNC_SETTINGS"
    }

    /**
     * Perform a sync. This builds a list of sync tasks from the types specified in the `extras bundle`, iterating
     * over each. It posts and removes a `SyncEvent` with the type of sync task. As well as showing the progress
     * in a notification.
     */
    override fun onPerformSync(account: Account, extras: Bundle, authority: String, provider: ContentProviderClient, syncResult: SyncResult) {
        RemoteConfig.fetch()

        isCancelled = false
        val uploadOnly = extras.getBoolean(ContentResolver.SYNC_EXTRAS_UPLOAD, false)
        val manualSync = extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false)
        val initialize = extras.getBoolean(ContentResolver.SYNC_EXTRAS_INITIALIZE, false)
        val type = extras.getInt(SyncService.EXTRA_SYNC_TYPE, SyncService.FLAG_SYNC_ALL)
        Timber.i("Beginning sync for account ${account.name}, uploadOnly=$uploadOnly manualSync=$manualSync initialize=$initialize, type=$type")

        FirebaseCrashlytics.getInstance().setCustomKey(CrashKeys.SYNC_TYPES, type)

        var statuses = listOf(prefs.getSyncStatusesOrDefault()).formatList()
        if (prefs.getBoolean(PREFERENCES_KEY_SYNC_PLAYS, false)) statuses += " | plays"
        if (prefs.getBoolean(PREFERENCES_KEY_SYNC_BUDDIES, false)) statuses += " | buddies"
        FirebaseCrashlytics.getInstance().setCustomKey(CrashKeys.SYNC_SETTINGS, statuses)

        if (initialize) {
            ContentResolver.setIsSyncable(account, authority, 1)
            ContentResolver.setSyncAutomatically(account, authority, true)
            ContentResolver.addPeriodicSync(account, authority, Bundle(), (24 * 60 * 60).toLong()) // 24 hours
        }
        if (!shouldContinueSync()) {
            finishSync()
            return
        }
        toggleCancelReceiver(true)
        val tasks = createTasks(application, type, uploadOnly, syncResult, account)
        for (i in tasks.indices) {
            if (isCancelled) {
                Timber.i("Cancelling all sync tasks")
                notifySyncIsCancelled(currentTask?.notificationSummaryMessageId ?: SyncTask.NO_NOTIFICATION)
                break
            }
            currentTask = tasks[i]
            try {
                syncPrefs.setCurrentTimestamp()
                currentTask?.let {
                    FirebaseCrashlytics.getInstance().setCustomKey(CrashKeys.SYNC_TYPE, it.syncType)
                    it.updateProgressNotification()
                    it.execute()
                }
                if (currentTask?.isCancelled == true) {
                    Timber.i("Sync task %s has requested the sync operation to be cancelled", currentTask)
                    break
                }
            } catch (e: Exception) {
                Timber.e(e, "Syncing %s", currentTask)
                syncResult.stats.numIoExceptions += 10
                showException(currentTask, e)
                if (e.cause is SocketTimeoutException) {
                    break
                }
            }
        }
        finishSync()
    }

    private fun finishSync() {
        context.cancelNotification(NotificationTags.SYNC_PROGRESS)
        toggleCancelReceiver(false)
        syncPrefs.setCurrentTimestamp(0L)
        try {
            context.unregisterReceiver(cancelReceiver)
        } catch (e: Exception) {
            Timber.w(e)
        }
    }

    /**
     * Indicates that a sync operation has been canceled.
     */
    override fun onSyncCanceled() {
        super.onSyncCanceled()
        Timber.i("Sync cancel requested.")
        isCancelled = true
        currentTask?.cancel()
    }

    /**
     * Determine if the sync should continue based on the current state of the device.
     */
    private fun shouldContinueSync(): Boolean {
        if (context.isOffline()) {
            Timber.i("Skipping sync; offline")
            return false
        }
        if (prefs.getSyncOnlyCharging() && !context.isCharging()) {
            Timber.i("Skipping sync; not charging")
            return false
        }
        if (prefs.getSyncOnlyWifi() && !context.isOnWiFi()) {
            Timber.i("Skipping sync; not on wifi")
            return false
        }
        if (context.isBatteryLow()) {
            Timber.i("Skipping sync; battery low")
            return false
        }
        if (!RemoteConfig.getBoolean(RemoteConfig.KEY_SYNC_ENABLED)) {
            Timber.i("Sync disabled remotely")
            return false
        }
        if (hasPrivacyError()) {
            Timber.i("User still hasn't accepted the new privacy policy.")
            return false
        }
        return true
    }

    private fun hasPrivacyError(): Boolean {
        val weeksToCompare = RemoteConfig.getInt(RemoteConfig.KEY_PRIVACY_CHECK_WEEKS)
        val weeks = prefs.getLastPrivacyCheckTimestamp().howManyWeeksOld()
        if (weeks < weeksToCompare) {
            Timber.i("We checked the privacy statement less than %,d weeks ago; skipping", weeksToCompare)
            return false
        }
        val httpClient = HttpUtils.getHttpClientWithAuth(context)
        val url = "https://www.boardgamegeek.com"
        val request: Request = Request.Builder().url(url).build()
        return try {
            val response = httpClient.newCall(request).execute()
            val body = response.body
            val content = body?.string()?.trim().orEmpty()
            if (content.contains("Please update your privacy and marketing preferences")) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
                )
                val message = context.getString(R.string.sync_notification_message_privacy_error)
                val builder = context
                    .createNotificationBuilder(R.string.sync_notification_title_error, NotificationChannels.ERROR)
                    .setContentText(message)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                    .setContentIntent(pendingIntent)
                    .setCategory(NotificationCompat.CATEGORY_ERROR)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                context.notify(builder, NotificationTags.SYNC_ERROR, Int.MAX_VALUE)
                true
            } else {
                prefs.setLastPrivacyCheckTimestamp()
                false
            }
        } catch (e: IOException) {
            Timber.w(e)
            true
        }
    }

    /**
     * Create a list of sync tasks based on the specified type.
     */
    private fun createTasks(
        application: BggApplication,
        typeList: Int,
        uploadOnly: Boolean,
        syncResult: SyncResult,
        account: Account
    ): List<SyncTask> {
        val tasks = mutableListOf<SyncTask>()
        if (shouldCreateTask(typeList, SyncService.FLAG_SYNC_COLLECTION_UPLOAD)) {
            tasks.add(SyncCollectionUpload(application, syncResult))
        }
        if (shouldCreateTask(typeList, SyncService.FLAG_SYNC_COLLECTION_DOWNLOAD) && !uploadOnly) {
            tasks.add(SyncCollectionComplete(application, syncResult, collectionItemRepository))
            tasks.add(SyncCollectionModifiedSince(application, syncResult, collectionItemRepository))
            tasks.add(SyncCollectionUnupdated(application, syncResult, collectionItemRepository))
        }
        if (shouldCreateTask(typeList, SyncService.FLAG_SYNC_GAMES) && !uploadOnly) {
            tasks.add(SyncGamesRemove(application, syncResult, gameRepository))
            tasks.add(SyncGamesOldest(application, syncResult, gameRepository))
            tasks.add(SyncGamesUnupdated(application, syncResult, gameRepository))
        }
        if (shouldCreateTask(typeList, SyncService.FLAG_SYNC_PLAYS_UPLOAD)) {
            tasks.add(SyncPlaysUpload(application, syncResult, playRepository))
        }
        if (shouldCreateTask(typeList, SyncService.FLAG_SYNC_PLAYS_DOWNLOAD) && !uploadOnly) {
            tasks.add(SyncPlays(application, syncResult, account, playRepository))
        }
        if (shouldCreateTask(typeList, SyncService.FLAG_SYNC_BUDDIES) && !uploadOnly) {
            tasks.add(SyncBuddiesList(application, syncResult, userRepository))
            tasks.add(SyncBuddiesDetailOldest(application, syncResult, userRepository))
            tasks.add(SyncBuddiesDetailUnupdated(application, syncResult, userRepository))
        }
        return tasks
    }

    private fun shouldCreateTask(typeList: Int, type: Int): Boolean {
        return (typeList and type) == type
    }

    /**
     * Enable or disable the cancel receiver. (There's no reason for the receiver to be enabled when the sync isn't running.
     */
    private fun toggleCancelReceiver(enable: Boolean) {
        val receiver = ComponentName(context, CancelReceiver::class.java)
        context.packageManager.setComponentEnabledSetting(
            receiver,
            if (enable) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    /**
     * Show a notification of any exception thrown by a sync task that isn't caught by the task.
     */
    private fun showException(task: SyncTask?, t: Throwable) {
        val message = t.message?.ifEmpty {
            t.cause?.toString()
        } ?: t.cause?.toString()
        Timber.w(message)
        if (!prefs.getSyncShowErrors() || task == null) return
        val messageId = task.notificationSummaryMessageId
        if (messageId != SyncTask.NO_NOTIFICATION) {
            val text = context.getText(messageId)
            val builder = context
                .createNotificationBuilder(R.string.sync_notification_title_error, NotificationChannels.ERROR)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ERROR)
            if (!message.isNullOrBlank()) {
                builder.setStyle(NotificationCompat.BigTextStyle().bigText(message).setSummaryText(text))
            }
            context.notify(builder, NotificationTags.SYNC_ERROR)
        }
    }

    /**
     * Show that the sync was cancelled in a notification. This may be useless since the notification is cancelled
     * almost immediately after this is shown.
     */
    private fun notifySyncIsCancelled(messageId: Int) {
        if (!prefs.getSyncShowNotifications()) return
        val contextText = if (messageId == SyncTask.NO_NOTIFICATION) "" else context.getText(messageId)
        val builder = context
            .createNotificationBuilder(R.string.sync_notification_title_cancel, NotificationChannels.SYNC_PROGRESS)
            .setContentText(contextText)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
        context.notify(builder, NotificationTags.SYNC_PROGRESS)
    }
}
