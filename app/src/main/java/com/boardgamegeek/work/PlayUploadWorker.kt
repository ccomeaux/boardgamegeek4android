package com.boardgamegeek.work

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.boardgamegeek.R
import com.boardgamegeek.extensions.KEY_SYNC_PROGRESS
import com.boardgamegeek.extensions.createWorkConstraints
import com.boardgamegeek.model.Play
import com.boardgamegeek.model.PlayUploadResult
import com.boardgamegeek.extensions.formatList
import com.boardgamegeek.extensions.get
import com.boardgamegeek.extensions.notifyLoggedPlay
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.PlayRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.concurrent.TimeUnit

@HiltWorker
class PlayUploadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val playRepository: PlayRepository,
) : CoroutineWorker(appContext, workerParams) {
    private val prefs: SharedPreferences by lazy { appContext.preferences() }
    private val gameIds = mutableSetOf<Int>()

    override suspend fun doWork(): Result {
        Timber.i("Begin uploading plays")

        if (prefs[KEY_SYNC_PROGRESS, false] ?: false)
            setForeground(createForegroundInfo(applicationContext.getString(R.string.sync_notification_plays_upload)))

        val playsToDelete = mutableListOf<Play>()
        val playsToUpsert = mutableListOf<Play>()

        val internalId = inputData.getLong(INTERNAL_ID, BggContract.INVALID_ID.toLong())
        val internalIds = inputData.getLongArray(INTERNAL_IDS)
        val requestedGameId = inputData.getInt(GAME_ID, BggContract.INVALID_ID)

        if (internalId != BggContract.INVALID_ID.toLong()) {
            Timber.i("Uploading play with internal ID $internalId")
            playRepository.loadPlay(internalId)?.let { play ->
                if (play.deleteTimestamp > 0L) playsToDelete += play
                if (play.updateTimestamp > 0L) playsToUpsert += play
            }
        } else if (internalIds != null && internalIds.isNotEmpty()) {
            Timber.i("Uploading plays with internal IDs ${internalIds.toList().formatList()}")
            internalIds.forEach {
                playRepository.loadPlay(it)?.let { play ->
                    if (play.deleteTimestamp > 0L) playsToDelete += play
                    if (play.updateTimestamp > 0L) playsToUpsert += play
                }
            }
        } else if (requestedGameId != BggContract.INVALID_ID) {
            Timber.i("Uploading all plays for game ID=$requestedGameId marked for deletion or updating")
            playsToDelete += playRepository.loadDeletingPlays().filter { it.gameId == requestedGameId }
            playsToUpsert += playRepository.loadUpdatingPlays().filter { it.deleteTimestamp == 0L }.filter { it.gameId == requestedGameId }
        } else {
            Timber.i("Uploading all plays marked for deletion or updating")
            playsToDelete += playRepository.loadDeletingPlays()
            playsToUpsert += playRepository.loadUpdatingPlays().filter { it.deleteTimestamp == 0L }
        }

        Timber.i("Found ${playsToDelete.count()} play(s) marked for deletion")
        uploadList(playsToDelete, R.string.sync_notification_plays_upload_delete) {
            playRepository.deletePlay(it)
        }

        Timber.i("Found ${playsToUpsert.count()} play(s) marked for upsert")
        uploadList(playsToUpsert, R.string.sync_notification_plays_upload_update) {
            playRepository.uploadPlay(it)
        }

        if (prefs[KEY_SYNC_PROGRESS, false] ?: false)
            setForeground(createForegroundInfo(applicationContext.getString(R.string.sync_notification_plays_upload_stats)))
        gameIds.filterNot { it == BggContract.INVALID_ID }.forEach { gameId ->
            playRepository.updateGamePlayCount(gameId)
            Timber.i("Updated game [$gameId]'s game count")
        }
        if (gameIds.isNotEmpty()) {
            playRepository.calculateStats()
            Timber.i("Recalculated game stats")
        }

        return Result.success()
    }

    private suspend fun uploadList(
        playsToUpsert: MutableList<Play>,
        @StringRes messageResId: Int,
        uploadItem: suspend (item: Play) -> kotlin.Result<PlayUploadResult>
    ) : Result {
        playsToUpsert.forEach { play ->
            if (prefs[KEY_SYNC_PROGRESS, false] ?: false)
                setForeground(createForegroundInfo(applicationContext.getString(messageResId, play.gameName)))
            val result = uploadItem(play)
            if (result.isSuccess) {
                result.getOrNull()?.let {
                    applicationContext.notifyLoggedPlay(it)
                }
                gameIds += play.gameId
            } else return Result.failure(workDataOf(ERROR_MESSAGE to result.exceptionOrNull()?.message))
        }
        return Result.success()
    }

    private fun createForegroundInfo(contentText: String): ForegroundInfo {
        return applicationContext.createForegroundInfo(R.string.sync_notification_title_play_upload, NOTIFICATION_ID_PLAYS_UPLOAD, id, contentText)
    }

    companion object {
        const val UNIQUE_WORK_NAME = "com.boardgamegeek.UPLOAD_PLAYS"
        const val INTERNAL_ID = "INTERNAL_ID"
        const val INTERNAL_IDS = "INTERNAL_IDS"
        const val GAME_ID = "GAME_ID"
        const val ERROR_MESSAGE = "ERROR_MESSAGE"

        fun requestSync(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<PlayUploadWorker>()
                .setConstraints(context.createWorkConstraints())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}