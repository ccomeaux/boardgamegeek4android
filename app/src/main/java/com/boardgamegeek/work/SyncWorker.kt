package com.boardgamegeek.work

import android.content.Context
import android.content.SharedPreferences
import android.text.format.DateUtils
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.boardgamegeek.R
import com.boardgamegeek.db.UserDao
import com.boardgamegeek.entities.PlayEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.pref.getBuddiesTimestamp
import com.boardgamegeek.pref.setBuddiesTimestamp
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.PlayRepository
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
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val playRepository: PlayRepository,
    private val userRepository: UserRepository,
) : CoroutineWorker(appContext, workerParams) {
    private val prefs: SharedPreferences by lazy { appContext.preferences() }
    private val syncPrefs: SharedPreferences by lazy { SyncPrefs.getPrefs(appContext) }

    private val playsFetchPauseMilliseconds = RemoteConfig.getLong(RemoteConfig.KEY_SYNC_PLAYS_FETCH_PAUSE_MILLIS)
    private val buddiesFetchPauseMilliseconds = RemoteConfig.getLong(RemoteConfig.KEY_SYNC_BUDDIES_FETCH_PAUSE_MILLIS)
    private val buddiesFetchIntervalDays = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_BUDDIES_FETCH_INTERVAL_DAYS)
    private val buddySyncSliceCount = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_BUDDIES_DAYS)
    private val buddySyncSliceMaxSize = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_BUDDIES_MAX)

    private var startTime = System.currentTimeMillis() // TODO rename

    override suspend fun doWork(): Result {
        val syncType = inputData.getString(SYNC_TYPE)

        if (syncType == null || syncType == SYNC_TYPE_PLAYS) {
            uploadPlays()
            if (isStopped) return Result.failure(workDataOf(STOPPED_REASON to "Canceled after uploading plays"))
            downloadPlays()
            if (isStopped) return Result.failure(workDataOf(STOPPED_REASON to "Canceled after downloading plays"))
        }

        if (syncType == null || syncType == SYNC_TYPE_BUDDIES) {
            syncBuddies()
            if (isStopped) return Result.success(workDataOf(STOPPED_REASON to "Canceled after downloading buddies"))
        }

        return Result.success()
    }

    private suspend fun uploadPlays(): Result {
        Timber.i("Begin uploading plays")
        setForeground(createForegroundInfo(applicationContext.getString(R.string.sync_notification_plays_upload)))

        val playsToDelete = mutableListOf<PlayEntity>()
        val playsToUpsert = mutableListOf<PlayEntity>()
        val gameIds = mutableSetOf<Int>()
        val requestedGameId = inputData.getInt(PlayUpsertWorker.GAME_ID, BggContract.INVALID_ID)

        if (requestedGameId == BggContract.INVALID_ID) {
            Timber.i("Uploading all plays marked for deletion or updating")
            playsToDelete += playRepository.getDeletingPlays()
            playsToUpsert += playRepository.getUpdatingPlays()
        } else {
            Timber.i("Uploading all plays for game ID=$requestedGameId marked for deletion or updating")
            playsToDelete += playRepository.getDeletingPlays().filter { it.gameId == requestedGameId }
            playsToUpsert += playRepository.getUpdatingPlays().filter { it.gameId == requestedGameId }
        }

        setProgress(workDataOf(PROGRESS_MAX to playsToDelete.size + playsToUpsert.size + 2, PROGRESS_VALUE to 0))
        var playUploadCount = 0

        Timber.i("Found ${playsToDelete.count()} play(s) marked for deletion")
        playsToDelete.forEach { playEntity ->
            setProgress(workDataOf(PROGRESS_VALUE to ++playUploadCount))
            gameIds += if (playEntity.playId == BggContract.INVALID_ID) {
                playRepository.delete(playEntity.internalId)
                playEntity.gameId
            } else {
                val result = playRepository.deletePlay(playEntity)
                if (result.isSuccess) {
                    applicationContext.notifyDeletedPlay(result.getOrThrow())
                    playEntity.gameId
                } else return Result.failure(workDataOf(ERROR_MESSAGE to result.exceptionOrNull()?.message))
            }
        }

        Timber.i("Found ${playsToUpsert.count()} play(s) marked for upsert")
        playsToUpsert.forEach { playEntity ->
            setProgress(workDataOf(PROGRESS_VALUE to ++playUploadCount))
            val result = playRepository.upsertPlay(playEntity)
            if (result.isSuccess) {
                result.getOrNull()?.let { applicationContext.notifyLoggedPlay(it) }
                gameIds += playEntity.gameId
            } else return Result.failure(workDataOf(ERROR_MESSAGE to result.exceptionOrNull()?.message))
        }

        setProgress(workDataOf(PROGRESS_VALUE to ++playUploadCount))
        gameIds.filterNot { it == BggContract.INVALID_ID }.forEach { gameId ->
            playRepository.updateGamePlayCount(gameId)
        }

        setProgress(workDataOf(PROGRESS_VALUE to ++playUploadCount))
        playRepository.calculatePlayStats()

        return Result.success()
    }

    private suspend fun downloadPlays(): Result {
        if (prefs[PREFERENCES_KEY_SYNC_PLAYS, false] != true) {
            Timber.i("Plays not set to sync")
            return Result.success()
        }

        Timber.i("Begin downloading plays")
        setForeground(createForegroundInfo(applicationContext.getString(R.string.sync_notification_plays)))
        try {
            val startTime = System.currentTimeMillis()

            val newestSyncDate = syncPrefs[SyncPrefs.TIMESTAMP_PLAYS_NEWEST_DATE] ?: 0L
            val newestPlaysResult = downloadPlays(newestSyncDate, 0L)
            if (newestPlaysResult is Result.Failure) return newestPlaysResult
            if (newestSyncDate > 0L) {
                val deletedCount = playRepository.deleteUnupdatedPlaysSince(startTime, newestSyncDate)
                Timber.i("Deleted $deletedCount unupdated plays since ${newestSyncDate.toDate()}")
            }

            val oldestDate = syncPrefs[SyncPrefs.TIMESTAMP_PLAYS_OLDEST_DATE] ?: Long.MAX_VALUE
            if (oldestDate > 0) {
                val oldestPlaysResult = downloadPlays(0L, oldestDate)
                if (oldestPlaysResult is Result.Failure) return oldestPlaysResult
                if (oldestDate != Long.MAX_VALUE) {
                    val deletedCount = playRepository.deleteUnupdatedPlaysBefore(startTime, oldestDate)
                    Timber.i("Deleted $deletedCount unupdated plays before ${oldestDate.toDate()}")
                }
                syncPrefs[SyncPrefs.TIMESTAMP_PLAYS_OLDEST_DATE] = 0L
            } else Timber.i("Downloaded all past plays")
            playRepository.calculatePlayStats()
            Timber.i("Plays synced successfully ")
            return Result.success()
        } catch (e: Exception) {
            return handleException(e)
        }
    }

    private suspend fun downloadPlays(minDate: Long, maxDate: Long): Result {
        var page = 1
        do {
            if (page > 1) delay(playsFetchPauseMilliseconds)

            if (isStopped) {
                Timber.i("Stopping while downloading plays")
                return Result.failure(workDataOf(ERROR_MESSAGE to "Worker stopped early while downloading plays"))
            }

            val message = when {
                minDate == 0L && maxDate == 0L -> applicationContext.getString(R.string.sync_notification_plays_all)
                minDate == 0L -> applicationContext.getString(R.string.sync_notification_plays_old, maxDate.toDate())
                maxDate == 0L -> applicationContext.getString(R.string.sync_notification_plays_new, minDate.toDate())
                else -> applicationContext.getString(R.string.sync_notification_plays_between, minDate.toDate(), maxDate.toDate())
            }
            val contentText = when {
                page > 1 -> applicationContext.getString(R.string.sync_notification_page_suffix, message, page)
                else -> message
            }.also { Timber.i(it) }
            setForeground(createForegroundInfo(contentText))

            var shouldContinue: Boolean
            try {
                val (plays, hasMorePages) = playRepository.downloadPlays(minDate, maxDate, page)
                val gameIds = plays.map { it.gameId }.toSet()
                playRepository.saveFromSync(plays, startTime)
                plays.maxOfOrNull { it.dateInMillis }?.let {
                    if (it > (syncPrefs[SyncPrefs.TIMESTAMP_PLAYS_NEWEST_DATE] ?: 0L)) {
                        syncPrefs[SyncPrefs.TIMESTAMP_PLAYS_NEWEST_DATE] = it
                    }
                }
                plays.minOfOrNull { it.dateInMillis }?.let {
                    if (it < (syncPrefs[SyncPrefs.TIMESTAMP_PLAYS_OLDEST_DATE] ?: Long.MAX_VALUE)) {
                        syncPrefs[SyncPrefs.TIMESTAMP_PLAYS_OLDEST_DATE] = it
                    }
                }
                gameIds.filterNot { it == BggContract.INVALID_ID }.forEach { gameId ->
                    playRepository.updateGamePlayCount(gameId)
                }
                shouldContinue = hasMorePages
            } catch (e: Exception) {
                return handleException(e)
            }
            page++
        } while (shouldContinue)
        return Result.success()
    }

    private suspend fun syncBuddies(): Result {
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
        val bigText = if (e is HttpException) e.code().asHttpErrorMessage(applicationContext) else e.localizedMessage
        applicationContext.notifySyncError(applicationContext.getString(R.string.sync_notification_plays), bigText)
        return Result.failure(workDataOf(ERROR_MESSAGE to e.message))
    }

    private fun Long.toDate() = this.formatDateTime(applicationContext, flags = DateUtils.FORMAT_SHOW_DATE)

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
        const val UNIQUE_WORK_NAME = "com.boardgamegeek.SYNC"
        const val SYNC_TYPE = "SYNC_TYPE"
        const val SYNC_TYPE_PLAYS = "SYNC_TYPE_PLAYS"
        const val SYNC_TYPE_BUDDIES = "SYNC_TYPE_BUDDIES"
        const val ERROR_MESSAGE = "ERROR_MESSAGE"
        const val PROGRESS_MAX = "PROGRESS_MAX"
        const val PROGRESS_VALUE = "PROGRESS_VALUE"
        const val PROGRESS_USERNAME = "PROGRESS_USERNAME"
        const val STOPPED_REASON = "STOPPED_REASON"

        fun requestPlaySync(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(context.createWorkConstraints(true))
                .setInputData(workDataOf(SYNC_TYPE to SYNC_TYPE_PLAYS))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }

        fun requestBuddySync(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(context.createWorkConstraints(true))
                .setInputData(workDataOf(SYNC_TYPE to SYNC_TYPE_BUDDIES))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}