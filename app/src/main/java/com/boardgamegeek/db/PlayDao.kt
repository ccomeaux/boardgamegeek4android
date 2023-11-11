package com.boardgamegeek.db

import android.content.ContentProviderOperation
import android.content.Context
import androidx.core.content.contentValuesOf
import com.boardgamegeek.db.model.*
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class PlayDao(private val context: Context) {
    suspend fun upsert(play: PlayBasic, internalId: Long = play.internalId): Long = withContext(Dispatchers.IO) {
        val batch = arrayListOf<ContentProviderOperation>()

        val values = contentValuesOf(
            Plays.Columns.PLAY_ID to play.playId,
            Plays.Columns.DATE to play.date,
            Plays.Columns.ITEM_NAME to play.itemName,
            Plays.Columns.OBJECT_ID to play.objectId,
            Plays.Columns.QUANTITY to play.quantity,
            Plays.Columns.LENGTH to play.length,
            Plays.Columns.INCOMPLETE to play.incomplete,
            Plays.Columns.NO_WIN_STATS to play.noWinStats,
            Plays.Columns.LOCATION to play.location,
            Plays.Columns.COMMENTS to play.comments,
            Plays.Columns.PLAYER_COUNT to (play.players?.size ?: 0),
            Plays.Columns.SYNC_TIMESTAMP to play.syncTimestamp,
            Plays.Columns.START_TIME to play.startTime,
            Plays.Columns.SYNC_HASH_CODE to play.generateSyncHashCode(),
            Plays.Columns.DELETE_TIMESTAMP to play.deleteTimestamp,
            Plays.Columns.UPDATE_TIMESTAMP to play.updateTimestamp,
            Plays.Columns.DIRTY_TIMESTAMP to play.dirtyTimestamp,
        )

        batch += if (internalId != INVALID_ID.toLong()) {
            ContentProviderOperation.newUpdate(Plays.buildPlayUri(internalId))
        } else {
            ContentProviderOperation.newInsert(Plays.CONTENT_URI)
        }.withValues(values).build()

        if (play.deleteTimestamp == 0L) {
            deletePlayerWithEmptyUserNameInBatch(internalId, batch)
            val existingPlayerIds = removeDuplicateUserNamesFromBatch(internalId, batch).toMutableList()
            addPlayersToBatch(play, existingPlayerIds, internalId, batch)
            removeUnusedPlayersFromBatch(internalId, existingPlayerIds, batch)

            val results = context.contentResolver.applyBatch(batch)
            var insertedId = internalId
            if (insertedId == INVALID_ID.toLong() && results.isNotEmpty()) {
                insertedId = results.getOrNull(0)?.uri?.lastPathSegment?.toLong() ?: INVALID_ID.toLong()
            }
            Timber.d("Saved play _ID=$insertedId")
            insertedId
        } else {
            Timber.i("Skipping upserting a deleted play")
            INVALID_ID.toLong()
        }
    }

    private fun deletePlayerWithEmptyUserNameInBatch(internalId: Long, batch: ArrayList<ContentProviderOperation>) {
        if (internalId == INVALID_ID.toLong()) return
        batch += ContentProviderOperation
            .newDelete(Plays.buildPlayerUri(internalId))
            .withSelection(PlayPlayers.Columns.USER_NAME.whereNullOrBlank(), null)
            .build()
    }

    private fun removeDuplicateUserNamesFromBatch(
        internalId: Long,
        batch: ArrayList<ContentProviderOperation>
    ): List<String> {
        if (internalId == INVALID_ID.toLong()) return emptyList()
        val playerUri = Plays.buildPlayerUri(internalId)

        val userNames = context.contentResolver.queryStrings(playerUri, PlayPlayers.Columns.USER_NAME).filterNot { it.isBlank() }
        if (userNames.isEmpty()) return emptyList()

        val uniqueUserNames = mutableListOf<String>()
        val userNamesToDelete = mutableListOf<String>()
        userNames.filter { it.isNotEmpty() }.forEach { userName ->
            if (uniqueUserNames.contains(userName)) {
                userNamesToDelete += userName
            } else {
                uniqueUserNames += userName
            }
        }
        userNamesToDelete.forEach { userName ->
            batch += ContentProviderOperation
                .newDelete(playerUri)
                .withSelection("${PlayPlayers.Columns.USER_NAME}=?", arrayOf(userName))
                .build()
            uniqueUserNames.remove(userName)
        }
        return uniqueUserNames
    }

    private fun addPlayersToBatch(
        play: PlayBasic,
        playerUserNames: MutableList<String>,
        internalId: Long,
        batch: ArrayList<ContentProviderOperation>
    ) {
        play.players?.forEach { player ->
            val userName = player.username
            val values = contentValuesOf(
                PlayPlayers.Columns.USER_NAME to userName,
                PlayPlayers.Columns.NAME to player.name,
                PlayPlayers.Columns.USER_ID to player.userId,
                PlayPlayers.Columns.START_POSITION to player.startingPosition,
                PlayPlayers.Columns.COLOR to player.color,
                PlayPlayers.Columns.SCORE to player.score,
                PlayPlayers.Columns.RATING to player.rating,
                PlayPlayers.Columns.NEW to player.isNew,
                PlayPlayers.Columns.WIN to player.isWin
            )
            if (playerUserNames.remove(userName)) {
                batch += ContentProviderOperation
                    .newUpdate(Plays.buildPlayerUri(internalId))
                    .withSelection("${PlayPlayers.Columns.USER_NAME}=?", arrayOf(userName))
                    .withValues(values).build()
            } else {
                batch += if (internalId == INVALID_ID.toLong()) {
                    ContentProviderOperation
                        .newInsert(Plays.buildPlayerUri())
                        .withValueBackReference(PlayPlayers.Columns._PLAY_ID, 0)
                        .withValues(values)
                        .build()
                } else {
                    ContentProviderOperation
                        .newInsert(Plays.buildPlayerUri(internalId))
                        .withValues(values)
                        .build()
                }
            }
        }
    }

    private fun removeUnusedPlayersFromBatch(
        internalId: Long,
        playerUserNames: List<String>,
        batch: ArrayList<ContentProviderOperation>
    ) {
        if (internalId == INVALID_ID.toLong()) return
        for (playerUserName in playerUserNames) {
            batch += ContentProviderOperation
                .newDelete(Plays.buildPlayerUri(internalId))
                .withSelection("${PlayPlayers.Columns.USER_NAME}=?", arrayOf(playerUserName))
                .build()
        }
    }
}
