package com.boardgamegeek.db

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns
import androidx.core.content.contentValuesOf
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.boardgamegeek.db.model.*
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class PlayDao(private val context: Context) {
    //region Player Colors

    suspend fun loadAllPlayerColors() = loadColors(PlayerColors.CONTENT_URI)

    suspend fun loadUserColors(username: String) = loadColors(PlayerColors.buildUserUri(username))

    suspend fun loadNonUserColors(playerName: String) = loadColors(PlayerColors.buildPlayerUri(playerName))

    private suspend fun loadColors(uri: Uri): List<PlayerColorsLocal> {
        return withContext(Dispatchers.IO) {
            context.contentResolver.loadList(
                uri,
                playerColorsProjection,
            ) {
                playerColorsFromCursor(it)
            }
        }
    }

    private val playerColorsProjection = arrayOf(
        BaseColumns._ID,
        PlayerColors.Columns.PLAYER_COLOR,
        PlayerColors.Columns.PLAYER_COLOR_SORT_ORDER,
        PlayerColors.Columns.PLAYER_TYPE,
        PlayerColors.Columns.PLAYER_NAME,
    )

    private fun playerColorsFromCursor(it: Cursor) = PlayerColorsLocal(
        internalId = it.getInt(0),
        playerType = it.getInt(3),
        playerName = it.getString(4),
        playerColor = it.getString(1),
        playerColorSortOrder = it.getInt(2),
    )

    suspend fun deleteColorsForPlayer(playerName: String): Int = withContext(Dispatchers.IO) {
        context.contentResolver.delete(PlayerColors.buildPlayerUri(playerName), null, null)
    }

    suspend fun saveUserColors(username: String, colors: List<String>?) {
        saveColorsForPlayer(PlayerColors.buildUserUri(username), colors)
    }

    suspend fun saveNonUserColors(playerName: String, colors: List<String>?) {
        saveColorsForPlayer(PlayerColors.buildPlayerUri(playerName), colors)
    }

    private suspend fun saveColorsForPlayer(uri: Uri, colors: List<String>?) = withContext(Dispatchers.IO) {
        val batch = arrayListOf<ContentProviderOperation>()
        batch += ContentProviderOperation.newDelete(uri).build()
        var sortOrder = 1
        colors?.filter { it.isNotBlank() }?.forEach {
            batch += ContentProviderOperation
                .newInsert(uri)
                .withValue(PlayerColors.Columns.PLAYER_COLOR_SORT_ORDER, sortOrder++)
                .withValue(PlayerColors.Columns.PLAYER_COLOR, it)
                .build()
        }
        context.contentResolver.applyBatch(batch)
    }

    suspend fun loadUserUsedColors(username: String) = loadPlayerUsedColors("${PlayPlayers.Columns.USER_NAME}=?", arrayOf(username))

    suspend fun loadNonUserUsedColors(playerName: String) = loadPlayerUsedColors("${PlayPlayers.Columns.USER_NAME.whereEqualsOrNull()} AND play_players.${PlayPlayers.Columns.NAME}=?", arrayOf("", playerName))

    private suspend fun loadPlayerUsedColors(selection: String, selectionArgs: Array<String>): List<String> = withContext(Dispatchers.IO) {
        context.contentResolver.loadList(
            Plays.buildPlayerUri(),
            arrayOf(
                BaseColumns._ID,
                PlayPlayers.Columns.NAME,
                PlayPlayers.Columns.USER_NAME,
                PlayPlayers.Columns.COLOR,
            ),
            selection,
            selectionArgs
        ) {
            it.getStringOrNull(3).orEmpty()
        }
    }

    /**
     * Load all non-blank colors/teams in the specified game's logged plays.
     */
    suspend fun loadPlayerColorsForGame(gameId: Int) = withContext(Dispatchers.IO) {
        context.contentResolver.queryStrings(
            Plays.buildPlayersByColor(),
            PlayPlayers.Columns.COLOR,
            "${Plays.Columns.OBJECT_ID}=?",
            arrayOf(gameId.toString()),
        ).filter { it.isNotBlank() }
    }

    //endregion

    enum class SaveStatus {
        UPDATED, INSERTED, DIRTY, ERROR, UNCHANGED
    }

    suspend fun saveFromSync(play: PlayBasic): SaveStatus = withContext(Dispatchers.IO) {
        val candidate = PlaySyncCandidate.find(context.contentResolver, play.playId)
        when {
            (play.playId == null || play.playId == 0) -> {
                Timber.i("Can't sync a play without a play ID.")
                SaveStatus.ERROR
            }
            candidate == null || candidate.internalId == INVALID_ID.toLong() -> {
                upsert(play, INVALID_ID.toLong())
                SaveStatus.INSERTED
            }
            candidate.isDirty -> {
                Timber.i("Not saving during the sync; local play is modified.")
                SaveStatus.DIRTY
            }
            candidate.syncHashCode == play.generateSyncHashCode() -> {
                context.contentResolver.update(
                    Plays.buildPlayUri(candidate.internalId),
                    contentValuesOf(Plays.Columns.SYNC_TIMESTAMP to play.syncTimestamp),
                    null,
                    null
                )
                SaveStatus.UNCHANGED
            }
            else -> {
                upsert(play, candidate.internalId)
                SaveStatus.UPDATED
            }
        }
    }

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

            if ((play.playId != null && play.playId > 0) ||
                (play.updateTimestamp != null && play.updateTimestamp > 0)
            ) {
                // Do these when a new play is ready to be synced
                saveGamePlayerSortOrderToBatch(play, batch)
                updateColorsInBatch(play, batch)
                saveBuddyNicknamesToBatch(play, batch)
            }

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

    /**
     * Determine if the players are custom sorted or not, and save it to the game.
     */
    private suspend fun saveGamePlayerSortOrderToBatch(play: PlayBasic, batch: ArrayList<ContentProviderOperation>) = withContext(Dispatchers.IO) {
        play.arePlayersCustomSorted()?.let {
            val gameUri = Games.buildGameUri(play.objectId)
            if (context.contentResolver.rowExists(gameUri)) {
                batch += ContentProviderOperation
                    .newUpdate(gameUri)
                    .withValue(Games.Columns.CUSTOM_PLAYER_SORT, it)
                    .build()
            }
        }
    }

    /**
     * Add the current players' team/colors to the permanent list for the game.
     */
    private suspend fun updateColorsInBatch(play: PlayBasic, batch: ArrayList<ContentProviderOperation>) = withContext(Dispatchers.IO) {
        if (context.contentResolver.rowExists(Games.buildGameUri(play.objectId))) {
            play.players?.distinctBy { it.color }?.forEach {
                if (it.color != null &&
                    !context.contentResolver.rowExists(Games.buildColorsUri(play.objectId, it.color))) {
                    batch += ContentProviderOperation
                        .newInsert(Games.buildColorsUri(play.objectId))
                        .withValue(GameColors.Columns.COLOR, it.color)
                        .build()
                }
            }
        }
    }

    /**
     * Update GeekBuddies' nicknames with the names used here.
     */
    private suspend fun saveBuddyNicknamesToBatch(play: PlayBasic, batch: ArrayList<ContentProviderOperation>) = withContext(Dispatchers.IO) {
        play.players?.forEach { player ->
            if (!player.username.isNullOrBlank() && !player.name.isNullOrBlank()) {
                val uri = Users.buildUserUri(player.username)
                if (context.contentResolver.rowExists(uri, Users.Columns.USERNAME)) {
                    val nickname = context.contentResolver.queryString(uri, Users.Columns.PLAY_NICKNAME)
                    if (nickname.isNullOrBlank()) {
                        batch += ContentProviderOperation
                            .newUpdate(Users.CONTENT_URI)
                            .withSelection("${Users.Columns.USERNAME}=?", arrayOf(player.username))
                            .withValue(Users.Columns.PLAY_NICKNAME, player.name)
                            .build()
                    }
                }
            }
        }
    }

    //region Deletes

    suspend fun deleteAllPlays(): Int = withContext(Dispatchers.IO) {
        context.contentResolver.delete(Plays.CONTENT_URI, null, null)
    }

    suspend fun delete(internalId: Long): Boolean = withContext(Dispatchers.IO) {
        if (internalId == INVALID_ID.toLong())
            false
        else
            context.contentResolver.delete(Plays.buildPlayUri(internalId), null, null) > 0
    }

    suspend fun deleteUnupdatedPlaysByDate(syncTimestamp: Long, playDate: Long, dateComparator: String): Int = withContext(Dispatchers.IO) {
        val selection = createDeleteSelectionAndArgs(syncTimestamp)
        context.contentResolver.delete(
            Plays.CONTENT_URI,
            "${selection.first} AND ${Plays.Columns.DATE}$dateComparator?",
            selection.second + playDate.asDateForApi()
        )
    }

    suspend fun deleteUnupdatedPlays(gameId: Int, syncTimestamp: Long) = withContext(Dispatchers.IO) {
        val selection = createDeleteSelectionAndArgs(syncTimestamp)
        val count = context.contentResolver.delete(
            Plays.CONTENT_URI,
            "${selection.first} AND ${Plays.Columns.OBJECT_ID}=?",
            selection.second + gameId.toString()
        )
        Timber.d("Deleted %,d unupdated play(s) of game ID=%s", count, gameId)
    }

    private fun createDeleteSelectionAndArgs(syncTimestamp: Long): Pair<String, Array<String>> {
        val selection = arrayOf(
            "${Plays.Columns.SYNC_TIMESTAMP}<?",
            Plays.Columns.UPDATE_TIMESTAMP.whereZeroOrNull(),
            Plays.Columns.DELETE_TIMESTAMP.whereZeroOrNull(),
            Plays.Columns.DIRTY_TIMESTAMP.whereZeroOrNull()
        ).joinToString(" AND ")
        val selectionArgs = arrayOf(syncTimestamp.toString())
        return selection to selectionArgs
    }

    //endregion

    //region Upserts

    suspend fun clearSyncHashCodes(): Int = withContext(Dispatchers.IO) {
        context.contentResolver.update(Plays.CONTENT_URI, contentValuesOf(Plays.Columns.SYNC_HASH_CODE to 0), null, null)
    }

    suspend fun updateLocation(internalId: Long, locationName: String?, dirtyTimestamp: Long, updateTimestamp: Long): Boolean {
        val values = contentValuesOf(Plays.Columns.LOCATION to locationName)
        if (dirtyTimestamp > 0 || updateTimestamp > 0) {
            values.put(Plays.Columns.UPDATE_TIMESTAMP, System.currentTimeMillis())
        }
        return update(internalId, values)
    }

    private suspend fun update(internalId: Long, values: ContentValues): Boolean = withContext(Dispatchers.IO) {
        val rowsUpdated = context.contentResolver.update(Plays.buildPlayUri(internalId), values, null, null)
        if (rowsUpdated == 1) {
            Timber.d("Updated play internal ID=$internalId")
            true
        } else {
            Timber.w("Updated $rowsUpdated plays when trying to set internal ID=$internalId")
            false
        }
    }

    //endregion

    data class PlaySyncCandidate(
        val internalId: Long = INVALID_ID.toLong(),
        val syncHashCode: Int = 0,
        private val deleteTimestamp: Long = 0L,
        private val updateTimestamp: Long = 0L,
        private val dirtyTimestamp: Long = 0L,
    ) {
        val isDirty: Boolean
            get() = dirtyTimestamp > 0 || deleteTimestamp > 0 || updateTimestamp > 0

        companion object {
            suspend fun find(resolver: ContentResolver, playId: Int?): PlaySyncCandidate? = withContext(Dispatchers.IO) {
                if (playId == null || playId <= 0) {
                    Timber.i("Can't sync a play without a play ID.")
                    null
                } else {
                    resolver.loadEntity(
                        Plays.CONTENT_URI,
                        arrayOf(
                            BaseColumns._ID,
                            Plays.Columns.SYNC_HASH_CODE,
                            Plays.Columns.DELETE_TIMESTAMP,
                            Plays.Columns.UPDATE_TIMESTAMP,
                            Plays.Columns.DIRTY_TIMESTAMP,
                        ),
                        "${Plays.Columns.PLAY_ID}=?",
                        arrayOf(playId.toString()),
                    ) {
                        PlaySyncCandidate(
                            internalId = it.getLongOrNull(0) ?: INVALID_ID.toLong(),
                            syncHashCode = it.getIntOrNull(1) ?: 0,
                            deleteTimestamp = it.getLongOrNull(2) ?: 0L,
                            updateTimestamp = it.getLongOrNull(3) ?: 0L,
                            dirtyTimestamp = it.getLongOrNull(4) ?: 0L,
                        )
                    }
                }
            }
        }
    }
}
