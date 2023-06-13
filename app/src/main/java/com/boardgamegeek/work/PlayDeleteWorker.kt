package com.boardgamegeek.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.boardgamegeek.entities.PlayEntity
import com.boardgamegeek.extensions.notifyDeletedPlay
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.PlayRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class PlayDeleteWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val playRepository: PlayRepository,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val gameIds = mutableSetOf<Int>()
        val internalId = inputData.getLong(INTERNAL_ID, BggContract.INVALID_ID.toLong())
        if (internalId == BggContract.INVALID_ID.toLong()) {
            Timber.i("Deleting all plays marked for deletion")
            val plays = playRepository.getDeletingPlays()
            plays.forEach { playEntity ->
                val (gameId, errorMessage) = deletePlayAndNotify(playEntity)
                if (errorMessage.isNotBlank())
                    return Result.failure(workDataOf(ERROR_MESSAGE to errorMessage))
                else gameIds += gameId
            }
        } else {
            Timber.i("Deleting play with internal ID=$internalId")
            playRepository.loadPlay(internalId)?.let { playEntity ->
                val (gameId, errorMessage) = deletePlayAndNotify(playEntity)
                if (errorMessage.isNotBlank())
                    return Result.failure(workDataOf(ERROR_MESSAGE to errorMessage))
                else gameIds += gameId
            } ?: return Result.failure(workDataOf(ERROR_MESSAGE to "Failed to load play for deletion with internal ID=$internalId"))
        }
        gameIds.forEach { gameId ->
            playRepository.updateGamePlayCount(gameId)
        }
        playRepository.calculatePlayStats()
        return Result.success()
    }

    private suspend fun deletePlayAndNotify(playEntity: PlayEntity): Pair<Int, String> {
        return if (playEntity.playId == BggContract.INVALID_ID) {
            playRepository.delete(playEntity.internalId)
            playEntity.gameId to ""
        } else {
            val deleteResult = playRepository.deletePlay(playEntity)
            if (deleteResult.errorMessage.isBlank()) {
                applicationContext.notifyDeletedPlay(deleteResult)
                playEntity.gameId to ""
            } else {
                BggContract.INVALID_ID to deleteResult.errorMessage
            }
        }
    }

    companion object {
        const val INTERNAL_ID = "INTERNAL_ID"
        const val ERROR_MESSAGE = "ERROR_MESSAGE"
    }
}