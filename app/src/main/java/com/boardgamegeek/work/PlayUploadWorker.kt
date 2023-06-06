package com.boardgamegeek.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.boardgamegeek.R
import com.boardgamegeek.entities.PlayUploadResult
import com.boardgamegeek.extensions.asRangeDescription
import com.boardgamegeek.extensions.getText
import com.boardgamegeek.extensions.notifyLoggedPlay
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.PlayRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class PlayUploadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val playRepository: PlayRepository,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val internalId = inputData.getLong(INTERNAL_ID, BggContract.INVALID_ID.toLong())
        playRepository.loadPlay(internalId)?.let { playEntity ->
            val uploadResult = playRepository.uploadPlay(playEntity)
            if (uploadResult.errorMessage.isBlank()) {
                playRepository.updateGamePlayCount(playEntity.gameId)
                playRepository.calculatePlayStats()
                applicationContext.notifyLoggedPlay(uploadResult)
                return Result.success() // TODO include uploadResult?
            } else {
                return Result.failure(workDataOf(ERROR_MESSAGE to uploadResult.errorMessage))
            }
        }
        return Result.failure(workDataOf(ERROR_MESSAGE to "Failed to load play for upload with internal ID=$internalId"))
    }

    companion object {
        const val INTERNAL_ID = "INTERNAL_ID"
        const val ERROR_MESSAGE = "ERROR_MESSAGE"
    }
}