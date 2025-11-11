package com.boardgamegeek.work

import android.content.Context
import android.content.SharedPreferences
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.Player
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.pref.getBuddiesTimestamp
import com.boardgamegeek.repository.PlayRepository
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
    private val playRepository: PlayRepository,
) : CoroutineWorker(appContext, workerParams) {
    private val prefs: SharedPreferences by lazy { appContext.preferences() }
    private val syncPrefs: SharedPreferences by lazy { SyncPrefs.getPrefs(appContext) }

    private val userFetchPauseMilliseconds = RemoteConfig.getLong(RemoteConfig.KEY_SYNC_BUDDIES_FETCH_PAUSE_MILLIS)
    private val buddiesFetchIntervalDays = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_BUDDIES_FETCH_INTERVAL_DAYS)
    private val buddySyncSliceCount = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_BUDDIES_DAYS).coerceAtLeast(1)
    private val buddySyncSliceMaxSize = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_BUDDIES_MAX).coerceAtLeast(0)

    override suspend fun doWork(): Result {
        try {
            Timber.i("Syncing list of buddies")
            setProgress(PROGRESS_STEP_BUDDY_LIST)

            if (prefs[KEY_SYNC_PROGRESS, false] ?: false)
                setForeground(createForegroundInfo(applicationContext.getString(R.string.sync_notification_buddies_list)))

            if (prefs[PREFERENCES_KEY_SYNC_BUDDIES, false] == true) {
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
            } else {
                Timber.i("Buddies not set to sync")
            }

            val allBuddies = userRepository.loadBuddies().sortedBy { it.updatedTimestamp }
            val (newBuddies, existingBuddies) = allBuddies.partition { it.updatedTimestamp == 0L }

            Timber.i("Syncing new buddies")
            if (prefs[KEY_SYNC_PROGRESS, false] ?: false)
                setForeground(createForegroundInfo(applicationContext.getString(R.string.sync_notification_buddies_unupdated)))
            Timber.i("Found ${newBuddies.size} buddies that haven't been updated; updating at most $buddySyncSliceMaxSize of them")
            syncUsers(newBuddies.take(buddySyncSliceMaxSize).map { it.username }, PROGRESS_STEP_NEW_BUDDIES)?.let { return Result.failure(it) }

            Timber.i("Syncing stale buddies")
            if (prefs[KEY_SYNC_PROGRESS, false] ?: false)
                setForeground(createForegroundInfo(applicationContext.getString(R.string.sync_notification_buddies_oldest)))
            val limit = (existingBuddies.size / buddySyncSliceCount).coerceAtMost(buddySyncSliceMaxSize)
            Timber.i("Updating $limit users; ${existingBuddies.size} total users cut in $buddySyncSliceCount slices of no more than $buddySyncSliceMaxSize")
            syncUsers(existingBuddies.take(limit).map { it.username }, PROGRESS_STEP_STALE_BUDDIES)?.let { return Result.failure(it) }

            val allPlayers = playRepository.loadPlayers(Player.SortType.PLAY_COUNT).filter { it.username.isNotEmpty() }
            val (newPlayers, existingPlayers) = allPlayers.partition { it.userUpdatedTimestamp == null || it.userUpdatedTimestamp == 0L }

            Timber.i("Syncing new players")
            if (prefs[KEY_SYNC_PROGRESS, false] ?: false)
                setForeground(createForegroundInfo(applicationContext.getString(R.string.sync_notification_players_unupdated)))
            Timber.i("Found ${newPlayers.size} players that haven't been updated; updating at most $buddySyncSliceMaxSize of them")
            syncUsers(newPlayers.take(buddySyncSliceMaxSize).map { it.username }, PROGRESS_STEP_NEW_PLAYERS)?.let { return Result.failure(it) }

            Timber.i("Syncing stale players")
            if (prefs[KEY_SYNC_PROGRESS, false] ?: false)
                setForeground(createForegroundInfo(applicationContext.getString(R.string.sync_notification_players_oldest)))
            val playerLimit = (existingPlayers.size / buddySyncSliceCount).coerceAtMost(buddySyncSliceMaxSize)
            Timber.i("Updating $playerLimit users; ${existingPlayers.size} total users cut in $buddySyncSliceCount slices of no more than $buddySyncSliceMaxSize")
            syncUsers(
                existingPlayers.sortedBy { it.userUpdatedTimestamp }.take(playerLimit).map { it.username },
                PROGRESS_STEP_STALE_PLAYERS,
            )?.let { return Result.failure(it) }

            return Result.success()
        } catch (e: Exception) {
            return Result.failure(handleException(e))
        }
    }

    // download the details of each user in the list, indicating the step for marking progress.
    private suspend fun syncUsers(
        usernames: List<String>,
        step: Int,
    ): Data? {
        usernames.forEachIndexed { index, username ->
            checkIfStopped("Canceled while refreshing users")?.let { return it }
            setProgress(step, username, index, usernames.size)

            Timber.i("About to refresh user $username")
            if (prefs[KEY_SYNC_PROGRESS, false] ?: false)
                setForeground(createForegroundInfo(applicationContext.getString(R.string.sync_notification_user, username)))
            delay(userFetchPauseMilliseconds)
            try {
                userRepository.refresh(username)
            } catch (e: Exception) {
                return handleException(e)
            }
        }
        return null
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

    private fun checkIfStopped(@Suppress("SameParameterValue") reason: String): Data? {
        return if (isStopped) {
            Timber.i(reason)
            workDataOf(STOPPED_REASON to reason)
        } else {
            null
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "com.boardgamegeek.SYNC_USERS"
        const val UNIQUE_WORK_NAME_AD_HOC = "$UNIQUE_WORK_NAME.adhoc"
        private const val ERROR_MESSAGE = "ERROR_MESSAGE"
        private const val STOPPED_REASON = "STOPPED_REASON"

        const val PROGRESS_STEP = "PROGRESS_STEP"
        const val PROGRESS_USERNAME = "PROGRESS_USERNAME"
        const val PROGRESS_INDEX = "PROGRESS_INDEX"
        const val PROGRESS_TOTAL = "PROGRESS_TOTAL"

        const val PROGRESS_STEP_UNKNOWN = 0
        const val PROGRESS_STEP_BUDDY_LIST = 1
        const val PROGRESS_STEP_NEW_BUDDIES = 2
        const val PROGRESS_STEP_STALE_BUDDIES = 3
        const val PROGRESS_STEP_NEW_PLAYERS = 4
        const val PROGRESS_STEP_STALE_PLAYERS = 5

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
