package com.boardgamegeek.db

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns
import androidx.core.content.contentValuesOf
import androidx.core.database.getDoubleOrNull
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.boardgamegeek.db.model.*
import com.boardgamegeek.entities.*
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class PlayDao(private val context: Context) {

    //region Plays

    enum class PlaysSortBy {
        DATE, LOCATION, GAME, LENGTH, UPDATED_DATE, DELETED_DATE
    }

    suspend fun loadPlay(id: Long): PlayLocal? = withContext(Dispatchers.IO) {
        if (id != INVALID_ID.toLong()) {
            context.contentResolver.loadEntity(
                Plays.buildPlayWithGameUri(id),
                playProjection,
            ) {
                val internalId = it.getLong(0)
                val players = loadPlayersByPlay(internalId)
                playLocalFromCursor(internalId, it, players)
            }
        } else null
    }

    suspend fun loadPlays(sortBy: PlaysSortBy) = loadPlays(Plays.CONTENT_URI, Plays.Columns.DELETE_TIMESTAMP.whereZeroOrNull() to emptyArray(), sortBy)

    suspend fun loadPendingPlays() = loadPlays(Plays.CONTENT_URI, "${Plays.Columns.DELETE_TIMESTAMP}>0 OR ${Plays.Columns.UPDATE_TIMESTAMP}>0" to emptyArray())

    suspend fun loadDraftPlays() = loadPlays(Plays.CONTENT_URI, "${Plays.Columns.DIRTY_TIMESTAMP}>0" to emptyArray())

    suspend fun loadPlaysByGame(gameId: Int, sortBy: PlaysSortBy = PlaysSortBy.DATE): List<PlayLocal> {
        return if (gameId == INVALID_ID) emptyList()
        else loadPlays(Plays.CONTENT_URI, createGamePlaySelectionAndArgs(gameId), sortBy)
    }

    suspend fun loadPlaysByLocation(locationName: String?): List<PlayLocal> {
        return if (locationName == null) emptyList()
        else loadPlays(Plays.CONTENT_URI, "${Plays.Columns.LOCATION}=? AND ${Plays.Columns.DELETE_TIMESTAMP.whereZeroOrNull()}" to arrayOf(locationName))
    }

    suspend fun loadPlaysByUsername(username: String, includePlayers: Boolean = false): List<PlayLocal> {
        return if (username.isBlank()) emptyList()
        else loadPlays(Plays.buildPlayersByPlayUri(), createUsernamePlaySelectionAndArgs(username), includePlayers = includePlayers)
    }

    suspend fun loadPlaysByPlayerName(playerName: String, includePlayers: Boolean = false): List<PlayLocal> {
        return if (playerName.isBlank()) emptyList()
        else loadPlays(Plays.buildPlayersByPlayUri(), createPlayerNamePlaySelectionAndArgs(playerName), includePlayers = includePlayers)
    }

    suspend fun loadPlaysByPlayerAndGame(name: String, gameId: Int, isUser: Boolean): List<PlayLocal> {
        return if (name.isBlank() || gameId == INVALID_ID) emptyList() else {
            val uri = Plays.buildPlayersByPlayUri()
            val selection = createNamePlaySelectionAndArgs(name, isUser)
            loadPlays(uri, addGamePlaySelectionAndArgs(selection, gameId))
        }
    }

    suspend fun loadPlays(
        uri: Uri = Plays.CONTENT_URI,
        selection: Pair<String?, Array<String>?> = null to null,
        sortBy: PlaysSortBy = PlaysSortBy.DATE,
        includePlayers: Boolean = false,
    ): List<PlayLocal> {
        return withContext(Dispatchers.IO) {
            val sortOrder = when (sortBy) {
                PlaysSortBy.DATE -> ""
                PlaysSortBy.LOCATION -> Plays.Columns.LOCATION.ascending()
                PlaysSortBy.GAME -> Plays.Columns.ITEM_NAME.ascending()
                PlaysSortBy.LENGTH -> Plays.Columns.LENGTH.descending()
                PlaysSortBy.UPDATED_DATE -> Plays.Columns.UPDATE_TIMESTAMP.descending()
                PlaysSortBy.DELETED_DATE -> Plays.Columns.DELETE_TIMESTAMP.descending()
            }
            context.contentResolver.loadList(
                uri,
                playProjection,
                selection.first,
                selection.second,
                sortOrder,
            ) {
                val internalId = it.getLong(0)
                val players = if (includePlayers) loadPlayersByPlay(internalId) else null
                playLocalFromCursor(internalId, it, players)
            }
        }
    }

    private fun playLocalFromCursor(
        internalId: Long,
        it: Cursor,
        players: List<PlayPlayerLocal>?
    ) = PlayLocal(
        internalId = internalId,
        playId = it.getInt(1),
        date = it.getString(2),
        objectId = it.getInt(3),
        itemName = it.getString(4),
        quantity = it.getInt(5),
        length = it.getInt(6),
        location = it.getStringOrNull(7),
        incomplete = it.getBoolean(8),
        noWinStats = it.getBoolean(9),
        comments = it.getStringOrNull(10),
        syncTimestamp = it.getLong(11),
        initialPlayerCount = it.getIntOrNull(12) ?: 0,
        dirtyTimestamp = it.getLongOrNull(13),
        updateTimestamp = it.getLongOrNull(14),
        deleteTimestamp = it.getLongOrNull(15),
        startTime = it.getLongOrNull(16),
        gameImageUrl = it.getStringOrNull(17),
        gameThumbnailUrl = it.getStringOrNull(18),
        gameHeroImageUrl = it.getStringOrNull(19),
        gameUpdatedPlaysTimestamp = it.getLongOrNull(20) ?: 0L,
        syncHashCode = it.getIntOrNull(22),
        gameIsCustomSorted = it.getBooleanOrNull(21),
        players = players,
    )

    private val playProjection = arrayOf(
        BaseColumns._ID,
        Plays.Columns.PLAY_ID,
        Plays.Columns.DATE,
        Plays.Columns.OBJECT_ID,
        Plays.Columns.ITEM_NAME,
        Plays.Columns.QUANTITY,
        Plays.Columns.LENGTH,
        Plays.Columns.LOCATION,
        Plays.Columns.INCOMPLETE,
        Plays.Columns.NO_WIN_STATS,
        Plays.Columns.COMMENTS, // 10
        Plays.Columns.SYNC_TIMESTAMP,
        Plays.Columns.PLAYER_COUNT,
        Plays.Columns.DIRTY_TIMESTAMP,
        Plays.Columns.UPDATE_TIMESTAMP,
        Plays.Columns.DELETE_TIMESTAMP,
        Plays.Columns.START_TIME,
        Games.Columns.IMAGE_URL,
        Games.Columns.THUMBNAIL_URL,
        Games.Columns.HERO_IMAGE_URL,
        Games.Columns.UPDATED_PLAYS, // 20
        Games.Columns.CUSTOM_PLAYER_SORT,
        Plays.Columns.SYNC_HASH_CODE,
    )

    //endregion

    fun createPendingUpdatePlaySelectionAndArgs() = "${Plays.Columns.UPDATE_TIMESTAMP}>0" to emptyArray<String>()

    fun createPendingDeletePlaySelectionAndArgs() = "${Plays.Columns.DELETE_TIMESTAMP}>0" to emptyArray<String>()

    private fun createGamePlaySelectionAndArgs(gameId: Int) =
        "${Plays.Columns.OBJECT_ID}=? AND ${Plays.Columns.DELETE_TIMESTAMP.whereZeroOrNull()}" to arrayOf(gameId.toString())

    private fun createNamePlaySelectionAndArgs(name: String, isUser: Boolean) =
        if (isUser) createUsernamePlaySelectionAndArgs(name) else createPlayerNamePlaySelectionAndArgs(name)

    private fun createUsernamePlaySelectionAndArgs(username: String) =
        "${PlayPlayers.Columns.USER_NAME}=? AND ${Plays.Columns.DELETE_TIMESTAMP.whereZeroOrNull()}" to arrayOf(username)

    private fun createPlayerNamePlaySelectionAndArgs(playerName: String) =
        "${PlayPlayers.Columns.USER_NAME.whereNullOrBlank()} AND play_players.${PlayPlayers.Columns.NAME}=? AND ${Plays.Columns.DELETE_TIMESTAMP.whereZeroOrNull()}" to arrayOf(playerName)

    private fun addGamePlaySelectionAndArgs(existing: Pair<String, Array<String>>, gameId: Int) =
        "${existing.first} AND ${Plays.Columns.OBJECT_ID}=?" to (existing.second + arrayOf(gameId.toString()))

    //region Players

    private suspend fun loadPlayersByPlay(internalId: Long): List<PlayPlayerLocal> {
        return if (internalId == INVALID_ID.toLong()) emptyList()
        else loadPlayPlayers(Plays.buildPlayerUri(internalId))
    }

    suspend fun loadPlayersByGame(gameId: Int): List<PlayPlayerLocal> {
        return if (gameId == INVALID_ID) emptyList()
        else loadPlayPlayers(Plays.buildPlayersUri(), createGamePlaySelectionAndArgs(gameId))
    }

    private suspend fun loadPlayPlayers(
        uri: Uri,
        selection: Pair<String?, Array<String>?>? = null,
        sortBy: PlayerSortBy = PlayerSortBy.NAME
    ): List<PlayPlayerLocal> = withContext(Dispatchers.IO) {
        val defaultSortOrder = "${PlayPlayers.Columns.START_POSITION.ascending()}, ${PlayPlayers.Columns.NAME.collateNoCase()}"
        val sortOrder = when (sortBy) {
            PlayerSortBy.NAME -> defaultSortOrder
            PlayerSortBy.PLAY_COUNT -> "${Plays.Columns.SUM_QUANTITY.descending()}, $defaultSortOrder"
            PlayerSortBy.WIN_COUNT -> "${Plays.Columns.SUM_WINS.descending()}, $defaultSortOrder"
        }
        context.contentResolver.loadList(
            uri,
            playPlayersProjection,
            selection?.first,
            selection?.second,
            sortOrder
        ) {
            playPlayerLocalFromCursor(it)
        }
    }

    private val playPlayersProjection = arrayOf(
        BaseColumns._ID,
        PlayPlayers.Columns.NAME,
        PlayPlayers.Columns.USER_NAME,
        PlayPlayers.Columns.START_POSITION,
        PlayPlayers.Columns.COLOR,
        PlayPlayers.Columns.SCORE,
        PlayPlayers.Columns.RATING,
        PlayPlayers.Columns.USER_ID,
        PlayPlayers.Columns.NEW,
        PlayPlayers.Columns.WIN,
        Plays.Columns.PLAY_ID,
        BaseColumns._ID,
    )

    private fun playPlayerLocalFromCursor(it: Cursor) = PlayPlayerLocal(
        name = it.getStringOrNull(1),
        username = it.getStringOrNull(2),
        startingPosition = it.getStringOrNull(3),
        color = it.getStringOrNull(4),
        score = it.getStringOrNull(5),
        rating = it.getDoubleOrNull(6),
        userId = it.getIntOrNull(7),
        isNew = it.getBoolean(8),
        isWin = it.getBoolean(9),
        internalPlayId = it.getInt(10),
        internalId = it.getLong(11),
    )

   enum class PlayerSortBy {
        NAME, PLAY_COUNT, WIN_COUNT
    }

    suspend fun loadPlayers(
        sortBy: PlayerSortBy = PlayerSortBy.NAME,
        includeDeletedPlays: Boolean = true,
        includeIncompletePlays: Boolean = true,
    ): List<PlayerLocal> = withContext(Dispatchers.IO) {
        val sortOrder = when (sortBy) {
            PlayerSortBy.NAME -> PlayPlayers.Columns.NAME.collateNoCase()
            PlayerSortBy.PLAY_COUNT -> Plays.Columns.SUM_QUANTITY.descending()
            PlayerSortBy.WIN_COUNT -> Plays.Columns.SUM_WINS.descending()
        }
        val selection = arrayListOf<String>().apply {
            if (!includeDeletedPlays)
                add(Plays.Columns.DELETE_TIMESTAMP.whereZeroOrNull())
            if (!includeIncompletePlays)
                add(Plays.Columns.INCOMPLETE.whereZeroOrNull())
        }.joinTo(" AND ").toString()
        context.contentResolver.loadList(
            Plays.buildPlayersByUniquePlayerUri(),
            playerProjection,
            selection,
            null,
            sortOrder
        ) {
            playerLocalFromCursor(it)
        }
    }

    suspend fun loadUserPlayer(username: String): PlayerLocal? = withContext(Dispatchers.IO) {
        loadPlayer(
            Plays.buildPlayersByUniqueUserUri(),
            "${PlayPlayers.Columns.USER_NAME}=? AND ${Plays.Columns.NO_WIN_STATS.whereZeroOrNull()}",
            arrayOf(username)
        )
    }

    suspend fun loadNonUserPlayer(playerName: String): PlayerLocal? = withContext(Dispatchers.IO) {
        loadPlayer(
            Plays.buildPlayersByUniquePlayerUri(),
            "${PlayPlayers.Columns.USER_NAME.whereEqualsOrNull()} AND play_players.${PlayPlayers.Columns.NAME}=? AND ${Plays.Columns.NO_WIN_STATS.whereZeroOrNull()}",
            arrayOf("", playerName)
        )
    }

    private suspend fun loadPlayer(uri: Uri, selection: String, selectionArgs: Array<String>): PlayerLocal? {
        return withContext(Dispatchers.IO) {
            context.contentResolver.loadEntity(
                uri,
                playerProjection,
                selection,
                selectionArgs
            ) {
                playerLocalFromCursor(it)
            }
        }
    }

    private val playerProjection = arrayOf(
        PlayPlayers.Columns.NAME,
        PlayPlayers.Columns.USER_NAME,
        Plays.Columns.SUM_QUANTITY,
        Plays.Columns.SUM_WINS,
        Buddies.Columns.AVATAR_URL,
        PlayPlayers.Columns.UNIQUE_NAME,
    )

    private fun playerLocalFromCursor(it: Cursor) = PlayerLocal(
        name = it.getStringOrNull(0).orEmpty(),
        username = it.getStringOrNull(1).orEmpty(),
        playCount = it.getIntOrNull(2),
        winCount = it.getIntOrNull(3),
        avatar = it.getStringOrNull(4),
    )

    //endregion

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

   //region Locations

    enum class LocationSortBy {
        NAME, PLAY_COUNT
    }

    suspend fun loadLocations(sortBy: LocationSortBy = LocationSortBy.NAME): List<LocationBasic> =
        withContext(Dispatchers.IO) {
            val sortOrder = when (sortBy) {
                LocationSortBy.NAME -> ""
                LocationSortBy.PLAY_COUNT -> Plays.Columns.SUM_QUANTITY.descending()
            }
            context.contentResolver.loadList(
                Plays.buildLocationsUri(),
                arrayOf(
                    Plays.Columns.LOCATION,
                    Plays.Columns.SUM_QUANTITY
                ),
                sortOrder = sortOrder
            ) {
                LocationBasic(
                    name = it.getStringOrNull(0).orEmpty(),
                    playCount = it.getIntOrNull(1) ?: 0,
                )
            }
        }

    suspend fun loadPlayersByLocation(location: String): List<PlayerLocal> = withContext(Dispatchers.IO) {
        val selection = if (location.isNotBlank()) "${Plays.Columns.LOCATION}=?" else null
        val selectionArgs = if (location.isNotBlank()) arrayOf(location) else null
        context.contentResolver.loadList(
            Plays.buildPlayersByUniqueNameUri(),
            playerProjection,
            selection,
            selectionArgs,
            PlayPlayers.SORT_BY_SUM_QUANTITY
        ) {
            playerLocalFromCursor(it)
        }
    }

    //endregion

    suspend fun changePlayerName(play: PlayLocal, oldName: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        modifyPlayByPlayerName(play, oldName, contentValuesOf(PlayPlayers.Columns.NAME to newName))
    }

    suspend fun addUsernameToPlayer(play: PlayLocal, playerName: String, username: String) = withContext(Dispatchers.IO) {
        modifyPlayByPlayerName(play, playerName, contentValuesOf(PlayPlayers.Columns.USER_NAME to username))
    }

    private suspend fun modifyPlayByPlayerName(play: PlayLocal, playerName: String, values: ContentValues) = withContext(Dispatchers.IO) {
        play.players?.find { it.username.isNullOrBlank() && it.name == playerName }?.let { player ->
            val batch = arrayListOf<ContentProviderOperation>()
            if (play.updateTimestamp == 0L && play.dirtyTimestamp == 0L) {
                batch += ContentProviderOperation
                    .newUpdate(Plays.buildPlayUri(play.internalId))
                    .withValue(Plays.Columns.UPDATE_TIMESTAMP, System.currentTimeMillis())
                    .build()
            }
            batch += ContentProviderOperation
                .newUpdate(Plays.buildPlayerUri(play.internalId, player.internalId))
                .withValues(values)
                .build()
            context.contentResolver.applyBatch(batch)
            true
        } ?: false
    }

    suspend fun delete(internalId: Long): Boolean = withContext(Dispatchers.IO) {
        if (internalId == INVALID_ID.toLong())
            false
        else
            context.contentResolver.delete(Plays.buildPlayUri(internalId), null, null) > 0
    }

    enum class SaveStatus {
        UPDATED, INSERTED, DIRTY, ERROR, UNCHANGED
    }

    suspend fun save(play: PlayEntity, startTime: Long = System.currentTimeMillis()): SaveStatus = withContext(Dispatchers.IO) {
        val candidate = PlaySyncCandidate.find(context.contentResolver, play.playId)
        when {
            !play.isSynced -> {
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
                    contentValuesOf(Plays.Columns.SYNC_TIMESTAMP to startTime),
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

    suspend fun upsert(play: PlayEntity, internalId: Long = play.internalId): Long = withContext(Dispatchers.IO) {
        val batch = arrayListOf<ContentProviderOperation>()

        val values = contentValuesOf(
            Plays.Columns.PLAY_ID to play.playId,
            Plays.Columns.DATE to play.dateForDatabase(),
            Plays.Columns.ITEM_NAME to play.gameName,
            Plays.Columns.OBJECT_ID to play.gameId,
            Plays.Columns.QUANTITY to play.quantity,
            Plays.Columns.LENGTH to play.length,
            Plays.Columns.INCOMPLETE to play.incomplete,
            Plays.Columns.NO_WIN_STATS to play.noWinStats,
            Plays.Columns.LOCATION to play.location,
            Plays.Columns.COMMENTS to play.comments,
            Plays.Columns.PLAYER_COUNT to play.players.size,
            Plays.Columns.SYNC_TIMESTAMP to play.syncTimestamp,
            Plays.Columns.START_TIME to if (play.length > 0) 0 else play.startTime,
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

            if (play.isSynced || play.updateTimestamp > 0) {
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

    suspend fun updateAllPlays(contentValues: ContentValues): Int = withContext(Dispatchers.IO) {
        context.contentResolver.update(Plays.CONTENT_URI, contentValues, null, null)
    }

    private fun deletePlayerWithEmptyUserNameInBatch(internalId: Long, batch: ArrayList<ContentProviderOperation>) {
        if (internalId == INVALID_ID.toLong()) return
        batch += ContentProviderOperation
            .newDelete(Plays.buildPlayerUri(internalId))
            .withSelection(String.format("%1\$s IS NULL OR %1\$s=''", PlayPlayers.Columns.USER_NAME), null)
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
        play: PlayEntity,
        playerUserNames: MutableList<String>,
        internalId: Long,
        batch: ArrayList<ContentProviderOperation>
    ) {
        for (player in play.players) {
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
    private suspend fun saveGamePlayerSortOrderToBatch(play: PlayEntity, batch: ArrayList<ContentProviderOperation>) = withContext(Dispatchers.IO) {
        // We can't determine the sort order without players
        if (play.playerCount > 0) {
            // We can't save the sort order if we aren't storing the game
            val gameUri = Games.buildGameUri(play.gameId)
            if (context.contentResolver.rowExists(gameUri)) {
                batch += ContentProviderOperation
                    .newUpdate(gameUri)
                    .withValue(Games.Columns.CUSTOM_PLAYER_SORT, play.arePlayersCustomSorted())
                    .build()
            }
        }
    }

    /**
     * Add the current players' team/colors to the permanent list for the game.
     */
    private suspend fun updateColorsInBatch(play: PlayEntity, batch: ArrayList<ContentProviderOperation>) = withContext(Dispatchers.IO) {
        // There are no players, so there are no colors to save
        if (play.playerCount > 0) {
            // We can't save the colors if we aren't storing the game
            if (context.contentResolver.rowExists(Games.buildGameUri(play.gameId))) {
                val insertUri = Games.buildColorsUri(play.gameId)
                play.players.filter { it.color.isNotBlank() }.distinctBy { it.color }.forEach {
                    if (!context.contentResolver.rowExists(Games.buildColorsUri(play.gameId, it.color))) {
                        batch += ContentProviderOperation
                            .newInsert(insertUri)
                            .withValue(GameColors.Columns.COLOR, it.color)
                            .build()
                    }
                }
            }
        }
    }

    /**
     * Update GeekBuddies' nicknames with the names used here.
     */
    private suspend fun saveBuddyNicknamesToBatch(play: PlayEntity, batch: ArrayList<ContentProviderOperation>) = withContext(Dispatchers.IO) {
        play.players.filter { it.username.isNotBlank() && it.name.isNotBlank() }.forEach { player ->
            val uri = Buddies.buildBuddyUri(player.username)
            if (context.contentResolver.rowExists(uri)) {
                val nickname = context.contentResolver.queryString(uri, Buddies.Columns.PLAY_NICKNAME)
                if (nickname.isNullOrBlank()) {
                    batch += ContentProviderOperation
                        .newUpdate(Buddies.CONTENT_URI)
                        .withSelection("${Buddies.Columns.BUDDY_NAME}=?", arrayOf(player.username))
                        .withValue(Buddies.Columns.PLAY_NICKNAME, player.name)
                        .build()
                }
            }
        }
    }

    suspend fun deletePlays(): Int = withContext(Dispatchers.IO) {
        context.contentResolver.delete(Plays.CONTENT_URI, null, null)
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

    suspend fun update(internalId: Long, values: ContentValues): Boolean = withContext(Dispatchers.IO) {
        val rowsUpdated = context.contentResolver.update(Plays.buildPlayUri(internalId), values, null, null)
        if (rowsUpdated == 1) {
            Timber.d("Updated play internal ID=$internalId")
            true
        } else {
            Timber.w("Updated $rowsUpdated plays when trying to set internal ID=$internalId")
            false
        }
    }

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
            suspend fun find(resolver: ContentResolver, playId: Int): PlaySyncCandidate? = withContext(Dispatchers.IO) {
                if (playId <= 0) {
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
