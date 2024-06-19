package com.boardgamegeek.work

import android.content.Context
import android.content.SharedPreferences
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.pref.getBuddiesTimestamp
import com.boardgamegeek.repository.UserRepository
import com.boardgamegeek.util.RemoteConfig
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
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
    private val buddySyncSliceCount = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_BUDDIES_DAYS).coerceAtLeast(1)
    private val buddySyncSliceMaxSize = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_BUDDIES_MAX).coerceAtLeast(0)

    override suspend fun doWork(): Result {
        if (prefs[PREFERENCES_KEY_SYNC_BUDDIES, false] != true) {
            Timber.i("Buddies not set to sync")
            return Result.success()
        }

        Timber.i("Syncing list of buddies")
        setProgress(PROGRESS_STEP_BUDDY_LIST)
        setForeground(createForegroundInfo(applicationContext.getString(R.string.sync_notification_buddies_list)))
        try {
            val forceBuddySync = inputData.getBoolean(KEY_FORCE_BUDDY_SYNC, false)
            if (forceBuddySync) {
                Timber.i("Forced downloading buddies list")
                userRepository.refreshBuddies()?.let {
                    return Result.failure(workDataOf(ERROR_MESSAGE to it))
                }
            } else {
                val lastCompleteSync = syncPrefs.getBuddiesTimestamp()
                if (lastCompleteSync >= 0 && !lastCompleteSync.isOlderThan(buddiesFetchIntervalDays.days)) {
                    Timber.i("Skipping downloading buddies list; we synced already within the last $buddiesFetchIntervalDays days")
                } else {
                    userRepository.refreshBuddies()?.let {
                        return Result.failure(workDataOf(ERROR_MESSAGE to it))
                    }
                }
            }

            val allBuddies = userRepository.loadBuddies().sortedBy { it.updatedTimestamp }
            var updatedBuddyCount = 0

            Timber.i("Syncing oldest buddies")
            setForeground(createForegroundInfo(applicationContext.getString(R.string.sync_notification_buddies_oldest)))

            val unupdatedBuddyUsernames = allBuddies.filter { it.updatedTimestamp == 0L }.map { it.username }
            Timber.i("Found ${unupdatedBuddyUsernames.size} buddies that haven't been updated; updating at most $buddySyncSliceMaxSize of them")
            val limitedUnupdatedBuddyUsernames = unupdatedBuddyUsernames.take(buddySyncSliceMaxSize)
            limitedUnupdatedBuddyUsernames.forEachIndexed { index, username ->
                if (isStopped) {
                    return Result.failure(workDataOf(STOPPED_REASON to "Canceled during unupdated buddy update"))
                }
                setProgress(PROGRESS_STEP_UNUPDATED_USERS, username, index, limitedUnupdatedBuddyUsernames.size)
                updateBuddy(username)?.let {
                    return Result.failure(it)
                }
                updatedBuddyCount++
            }
            Timber.i("Updated %,d stale & unupdated buddies", updatedBuddyCount)

            val staleBuddyUsernames = allBuddies.filter { it.updatedTimestamp > 0L }.map { it.username }
            val limit = (staleBuddyUsernames.size / buddySyncSliceCount).coerceAtMost(buddySyncSliceMaxSize)
            Timber.i("Updating $limit buddies; ${staleBuddyUsernames.size} total buddies cut in $buddySyncSliceCount slices of no more than $buddySyncSliceMaxSize")
            val limitedStaleBuddyUsernames = staleBuddyUsernames.take(limit)
            limitedStaleBuddyUsernames.forEachIndexed { index, username ->
                if (isStopped) {
                    return Result.failure(workDataOf(STOPPED_REASON to "Canceled during stale buddy update"))
                }
                setProgress(PROGRESS_STEP_STALE_USERS, username, index, limitedStaleBuddyUsernames.size)
                updateBuddy(username)?.let {
                    return Result.failure(it)
                }
                updatedBuddyCount++
            }

            Timber.i("Syncing unupdated buddies")
            setForeground(createForegroundInfo(applicationContext.getString(R.string.sync_notification_buddies_unupdated)))

            return Result.success()
        } catch (e: Exception) {
            return Result.failure(handleException(e))
        }
    }

    private suspend fun setProgress(step: Int, username: String? = null, progress: Int = 0, max: Int = 0) {
        setProgress(
            workDataOf(
                PROGRESS_STEP to step,
                PROGRESS_USERNAME to username,
                PROGRESS_INDEX to progress,
                PROGRESS_TOTAL to max,
            )
        )
    }

    private suspend fun updateBuddy(username: String): Data? {
        Timber.i("About to refresh user $username")
        setForeground(createForegroundInfo(applicationContext.getString(R.string.sync_notification_user, username)))
        delay(buddiesFetchPauseMilliseconds)
        return try {
            userRepository.refresh(username)
            null
        } catch (e: Exception) {
            handleException(e)
        }
    }

    private fun handleException(e: Exception): Data {
        if (e is CancellationException) {
            Timber.i("Canceling users sync")
        } else {
            Timber.e(e)
            val bigText = if (e is HttpException) e.code().asHttpErrorMessage(applicationContext) else e.localizedMessage
            applicationContext.notifySyncError(applicationContext.getString(R.string.sync_notification_buddies_list), bigText)
        }
        return workDataOf(ERROR_MESSAGE to (e.message ?: "Unknown exception while syncing users"))
    }

    private fun createForegroundInfo(contentText: String): ForegroundInfo {
        return applicationContext.createForegroundInfo(R.string.sync_notification_title_buddies, NOTIFICATION_ID_USERS, id, contentText)
    }

    companion object {
        const val UNIQUE_WORK_NAME = "com.boardgamegeek.SYNC_USERS"
        const val UNIQUE_WORK_NAME_AD_HOC = "$UNIQUE_WORK_NAME.adhoc"
        const val ERROR_MESSAGE = "ERROR_MESSAGE"
        const val STOPPED_REASON = "STOPPED_REASON"

        const val PROGRESS_STEP = "PROGRESS_STEP"
        const val PROGRESS_USERNAME = "PROGRESS_USERNAME"
        const val PROGRESS_INDEX = "PROGRESS_INDEX"
        const val PROGRESS_TOTAL = "PROGRESS_TOTAL"

        const val PROGRESS_STEP_UNKNOWN = 0
        const val PROGRESS_STEP_BUDDY_LIST = 1
        const val PROGRESS_STEP_UNUPDATED_USERS = 2
        const val PROGRESS_STEP_STALE_USERS = 3

        private const val KEY_FORCE_BUDDY_SYNC = "KEY_FORCE_BUDDY_SYNC"

        fun requestSync(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<SyncUsersWorker>()
                .setInputData(workDataOf(KEY_FORCE_BUDDY_SYNC to true))
                .setConstraints(context.createWorkConstraints(true))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_WORK_NAME_AD_HOC, ExistingWorkPolicy.KEEP, workRequest)
        }
    }
}
