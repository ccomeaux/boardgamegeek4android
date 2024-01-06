package com.boardgamegeek.work

import android.content.Context
import android.content.SharedPreferences
import android.text.format.DateUtils
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.pref.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.util.RemoteConfig
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import retrofit2.HttpException
import timber.log.Timber
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncPlaysWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val playRepository: PlayRepository,
) : CoroutineWorker(appContext, workerParams) {
    private val prefs: SharedPreferences by lazy { appContext.preferences() }
    private val syncPrefs: SharedPreferences by lazy { SyncPrefs.getPrefs(appContext) }

    private val playsFetchPauseMilliseconds = RemoteConfig.getLong(RemoteConfig.KEY_SYNC_PLAYS_FETCH_PAUSE_MILLIS)

    private var startTime = System.currentTimeMillis()

    override suspend fun doWork(): Result {
        if (prefs[PREFERENCES_KEY_SYNC_PLAYS, false] != true) {
            Timber.i("Plays not set to sync")
            return Result.success()
        }

        Timber.i("Begin downloading plays")
        setForeground(createForegroundInfo(applicationContext.getString(R.string.sync_notification_plays)))
        return try {
            startTime = System.currentTimeMillis()

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
            } else Timber.i("Already downloaded all past plays")

            playRepository.calculatePlayStats()
            Timber.i("Plays synced successfully")
            Result.success()
        } catch (e: Exception) {
            handleException(e)
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

    private fun handleException(e: Exception): Result {
        Timber.e(e)
        if (e !is CancellationException) {
            val bigText = if (e is HttpException) e.code().asHttpErrorMessage(applicationContext) else e.localizedMessage
            applicationContext.notifySyncError(applicationContext.getString(R.string.sync_notification_plays), bigText)
        }
        return Result.failure(workDataOf(ERROR_MESSAGE to e.message))
    }

    private fun Long.toDate() = this.formatDateTime(applicationContext, flags = DateUtils.FORMAT_SHOW_DATE)

    private fun createForegroundInfo(contentText: String): ForegroundInfo {
        return applicationContext.createForegroundInfo(R.string.sync_notification_title_plays, NOTIFICATION_ID_PLAYS, id, contentText)
    }

    companion object {
        const val UNIQUE_WORK_NAME = "com.boardgamegeek.SYNC_PLAYS"
        const val ERROR_MESSAGE = "ERROR_MESSAGE"

        fun requestSync(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<SyncPlaysWorker>()
                .setConstraints(context.createWorkConstraints(true))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}