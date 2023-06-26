package com.boardgamegeek.work

import android.content.Context
import android.content.SharedPreferences
import android.text.format.DateUtils
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.boardgamegeek.R
import com.boardgamegeek.entities.PlayEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.util.RemoteConfig
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import retrofit2.HttpException
import timber.log.Timber
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val playRepository: PlayRepository,
) : CoroutineWorker(appContext, workerParams) {

    private val prefs: SharedPreferences by lazy { appContext.preferences() }
    private val syncPrefs: SharedPreferences by lazy { SyncPrefs.getPrefs(appContext) }
    private val playsFetchPauseMilliseconds = RemoteConfig.getLong(RemoteConfig.KEY_SYNC_PLAYS_FETCH_PAUSE_MILLIS)

    private var startTime = System.currentTimeMillis()

    override suspend fun doWork(): Result {
        val syncType = inputData.getString(SYNC_TYPE)

        if (syncType == null || syncType == SYNC_TYPE_PLAYS) {
            uploadPlays()
            if (isStopped) return Result.success(workDataOf(STOPPED_REASON to "Canceled after uploading plays"))
            downloadPlays()
            if (isStopped) return Result.success(workDataOf(STOPPED_REASON to "Canceled after downloading plays"))
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
            Timber.i("Plays not set to sync; aborting")
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
            Timber.i("Plays sync ended with exception:\n$e")
            return Result.failure(workDataOf(ERROR_MESSAGE to e.message))
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
                val bigText = if (e is HttpException) e.code().asHttpErrorMessage(applicationContext) else e.localizedMessage
                applicationContext.notifySyncError(applicationContext.getString(R.string.sync_notification_plays), bigText)
                return Result.failure(workDataOf(ERROR_MESSAGE to e.message))
            }
            page++
        } while (shouldContinue)
        return Result.success()
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
        const val ERROR_MESSAGE = "ERROR_MESSAGE"
        const val PROGRESS_MAX = "PROGRESS_MAX"
        const val PROGRESS_VALUE = "PROGRESS_VALUE"
        const val STOPPED_REASON = "STOPPED_REASON"

        fun requestPlaySync(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(context.createWorkConstraints(true))
                .setInputData(workDataOf(SYNC_TYPE to SYNC_TYPE_PLAYS))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}