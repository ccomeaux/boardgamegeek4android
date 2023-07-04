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
        Timber.i("Begin uploading plays")

        val playsToDelete = mutableListOf<PlayEntity>()
        val playsToUpsert = mutableListOf<PlayEntity>()
        val gameIds = mutableSetOf<Int>()
        val requestedGameId = inputData.getInt(GAME_ID, BggContract.INVALID_ID)

        if (requestedGameId == BggContract.INVALID_ID) {
            Timber.i("Uploading all plays marked for deletion or updating")
            playsToDelete += playRepository.getDeletingPlays()
            playsToUpsert += playRepository.getUpdatingPlays()
        } else {
            Timber.i("Uploading all plays for game ID=$requestedGameId marked for deletion or updating")
            playsToDelete += playRepository.getDeletingPlays().filter { it.gameId == requestedGameId }
            playsToUpsert += playRepository.getUpdatingPlays().filter { it.gameId == requestedGameId }
        }

        Timber.i("Found ${playsToDelete.count()} play(s) marked for deletion")
        playsToDelete.forEach { playEntity ->
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
            val result = playRepository.upsertPlay(playEntity)
            if (result.isSuccess) {
                result.getOrNull()?.let { applicationContext.notifyLoggedPlay(it) }
                gameIds += playEntity.gameId
            } else return Result.failure(workDataOf(ERROR_MESSAGE to result.exceptionOrNull()?.message))
        }

        gameIds.filterNot { it == BggContract.INVALID_ID }.forEach { gameId ->
            playRepository.updateGamePlayCount(gameId)
        }
        playRepository.calculatePlayStats()
        return Result.success()
    }

    companion object {
        const val GAME_ID = "GAME_ID"
        const val ERROR_MESSAGE = "ERROR_MESSAGE"
    }
}