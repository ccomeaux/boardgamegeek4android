package com.boardgamegeek.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.boardgamegeek.entities.PlayEntity
import com.boardgamegeek.extensions.notifyDeletedPlay
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
        val requestedGameId = inputData.getInt(PlayUpsertWorker.GAME_ID, BggContract.INVALID_ID)

        if (requestedGameId == BggContract.INVALID_ID) {
            Timber.i("Uploading all plays marked for deletion or updating")

            val playsToDelete = playRepository.getDeletingPlays()
            Timber.i("Found ${playsToDelete.count()} play(s) marked for deletion")
            playsToDelete.forEach { playEntity ->
                val (gameId, errorMessage) = deletePlayAndNotify(playEntity)
                if (errorMessage.isNotBlank())
                    return Result.failure(workDataOf(ERROR_MESSAGE to errorMessage))
                else gameIds += gameId
            }

            val playsToUpsert = playRepository.getUpdatingPlays()
            Timber.i("Found ${playsToUpsert.count()} play(s) marked for upsert")
            playsToUpsert.forEach { playEntity ->
                val (gameId, errorMessage) = upsertPlayAndNotify(playEntity)
                if (errorMessage.isNotBlank())
                    return Result.failure(workDataOf(ERROR_MESSAGE to errorMessage))
                else gameIds += gameId
            }
        } else {
            Timber.i("Uploading all plays for game ID=$requestedGameId marked for deletion or updating")

            val playsToDelete = playRepository.getDeletingPlays().filter { it.gameId == requestedGameId }
            Timber.i("Found ${playsToDelete.count()} play(s) marked for deletion")
            playsToDelete.forEach { playEntity ->
                val (gameId, errorMessage) = deletePlayAndNotify(playEntity)
                if (errorMessage.isNotBlank())
                    return Result.failure(workDataOf(ERROR_MESSAGE to errorMessage))
                else gameIds += gameId
            }

            val playsToUpsert = playRepository.getUpdatingPlays().filter { it.gameId == requestedGameId }
            Timber.i("Found ${playsToUpsert.count()} play(s) marked for upsert")
            playsToUpsert.forEach { playEntity ->
                val (gameId, errorMessage) = upsertPlayAndNotify(playEntity)
                if (errorMessage.isNotBlank())
                    return Result.failure(workDataOf(ERROR_MESSAGE to errorMessage))
                else gameIds += gameId
            }
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

    private suspend fun upsertPlayAndNotify(playEntity: PlayEntity): Pair<Int, String> {
        val result = playRepository.upsertPlay(playEntity)
        return if (result.errorMessage.isBlank()) {
            applicationContext.notifyLoggedPlay(result)
            playEntity.gameId to ""
        } else {
            BggContract.INVALID_ID to result.errorMessage
        }
    }

    companion object {
        const val ERROR_MESSAGE = "ERROR_MESSAGE"
    }
}