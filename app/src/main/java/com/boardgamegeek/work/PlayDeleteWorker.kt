package com.boardgamegeek.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.boardgamegeek.extensions.notifyDeletedPlay
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.PlayRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class PlayDeleteWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val playRepository: PlayRepository,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val internalId = inputData.getLong(INTERNAL_ID, BggContract.INVALID_ID.toLong())
        playRepository.loadPlay(internalId)?.let { playEntity ->
            val deleteResult = playRepository.deletePlay(playEntity)
            if (deleteResult.errorMessage.isBlank()) {
                playRepository.updateGamePlayCount(playEntity.gameId)
                playRepository.calculatePlayStats()
                applicationContext.notifyDeletedPlay(deleteResult)
                return Result.success() // TODO include result?
            } else {
                return Result.failure(workDataOf(ERROR_MESSAGE to deleteResult.errorMessage))
            }
        }
        return Result.failure(workDataOf(ERROR_MESSAGE to "Failed to load play for deletion with internal ID=$internalId"))
    }

    companion object {
        const val INTERNAL_ID = "INTERNAL_ID"
        const val ERROR_MESSAGE = "ERROR_MESSAGE"
    }
}