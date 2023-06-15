package com.boardgamegeek.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.boardgamegeek.entities.PlayEntity
import com.boardgamegeek.extensions.notifyLoggedPlay
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.PlayRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class PlayUploadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val playRepository: PlayRepository,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val gameIds = mutableSetOf<Int>()
        val internalId = inputData.getLong(INTERNAL_ID, BggContract.INVALID_ID.toLong())
        if (internalId == BggContract.INVALID_ID.toLong()) {
            Timber.i("Upserting all plays marked for update")
            val plays = playRepository.getUpdatingPlays()
            Timber.i("Found ${plays.count()} play(s) marked for update")
            plays.forEach { playEntity ->
                val (gameId, errorMessage) = upsertPlayAndNotify(playEntity)
                if (errorMessage.isNotBlank())
                    return Result.failure(workDataOf(ERROR_MESSAGE to errorMessage))
                else gameIds += gameId
            }
        } else {
            Timber.i("Upserting play with internal ID=$internalId")
            playRepository.loadPlay(internalId)?.let { playEntity ->
                val (gameId, errorMessage) = upsertPlayAndNotify(playEntity)
                if (errorMessage.isNotBlank())
                    return Result.failure(workDataOf(PlayDeleteWorker.ERROR_MESSAGE to errorMessage))
                else gameIds += gameId
            } ?: return Result.failure(workDataOf(ERROR_MESSAGE to "Failed to load play for upserting with internal ID=$internalId"))
        }
        gameIds.forEach { gameId ->
            playRepository.updateGamePlayCount(gameId)
        }
        playRepository.calculatePlayStats()
        return Result.success()
    }

    private suspend fun upsertPlayAndNotify(playEntity: PlayEntity): Pair<Int, String> {
        val uploadResult = playRepository.uploadPlay(playEntity)
        return if (uploadResult.errorMessage.isBlank()) {
            applicationContext.notifyLoggedPlay(uploadResult)
            playEntity.gameId to ""
        } else {
            BggContract.INVALID_ID to uploadResult.errorMessage
        }
    }

    companion object {
        const val INTERNAL_ID = "INTERNAL_ID"
        const val ERROR_MESSAGE = "ERROR_MESSAGE"
    }
}