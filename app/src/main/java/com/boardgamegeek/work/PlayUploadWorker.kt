package com.boardgamegeek.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.boardgamegeek.entities.PlayEntity
import com.boardgamegeek.entities.PlayUploadResult
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
    private val gameIds = mutableSetOf<Int>()

    override suspend fun doWork(): Result {
        Timber.i("Begin uploading plays")

        val playsToDelete = mutableListOf<PlayEntity>()
        val playsToUpsert = mutableListOf<PlayEntity>()

        val internalId = inputData.getLong(INTERNAL_ID, BggContract.INVALID_ID.toLong())
        val requestedGameId = inputData.getInt(GAME_ID, BggContract.INVALID_ID)

        if (internalId != BggContract.INVALID_ID.toLong()) {
            Timber.i("Uploading play internal ID=$internalId")
            playRepository.loadPlay(internalId)?.let {
                if (it.deleteTimestamp > 0L) playsToDelete += it
                if (it.updateTimestamp > 0L) playsToUpsert += it
            }
        } else if (requestedGameId == BggContract.INVALID_ID) {
            Timber.i("Uploading all plays marked for deletion or updating")
            playsToDelete += playRepository.getDeletingPlays()
            playsToUpsert += playRepository.getUpdatingPlays()
        } else {
            Timber.i("Uploading all plays for game ID=$requestedGameId marked for deletion or updating")
            playsToDelete += playRepository.getDeletingPlays().filter { it.gameId == requestedGameId }
            playsToUpsert += playRepository.getUpdatingPlays().filter { it.gameId == requestedGameId }
        }

        Timber.i("Found ${playsToDelete.count()} play(s) marked for deletion")
        uploadList(playsToDelete) {
            playRepository.deletePlay(it)
        }

        Timber.i("Found ${playsToUpsert.count()} play(s) marked for upsert")
        uploadList(playsToUpsert) {
            playRepository.upsertPlay(it)
        }

        gameIds.filterNot { it == BggContract.INVALID_ID }.forEach { gameId ->
            playRepository.updateGamePlayCount(gameId)
        }
        if (gameIds.isNotEmpty())
            playRepository.calculatePlayStats()

        return Result.success()
    }

    private suspend fun uploadList(
        playsToUpsert: MutableList<PlayEntity>,
        uploadItem: suspend (item: PlayEntity) -> kotlin.Result<PlayUploadResult>
    ) : Result {
        playsToUpsert.forEach { playEntity ->
            val result = uploadItem(playEntity)
            if (result.isSuccess) {
                result.getOrNull()?.let {
                    applicationContext.notifyLoggedPlay(it)
                }
                gameIds += playEntity.gameId
            } else return Result.failure(workDataOf(ERROR_MESSAGE to result.exceptionOrNull()?.message))
        }
        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "com.boardgamegeek.UPLOAD_PLAYS"
        const val INTERNAL_ID = "INTERNAL_ID"
        const val GAME_ID = "GAME_ID"
        const val ERROR_MESSAGE = "ERROR_MESSAGE"
    }
}