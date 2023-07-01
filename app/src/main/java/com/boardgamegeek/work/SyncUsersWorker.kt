package com.boardgamegeek.work

import android.content.Context
import android.content.SharedPreferences
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.boardgamegeek.R
import com.boardgamegeek.db.UserDao
import com.boardgamegeek.extensions.*
import com.boardgamegeek.pref.*
import com.boardgamegeek.repository.UserRepository
import com.boardgamegeek.util.RemoteConfig
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import retrofit2.HttpException
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.days

@HiltWorker
class SyncUsersWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val userRepository: UserRepository,
) : CoroutineWorker(appContext, workerParams) {
    private val prefs: SharedPreferences by lazy { appContext.preferences() }
    private val syncPrefs: SharedPreferences by lazy { SyncPrefs.getPrefs(appContext) }

    private val buddiesFetchPauseMilliseconds = RemoteConfig.getLong(RemoteConfig.KEY_SYNC_BUDDIES_FETCH_PAUSE_MILLIS)
    private val buddiesFetchIntervalDays = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_BUDDIES_FETCH_INTERVAL_DAYS)
    private val buddySyncSliceCount = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_BUDDIES_DAYS)
    private val buddySyncSliceMaxSize = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_BUDDIES_MAX)

    override suspend fun doWork(): Result {
        if (prefs[PREFERENCES_KEY_SYNC_BUDDIES, false] != true) {
            Timber.i("Buddies not set to sync")
            return Result.success()
        }

        Timber.i("Begin downloading buddies")
        setForeground(createForegroundInfo(applicationContext.getString(R.string.sync_notification_buddies_list)))
        try {
            val lastCompleteSync = syncPrefs.getBuddiesTimestamp()
            if (lastCompleteSync >= 0 && !lastCompleteSync.isOlderThan(buddiesFetchIntervalDays.days)) {
                Timber.i("Skipping downloading buddies list; we synced already within the last $buddiesFetchIntervalDays days")
            } else {
                val timestamp = System.currentTimeMillis()
                val (savedCount, deletedCount) = userRepository.refreshBuddies(timestamp)
                Timber.i("Saved $savedCount buddies; pruned $deletedCount users who are no longer buddies")
                syncPrefs.setBuddiesTimestamp(timestamp)
            }
        } catch (e: Exception) {
            return handleException(e)
        }

        Timber.i("Syncing oldest buddies")
        setForeground(createForegroundInfo(applicationContext.getString(R.string.sync_notification_buddies_oldest)))
        var updatedBuddyCount = 0
        val allUsers = userRepository.loadBuddies(sortBy = UserDao.UsersSortBy.UPDATED)

        val staleBuddies = allUsers.filter { it.updatedTimestamp > 0L }.map { it.userName }
        val limit = (staleBuddies.size / buddySyncSliceCount.coerceAtLeast(1)).coerceAtMost(buddySyncSliceMaxSize)
        Timber.i("Updating $limit buddies; ${staleBuddies.size} total buddies cut in $buddySyncSliceCount slices of no more than $buddySyncSliceMaxSize")
        for (username in staleBuddies.take(limit)) {
            if (isStopped) return Result.failure(workDataOf(STOPPED_REASON to "Canceled during stale buddy update"))
            Timber.i("About to refresh user $username")
            setProgress(workDataOf(PROGRESS_USERNAME to username))
            delay(buddiesFetchPauseMilliseconds)
            setForeground(createForegroundInfo(applicationContext.getString(R.string.sync_notification_buddy, username)))
            try {
                userRepository.refresh(username)
                updatedBuddyCount++
            } catch (e: Exception) {
                handleException(e)
            }
        }

        Timber.i("Syncing unupdated buddies")
        setForeground(createForegroundInfo(applicationContext.getString(R.string.sync_notification_buddies_unupdated)))

        val unupdatedBuddies = allUsers.filter { it.updatedTimestamp == 0L }.map { it.userName }
        Timber.i("Found ${unupdatedBuddies.size} buddies that haven't been updated; updating at most $buddySyncSliceMaxSize of them")
        for (username in unupdatedBuddies.take(buddySyncSliceMaxSize)) {
            if (isStopped) return Result.failure(workDataOf(STOPPED_REASON to "Canceled during unupdated buddy update"))
            Timber.i("About to refresh user $username")
            setProgress(workDataOf(PROGRESS_USERNAME to username))
            delay(buddiesFetchPauseMilliseconds)
            setForeground(createForegroundInfo(applicationContext.getString(R.string.sync_notification_buddy, username)))
            try {
                userRepository.refresh(username)
                updatedBuddyCount++
            } catch (e: Exception) {
                handleException(e)
            }
        }
        Timber.i("Updated %,d stale & unupdated buddies", updatedBuddyCount)

        return Result.success()
    }

    private fun handleException(e: Exception): Result {
        Timber.e(e)
        val bigText = if (e is HttpException) e.code().asHttpErrorMessage(applicationContext) else e.localizedMessage
        applicationContext.notifySyncError(applicationContext.getString(R.string.sync_notification_plays), bigText)
        return Result.failure(workDataOf(ERROR_MESSAGE to e.message))
    }

    private fun createForegroundInfo(contentText: String): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, NotificationChannels.SYNC_PROGRESS)
            .setContentTitle(applicationContext.getString(R.string.sync_notification_title))
            .setTicker(applicationContext.getString(R.string.sync_notification_title))
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

        return ForegroundInfo(42, notification) // What is 42?!
    }

    companion object {
        const val UNIQUE_WORK_NAME = "com.boardgamegeek.SYNC_USERS"
        const val ERROR_MESSAGE = "ERROR_MESSAGE"
        const val PROGRESS_USERNAME = "PROGRESS_USERNAME"
        const val STOPPED_REASON = "STOPPED_REASON"

        fun requestSync(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<SyncUsersWorker>()
                .setConstraints(context.createWorkConstraints(true))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}