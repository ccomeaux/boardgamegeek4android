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
        Timber.i("Plays sync request received")

        if (prefs[PREFERENCES_KEY_SYNC_PLAYS, false] != true) {
            Timber.i("Plays not set to sync")
            return Result.success()
        }

        Timber.i("Begin downloading plays")
        setForeground(createForegroundInfo(applicationContext.getString(R.string.sync_notification_plays)))
        return try {
            startTime = System.currentTimeMillis()

            val newestSyncDate = syncPrefs[SyncPrefs.TIMESTAMP_PLAYS_NEWEST_DATE] ?: 0L
            downloadPlays(newestSyncDate, 0L, PROGRESS_STEP_NEW)?.let { failureData ->
                return Result.failure(failureData)
            }
            if (newestSyncDate > 0L) {
                setProgress(PROGRESS_STEP_NEW, PROGRESS_ACTION_DELETING, minDate = newestSyncDate)
                val deletedCount = playRepository.deleteUnupdatedPlaysSince(startTime, newestSyncDate)
                Timber.i("Deleted $deletedCount unupdated plays since ${newestSyncDate.toDate()}")
            }

            val oldestDate = syncPrefs[SyncPrefs.TIMESTAMP_PLAYS_OLDEST_DATE] ?: Long.MAX_VALUE
            if (oldestDate > 0) {
                downloadPlays(0L, oldestDate, PROGRESS_STEP_OLD)?.let { failureData ->
                    return Result.failure(failureData)
                }
                if (oldestDate != Long.MAX_VALUE) {
                    setProgress(PROGRESS_STEP_OLD, PROGRESS_ACTION_DELETING, maxDate = oldestDate)
                    val deletedCount = playRepository.deleteUnupdatedPlaysBefore(startTime, oldestDate)
                    Timber.i("Deleted $deletedCount unupdated plays before ${oldestDate.toDate()}")
                }
                syncPrefs[SyncPrefs.TIMESTAMP_PLAYS_OLDEST_DATE] = 0L
            } else Timber.i("Already downloaded all past plays")

            setProgress(PROGRESS_STEP_STATS, PROGRESS_ACTION_UNKNOWN)
            playRepository.calculateStats()
            Timber.i("Plays synced successfully")
            Result.success()
        } catch (e: Exception) {
            val error = handleException(e)
            Result.failure(workDataOf(ERROR_MESSAGE to error))
        }
    }

    private suspend fun downloadPlays(minDate: Long, maxDate: Long, step: Int): Data? {
        var page = 1
        do {
            setProgress(step, PROGRESS_ACTION_WAITING, minDate, maxDate, page)
            if (page > 1) delay(playsFetchPauseMilliseconds)

            if (isStopped) {
                Timber.i("Stopping while downloading plays")
                return workDataOf(STOPPED_REASON to "Worker stopped early while downloading plays")
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
                setProgress(step, PROGRESS_ACTION_DOWNLOADING, minDate, maxDate, page)
                val (plays, hasMorePages) = playRepository.downloadPlays(minDate, maxDate, page)
                val gameIds = plays.map { it.gameId }.toSet()

                setProgress(step, PROGRESS_ACTION_SAVING, minDate, maxDate, page)
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
        return null
    }

    private suspend fun setProgress(step: Int, action: Int, minDate: Long = 0L, maxDate: Long = 0L, page: Int = 1) {
        setProgress(
            workDataOf(
                PROGRESS_STEP to step,
                PROGRESS_MIN_DATE to minDate,
                PROGRESS_MAX_DATE to maxDate,
                PROGRESS_PAGE to page,
                PROGRESS_ACTION to action,
            )
        )
    }

    private fun handleException(e: Exception): Data {
        if (e is CancellationException) {
            Timber.i("Canceling plays sync")
        } else {
            Timber.e(e)
            val bigText = if (e is HttpException) e.code().asHttpErrorMessage(applicationContext) else e.localizedMessage
            applicationContext.notifySyncError(applicationContext.getString(R.string.sync_notification_plays), bigText)
        }
        return workDataOf(ERROR_MESSAGE to (e.message ?: "Unknown exception while syncing plays"))
    }

    private fun Long.toDate() = this.formatDateTime(applicationContext, flags = DateUtils.FORMAT_SHOW_DATE)

    private fun createForegroundInfo(contentText: String): ForegroundInfo {
        return applicationContext.createForegroundInfo(R.string.sync_notification_title_plays, NOTIFICATION_ID_PLAYS, id, contentText)
    }

    companion object {
        const val UNIQUE_WORK_NAME = "com.boardgamegeek.SYNC_PLAYS"
        const val UNIQUE_WORK_NAME_AD_HOC = "${UNIQUE_WORK_NAME}.adhoc"
        const val STOPPED_REASON = "STOPPED_REASON"
        const val ERROR_MESSAGE = "ERROR_MESSAGE"

        const val PROGRESS_STEP = "STEP"
        const val PROGRESS_MIN_DATE = "MIN_DATE"
        const val PROGRESS_MAX_DATE = "MAX_DATE"
        const val PROGRESS_PAGE = "PAGE"
        const val PROGRESS_ACTION = "ACTION"

        const val PROGRESS_STEP_UNKNOWN = 0
        const val PROGRESS_STEP_NEW = 1
        const val PROGRESS_STEP_OLD = 2
        const val PROGRESS_STEP_STATS = 3

        const val PROGRESS_ACTION_UNKNOWN = 0
        const val PROGRESS_ACTION_WAITING = 1
        const val PROGRESS_ACTION_DOWNLOADING = 2
        const val PROGRESS_ACTION_SAVING = 3
        const val PROGRESS_ACTION_DELETING = 4

        fun requestSync(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<SyncPlaysWorker>()
                .setConstraints(context.createWorkConstraints(true))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_WORK_NAME_AD_HOC, ExistingWorkPolicy.KEEP, workRequest)
        }
    }
}