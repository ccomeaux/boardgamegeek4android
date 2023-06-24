package com.boardgamegeek.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.boardgamegeek.R
import com.boardgamegeek.entities.PlayEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.PlayRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val playRepository: PlayRepository,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
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

    private fun createForegroundInfo(contentText: String): ForegroundInfo {
        val id = NotificationChannels.SYNC_PROGRESS //applicationContext.getString( com.boardgamegeek.R.string.notification_channel_id)
        val title = applicationContext.getString(R.string.sync_notification_title)
        val cancel = applicationContext.getString(R.string.cancel)
        val cancelIntent = WorkManager.getInstance(applicationContext).createCancelPendingIntent(getId())

        // Create a Notification channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }

        val notification = NotificationCompat.Builder(applicationContext, id)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_stat_bgg)
            .setColor(ContextCompat.getColor(applicationContext, R.color.primary))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setProgress(1, 0, true)
            .addAction(R.drawable.ic_baseline_clear_24, cancel, cancelIntent)
            .build()

        return ForegroundInfo(42, notification) // What is 42?!
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        applicationContext.getSystemService<NotificationManager>()?.let {
            it.createNotificationChannel(
                NotificationChannel(
                    NotificationChannels.SYNC_PROGRESS,
                    applicationContext.getString(R.string.channel_name_sync_progress),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = applicationContext.getString(R.string.channel_description_sync_progress)
                }
            )
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "com.boardgamegeek.SYNC"
        const val ERROR_MESSAGE = "ERROR_MESSAGE"
        const val PROGRESS_MAX = "PROGRESS_MAX"
        const val PROGRESS_VALUE = "PROGRESS_VALUE"
    }
}