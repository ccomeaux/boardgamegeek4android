package com.boardgamegeek.work

import android.content.Context
import android.content.SharedPreferences
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

/***
 * 1. Download list of all GeekBuddies for the logged in user
 * 2. Update a few users that haven't been updated in a while
 * 3. Update all users that haven't been updated at all
 */

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
            updatedBuddyCount += updateBuddy(username)
        }

        Timber.i("Syncing unupdated buddies")
        setForeground(createForegroundInfo(applicationContext.getString(R.string.sync_notification_buddies_unupdated)))

        val unupdatedBuddies = allUsers.filter { it.updatedTimestamp == 0L }.map { it.userName }
        Timber.i("Found ${unupdatedBuddies.size} buddies that haven't been updated; updating at most $buddySyncSliceMaxSize of them")
        for (username in unupdatedBuddies.take(buddySyncSliceMaxSize)) {
            if (isStopped) return Result.failure(workDataOf(STOPPED_REASON to "Canceled during unupdated buddy update"))
            updatedBuddyCount += updateBuddy(username)
        }
        Timber.i("Updated %,d stale & unupdated buddies", updatedBuddyCount)

        return Result.success()
    }

    private suspend fun updateBuddy(username: String): Int {
        Timber.i("About to refresh user $username")
        setProgress(workDataOf(PROGRESS_USERNAME to username))
        setForeground(createForegroundInfo(applicationContext.getString(R.string.sync_notification_buddy, username)))
        delay(buddiesFetchPauseMilliseconds)
        return try {
            userRepository.refresh(username)
            1
        } catch (e: Exception) {
            handleException(e)
            0
        }
    }

    private fun handleException(e: Exception): Result {
        Timber.e(e)
        val bigText = if (e is HttpException) e.code().asHttpErrorMessage(applicationContext) else e.localizedMessage
        applicationContext.notifySyncError(applicationContext.getString(R.string.sync_notification_buddies_list), bigText)
        return Result.failure(workDataOf(ERROR_MESSAGE to e.message))
    }

    private fun createForegroundInfo(contentText: String): ForegroundInfo {
        return applicationContext.createForegroundInfo(R.string.sync_notification_title_buddies, NOTIFICATION_ID_USERS, id, contentText)
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