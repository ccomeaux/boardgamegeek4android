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
        val plays = mutableListOf<PlayEntity>()
        val gameIds = mutableSetOf<Int>()
        val internalId = inputData.getLong(INTERNAL_ID, BggContract.INVALID_ID.toLong())
        if (internalId == BggContract.INVALID_ID.toLong()) {
            Timber.i("Deleting all plays marked for deletion")
            plays += playRepository.getDeletingPlays()
        } else {
            Timber.i("Deleting play with internal ID=$internalId")
            playRepository.loadPlay(internalId)?.let { playEntity ->
                plays += playEntity
            } ?: return Result.failure(workDataOf(ERROR_MESSAGE to "Failed to load play for deletion with internal ID=$internalId"))
        }
        Timber.i("Found ${plays.count()} play(s) marked for deletion")
        plays.forEach { playEntity ->
            if (playEntity.playId == BggContract.INVALID_ID) {
                playRepository.delete(playEntity.internalId)
                gameIds += playEntity.gameId
            } else {
                val deleteResult = playRepository.deletePlay(playEntity)
                if (deleteResult.isSuccess) {
                    applicationContext.notifyDeletedPlay(deleteResult.getOrThrow())
                    gameIds += playEntity.gameId
                } else return Result.failure(workDataOf(PlayUpsertWorker.ERROR_MESSAGE to deleteResult.exceptionOrNull()?.message))
            }
        }
        gameIds.forEach { gameId ->
            playRepository.updateGamePlayCount(gameId)
        }
        playRepository.calculatePlayStats()
        return Result.success()
    }

    companion object {
        const val INTERNAL_ID = "INTERNAL_ID"
        const val ERROR_MESSAGE = "ERROR_MESSAGE"
    }
}