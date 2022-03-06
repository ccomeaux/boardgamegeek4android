package com.boardgamegeek.db

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.provider.BaseColumns
import androidx.core.content.contentValuesOf
import androidx.core.database.getDoubleOrNull
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.boardgamegeek.BggApplication
import com.boardgamegeek.entities.*
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class PlayDao(private val context: BggApplication) {
    enum class PlaysSortBy {
        DATE, LOCATION, GAME, LENGTH, UPDATED_DATE, DELETED_DATE
    }

    suspend fun loadPlay(id: Long): PlayEntity? = withContext(Dispatchers.IO) {
        context.contentResolver.load(
            Plays.buildPlayWithGameUri(id),
            arrayOf(
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
                Plays.Columns.COMMENTS,
                Plays.Columns.SYNC_TIMESTAMP,
                Plays.Columns.PLAYER_COUNT,
                Plays.Columns.DIRTY_TIMESTAMP,
                Plays.Columns.UPDATE_TIMESTAMP,
                Plays.Columns.DELETE_TIMESTAMP,
                Plays.Columns.START_TIME,
                Games.Columns.IMAGE_URL,
                Games.Columns.THUMBNAIL_URL,
                Games.Columns.HERO_IMAGE_URL,
                Games.Columns.UPDATED_PLAYS,
            ),
        )?.use {
            if (it.moveToFirst()) {
                val players = loadPlayers(id)
                val play = PlayEntity(
                    internalId = it.getLong(0),
                    playId = it.getInt(1),
                    rawDate = it.getString(2),
                    gameId = it.getInt(3),
                    gameName = it.getString(4),
                    quantity = it.getIntOrNull(5) ?: 1,
                    length = it.getIntOrNull(6) ?: 0,
                    location = it.getStringOrNull(7).orEmpty(),
                    incomplete = it.getInt(8) == 1,
                    noWinStats = it.getInt(9) == 1,
                    comments = it.getStringOrNull(10).orEmpty(),
                    syncTimestamp = it.getLong(11),
                    initialPlayerCount = it.getInt(12),
                    dirtyTimestamp = it.getLong(13),
                    updateTimestamp = it.getLong(14),
                    deleteTimestamp = it.getLong(15),
                    startTime = it.getLong(16),
                    imageUrl = it.getStringOrNull(17).orEmpty(),
                    thumbnailUrl = it.getStringOrNull(18).orEmpty(),
                    heroImageUrl = it.getStringOrNull(19).orEmpty(),
                    updatedPlaysTimestamp = it.getLongOrNull(20) ?: 0L,
                    _players = players,
                )
                play
            } else null
        }
    }

    private suspend fun loadPlayers(internalId: Long): List<PlayPlayerEntity> = withContext(Dispatchers.IO) {
        val players = mutableListOf<PlayPlayerEntity>()
        context.contentResolver.load(
            Plays.buildPlayerUri(internalId),
            arrayOf(
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
            ),
        )?.use {
            if (it.moveToFirst()) {
                do {
                    players += PlayPlayerEntity(
                        name = it.getStringOrNull(1).orEmpty(),
                        username = it.getStringOrNull(2).orEmpty(),
                        startingPosition = it.getStringOrNull(3).orEmpty(),
                        color = it.getStringOrNull(4).orEmpty(),
                        score = it.getStringOrNull(5).orEmpty(),
                        rating = it.getDoubleOrNull(6) ?: 0.0,
                        userId = it.getIntOrNull(7),
                        isNew = it.getBoolean(8),
                        isWin = it.getBoolean(9),
                        playId = it.getIntOrNull(10) ?: INVALID_ID,
                    )
                } while (it.moveToNext())
            }
        }
        players
    }

    suspend fun loadPlays(sortBy: PlaysSortBy) = loadPlays(Plays.CONTENT_URI, createPlaySelectionAndArgs(), sortBy)

    suspend fun loadPendingPlays() = loadPlays(Plays.CONTENT_URI, createPendingPlaySelectionAndArgs())

    suspend fun loadDraftPlays() = loadPlays(Plays.CONTENT_URI, createDraftPlaySelectionAndArgs())

    suspend fun loadPlaysByGame(gameId: Int, sortBy: PlaysSortBy = PlaysSortBy.DATE): List<PlayEntity> {
        return if (gameId == INVALID_ID) emptyList()
        else loadPlays(Plays.CONTENT_URI, createGamePlaySelectionAndArgs(gameId), sortBy)
    }

    suspend fun loadPlaysByLocation(locationName: String?): List<PlayEntity> {
        return if (locationName == null) emptyList()
        else loadPlays(Plays.CONTENT_URI, createLocationPlaySelectionAndArgs(locationName))
    }

    suspend fun loadPlaysByUsername(username: String): List<PlayEntity> {
        return if (username.isBlank()) emptyList()
        else loadPlays(Plays.buildPlayersByPlayUri(), createUsernamePlaySelectionAndArgs(username))
    }

    suspend fun loadPlaysByPlayerName(playerName: String): List<PlayEntity> {
        return if (playerName.isBlank()) emptyList()
        else loadPlays(Plays.buildPlayersByPlayUri(), createPlayerNamePlaySelectionAndArgs(playerName))
    }

    suspend fun loadPlaysByPlayerAndGame(name: String, gameId: Int, isUser: Boolean): List<PlayEntity> {
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
    ): List<PlayEntity> = withContext(Dispatchers.IO) {
        val list = mutableListOf<PlayEntity>()
        val sortOrder = when (sortBy) {
            PlaysSortBy.DATE -> ""
            PlaysSortBy.LOCATION -> Plays.Columns.LOCATION.ascending()
            PlaysSortBy.GAME -> Plays.Columns.ITEM_NAME.ascending()
            PlaysSortBy.LENGTH -> Plays.Columns.LENGTH.descending()
            PlaysSortBy.UPDATED_DATE -> Plays.Columns.UPDATE_TIMESTAMP.descending()
            PlaysSortBy.DELETED_DATE -> Plays.Columns.DELETE_TIMESTAMP.descending()
        }
        context.contentResolver.load(
            uri,
            arrayOf(
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
                Plays.Columns.COMMENTS,
                Plays.Columns.SYNC_TIMESTAMP,
                Plays.Columns.PLAYER_COUNT,
                Plays.Columns.DIRTY_TIMESTAMP,
                Plays.Columns.UPDATE_TIMESTAMP,
                Plays.Columns.DELETE_TIMESTAMP,
                Plays.Columns.START_TIME,
                Games.Columns.IMAGE_URL,
                Games.Columns.THUMBNAIL_URL,
                Games.Columns.HERO_IMAGE_URL,
                Games.Columns.UPDATED_PLAYS,
            ),
            selection.first,
            selection.second,
            sortOrder
        )?.use {
            if (it.moveToFirst()) {
                do {
                    val internalId = it.getLong(0)
                    val players = if (includePlayers) loadPlayers(internalId) else null
                    val play = PlayEntity(
                        internalId = internalId,
                        playId = it.getInt(1),
                        rawDate = it.getString(2),
                        gameId = it.getInt(3),
                        gameName = it.getString(4),
                        quantity = it.getIntOrNull(5) ?: 1,
                        length = it.getIntOrNull(6) ?: 0,
                        location = it.getStringOrNull(7).orEmpty(),
                        incomplete = it.getInt(8) == 1,
                        noWinStats = it.getInt(9) == 1,
                        comments = it.getStringOrNull(10).orEmpty(),
                        syncTimestamp = it.getLong(11),
                        initialPlayerCount = it.getInt(12),
                        dirtyTimestamp = it.getLong(13),
                        updateTimestamp = it.getLong(14),
                        deleteTimestamp = it.getLong(15),
                        startTime = it.getLong(16),
                        imageUrl = it.getStringOrNull(17).orEmpty(),
                        thumbnailUrl = it.getStringOrNull(18).orEmpty(),
                        heroImageUrl = it.getStringOrNull(19).orEmpty(),
                        updatedPlaysTimestamp = it.getLongOrNull(20) ?: 0L,
                        _players = players,
                    )
                    list += play
                } while (it.moveToNext())
            }
        }
        list
    }

    private fun createPlaySelectionAndArgs() =
        Plays.Columns.DELETE_TIMESTAMP.whereZeroOrNull() to emptyArray<String>()

    private fun createPendingPlaySelectionAndArgs() =
        "${Plays.Columns.DELETE_TIMESTAMP}>0 OR ${Plays.Columns.UPDATE_TIMESTAMP}>0" to emptyArray<String>()

    private fun createDraftPlaySelectionAndArgs() = "${Plays.Columns.DIRTY_TIMESTAMP}>0" to emptyArray<String>()

    fun createPendingUpdatePlaySelectionAndArgs() = "${Plays.Columns.UPDATE_TIMESTAMP}>0" to emptyArray<String>()

    fun createPendingDeletePlaySelectionAndArgs() = "${Plays.Columns.DELETE_TIMESTAMP}>0" to emptyArray<String>()

    private fun createGamePlaySelectionAndArgs(gameId: Int) =
        "${Plays.Columns.OBJECT_ID}=? AND ${Plays.Columns.DELETE_TIMESTAMP.whereZeroOrNull()}" to arrayOf(gameId.toString())

    private fun createLocationPlaySelectionAndArgs(locationName: String) =
        "${Plays.Columns.LOCATION}=? AND ${Plays.Columns.DELETE_TIMESTAMP.whereZeroOrNull()}" to arrayOf(locationName)

    private fun createNamePlaySelectionAndArgs(name: String, isUser: Boolean) =
        if (isUser) createUsernamePlaySelectionAndArgs(name) else createPlayerNamePlaySelectionAndArgs(name)

    private fun createUsernamePlaySelectionAndArgs(username: String) =
        "${PlayPlayers.Columns.USER_NAME}=? AND ${Plays.Columns.DELETE_TIMESTAMP.whereZeroOrNull()}" to arrayOf(username)

    private fun createPlayerNamePlaySelectionAndArgs(playerName: String) =
        "${PlayPlayers.Columns.USER_NAME}='' AND play_players.${PlayPlayers.Columns.NAME}=? AND ${Plays.Columns.DELETE_TIMESTAMP.whereZeroOrNull()}" to arrayOf(playerName)

    private fun addGamePlaySelectionAndArgs(existing: Pair<String, Array<String>>, gameId: Int) =
        "${existing.first} AND ${Plays.Columns.OBJECT_ID}=?" to (existing.second + arrayOf(gameId.toString()))

    suspend fun loadPlayersForStats(includeIncompletePlays: Boolean): List<PlayerEntity> {
        val username = context.preferences()[AccountPreferences.KEY_USERNAME, ""]
        val selection = arrayListOf<String>().apply {
            add(Plays.Columns.DELETE_TIMESTAMP.whereZeroOrNull())
            if (!username.isNullOrBlank()) {
                add(PlayPlayers.Columns.USER_NAME + "!=?")
            }
            if (!includeIncompletePlays) {
                add(Plays.Columns.INCOMPLETE.whereZeroOrNull())
            }
        }.joinTo(" AND ").toString()
        val selectionArgs = username?.let {
            when {
                it.isBlank() -> null
                else -> arrayOf(it)
            }
        }
        return loadPlayers(Plays.buildPlayersByUniquePlayerUri(), selection to selectionArgs, PlayerSortBy.PLAY_COUNT)
    }

    suspend fun loadUserPlayer(username: String): PlayerEntity? = withContext(Dispatchers.IO) {
        loadPlayer(
            Plays.buildPlayersByUniqueUserUri(),
            "${PlayPlayers.Columns.USER_NAME}=? AND ${Plays.Columns.NO_WIN_STATS.whereZeroOrNull()}",
            arrayOf(username)
        )
    }

    suspend fun loadNonUserPlayer(playerName: String): PlayerEntity? = withContext(Dispatchers.IO) {
        loadPlayer(
            Plays.buildPlayersByUniquePlayerUri(),
            "${PlayPlayers.Columns.USER_NAME.whereEqualsOrNull()} AND play_players.${PlayPlayers.Columns.NAME}=? AND ${Plays.Columns.NO_WIN_STATS.whereZeroOrNull()}",
            arrayOf("", playerName)
        )
    }

    private suspend fun loadPlayer(uri: Uri, selection: String, selectionArgs: Array<String>): PlayerEntity? = withContext(Dispatchers.IO) {
        context.contentResolver.load(
            uri,
            arrayOf(
                PlayPlayers.Columns.NAME,
                PlayPlayers.Columns.USER_NAME,
                Plays.Columns.SUM_QUANTITY,
                Plays.Columns.SUM_WINS
            ),
            selection,
            selectionArgs
        )?.use {
            if (it.moveToFirst()) {
                PlayerEntity(
                    name = it.getStringOrNull(0).orEmpty(),
                    username = it.getStringOrNull(1).orEmpty(),
                    playCount = it.getIntOrNull(2) ?: 0,
                    winCount = it.getIntOrNull(3) ?: 0,
                )
            } else null
        }
    }

    suspend fun loadColors(uri: Uri): List<PlayerColorEntity> = withContext(Dispatchers.IO) {
        val results = arrayListOf<PlayerColorEntity>()
        context.contentResolver.load(
            uri,
            arrayOf(
                BaseColumns._ID,
                PlayerColors.Columns.PLAYER_COLOR,
                PlayerColors.Columns.PLAYER_COLOR_SORT_ORDER
            )
        )?.use {
            if (it.moveToFirst()) {
                do {
                    results += PlayerColorEntity(
                        description = it.getStringOrNull(1).orEmpty(),
                        sortOrder = it.getIntOrNull(2) ?: 0,
                    )
                } while (it.moveToNext())
            }
        }
        results
    }

    suspend fun loadPlayerDetail(uri: Uri, selection: String, selectionArgs: Array<String>): List<PlayerDetailEntity> = withContext(Dispatchers.IO) {
        val list = mutableListOf<PlayerDetailEntity>()
        context.contentResolver.load(
            uri,
            arrayOf(
                BaseColumns._ID,
                PlayPlayers.Columns.NAME,
                PlayPlayers.Columns.USER_NAME,
                PlayPlayers.Columns.COLOR
            ),
            selection,
            selectionArgs
        )?.use {
            if (it.moveToFirst()) {
                do {
                    list += PlayerDetailEntity(
                        id = it.getLongOrNull(0) ?: INVALID_ID.toLong(),
                        name = it.getStringOrNull(1).orEmpty(),
                        username = it.getStringOrNull(2).orEmpty(),
                        color = it.getStringOrNull(3).orEmpty(),
                    )
                } while (it.moveToNext())
            } else list
        }
        list
    }

    enum class LocationSortBy {
        NAME, PLAY_COUNT
    }

    suspend fun loadLocations(sortBy: LocationSortBy = LocationSortBy.NAME): List<LocationEntity> =
        withContext(Dispatchers.IO) {
            val results = arrayListOf<LocationEntity>()
            val sortOrder = when (sortBy) {
                LocationSortBy.NAME -> ""
                LocationSortBy.PLAY_COUNT -> Plays.Columns.SUM_QUANTITY.descending()
            }
            context.contentResolver.load(
                Plays.buildLocationsUri(),
                arrayOf(
                    BaseColumns._ID,
                    Plays.Columns.LOCATION,
                    Plays.Columns.SUM_QUANTITY
                ),
                sortOrder = sortOrder
            )?.use {
                if (it.moveToFirst()) {
                    do {
                        results += LocationEntity(
                            name = it.getStringOrNull(1).orEmpty(),
                            playCount = it.getIntOrNull(2) ?: 0,
                        )
                    } while (it.moveToNext())
                }
            }
            results
        }

    suspend fun loadPlayersByLocation(location: String = ""): List<PlayerEntity> = withContext(Dispatchers.IO) {
        val results = mutableListOf<PlayerEntity>()
        val selection = if (location.isNotBlank()) "${Plays.Columns.LOCATION}=?" else null
        val selectionArgs = if (location.isNotBlank()) arrayOf(location) else null

        context.contentResolver.load(
            Plays.buildPlayersByUniqueNameUri(),
            arrayOf(
                PlayPlayers.Columns.NAME,
                PlayPlayers.Columns.USER_NAME,
                Buddies.Columns.AVATAR_URL,
                Plays.Columns.SUM_QUANTITY,
                PlayPlayers.Columns.UNIQUE_NAME
            ),
            selection,
            selectionArgs,
            PlayPlayers.SORT_BY_SUM_QUANTITY
        )?.use {
            if (it.moveToFirst()) {
                do {
                    results += PlayerEntity(
                        name = it.getStringOrNull(0).orEmpty(),
                        username = it.getStringOrNull(1).orEmpty(),
                        playCount = it.getIntOrNull(3) ?: 0,
                        rawAvatarUrl = it.getStringOrNull(2).orEmpty(),
                    )
                } while (it.moveToNext())
            }
        }
        results
    }

    /**
     * Load all non-blank colors/teams in the specified game's logged plays.
     */
    suspend fun loadPlayerColors(gameId: Int) = withContext(Dispatchers.IO) {
        context.contentResolver.queryStrings(
            Plays.buildPlayersByColor(),
            PlayPlayers.Columns.COLOR,
            "${Plays.Columns.OBJECT_ID}=?",
            arrayOf(gameId.toString()),
        ).filter { it.isNotBlank() }
    }

    enum class PlayerSortBy {
        NAME, PLAY_COUNT, WIN_COUNT
    }

    suspend fun loadPlayers(
        uri: Uri,
        selection: Pair<String?, Array<String>?>? = null,
        sortBy: PlayerSortBy = PlayerSortBy.NAME
    ): List<PlayerEntity> = withContext(Dispatchers.IO) {
        val results = mutableListOf<PlayerEntity>()
        val sortOrder = when (sortBy) {
            PlayerSortBy.NAME -> PlayPlayers.Columns.NAME.collateNoCase()
            PlayerSortBy.PLAY_COUNT -> Plays.Columns.SUM_QUANTITY.descending()
            PlayerSortBy.WIN_COUNT -> Plays.Columns.SUM_WINS.descending()
        }
        context.contentResolver.load(
            uri,
            arrayOf(
                PlayPlayers.Columns.NAME,
                PlayPlayers.Columns.USER_NAME,
                Plays.Columns.SUM_QUANTITY,
                Plays.Columns.SUM_WINS,
                Buddies.Columns.AVATAR_URL,
            ),
            selection?.first,
            selection?.second,
            sortOrder
        )?.use {
            if (it.moveToFirst()) {
                do {
                    results += PlayerEntity(
                        name = it.getStringOrNull(0).orEmpty(),
                        username = it.getStringOrNull(1).orEmpty(),
                        playCount = it.getIntOrNull(2) ?: 0,
                        winCount = it.getIntOrNull(3) ?: 0,
                        rawAvatarUrl = it.getStringOrNull(4).orEmpty(),
                    )
                } while (it.moveToNext())
            }
        }
        results
    }

    suspend fun delete(internalId: Long): Boolean = withContext(Dispatchers.IO) {
        context.contentResolver.delete(Plays.buildPlayUri(internalId), null, null) > 0
    }

    enum class SaveStatus {
        UPDATED, INSERTED, DIRTY, ERROR, UNCHANGED
    }

    suspend fun save(play: PlayEntity, startTime: Long = System.currentTimeMillis()): SaveStatus {
        val candidate = PlaySyncCandidate.find(context.contentResolver, play.playId)
        return when {
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
            Timber.i("Skipping inserting a deleted play")
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

    suspend fun loadPlayersByGame(gameId: Int): List<PlayPlayerEntity> = withContext(Dispatchers.IO) {
        if (gameId == INVALID_ID) emptyList()
        else loadPlayPlayers(Plays.buildPlayersUri(), createGamePlaySelectionAndArgs(gameId))
    }

    private suspend fun loadPlayPlayers(
        uri: Uri,
        selection: Pair<String?, Array<String>?>? = null,
        sortBy: PlayerSortBy = PlayerSortBy.NAME
    ): List<PlayPlayerEntity> = withContext(Dispatchers.IO) {
        val results = mutableListOf<PlayPlayerEntity>()
        val defaultSortOrder = "${PlayPlayers.Columns.START_POSITION.ascending()}, ${PlayPlayers.Columns.NAME.collateNoCase()}"
        val sortOrder = when (sortBy) {
            PlayerSortBy.NAME -> defaultSortOrder
            PlayerSortBy.PLAY_COUNT -> "${Plays.Columns.SUM_QUANTITY.descending()}, $defaultSortOrder"
            PlayerSortBy.WIN_COUNT -> "${Plays.Columns.SUM_WINS.descending()}, $defaultSortOrder"
        }
        context.contentResolver.load(
            uri,
            arrayOf(
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
            ),
            selection?.first,
            selection?.second,
            sortOrder
        )?.use {
            if (it.moveToFirst()) {
                do {
                    results += PlayPlayerEntity(
                        name = it.getStringOrNull(0).orEmpty(),
                        username = it.getStringOrNull(1).orEmpty(),
                        startingPosition = it.getStringOrNull(2).orEmpty(),
                        color = it.getStringOrNull(3).orEmpty(),
                        score = it.getStringOrNull(4).orEmpty(),
                        rating = it.getDoubleOrNull(5) ?: 0.0,
                        userId = it.getIntOrNull(6),
                        isNew = it.getBoolean(7),
                        isWin = it.getBoolean(8),
                        playId = it.getIntOrNull(9) ?: INVALID_ID,
                    )
                } while (it.moveToNext())
            }
        }
        results
    }

    suspend fun saveColorsForPlayer(uri: Uri, colors: List<PlayerColorEntity>?) = withContext(Dispatchers.IO) {
        val batch = arrayListOf<ContentProviderOperation>()
        batch += ContentProviderOperation.newDelete(uri).build()
        colors?.filter { it.description.isNotBlank() }?.forEach {
            batch += ContentProviderOperation
                .newInsert(uri).withValue(PlayerColors.Columns.PLAYER_COLOR_SORT_ORDER, it.sortOrder)
                .withValue(PlayerColors.Columns.PLAYER_COLOR, it.description)
                .build()
        }
        context.contentResolver.applyBatch(batch)
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

    suspend fun createCopyPlayerColorsOperations(
        oldName: String,
        newName: String
    ): ArrayList<ContentProviderOperation> {
        val colors = loadColors(PlayerColors.buildPlayerUri(oldName))
        val batch = arrayListOf<ContentProviderOperation>()
        colors.forEach {
            batch += ContentProviderOperation
                .newInsert(PlayerColors.buildPlayerUri(newName))
                .withValue(PlayerColors.Columns.PLAYER_COLOR, it.description)
                .withValue(PlayerColors.Columns.PLAYER_COLOR_SORT_ORDER, it.sortOrder)
                .build()
        }
        return batch
    }

    suspend fun createCopyPlayerColorsToUserOperations(
        playerName: String,
        username: String
    ): ArrayList<ContentProviderOperation> {
        val colors = loadColors(PlayerColors.buildPlayerUri(playerName))
        val batch = arrayListOf<ContentProviderOperation>()
        colors.forEach {
            batch += ContentProviderOperation
                .newInsert(PlayerColors.buildUserUri(username))
                .withValue(PlayerColors.Columns.PLAYER_COLOR, it.description)
                .withValue(PlayerColors.Columns.PLAYER_COLOR_SORT_ORDER, it.sortOrder)
                .build()
        }
        return batch
    }

    fun createDirtyPlaysForUserAndNickNameOperations(
        username: String,
        nickName: String,
        timestamp: Long = System.currentTimeMillis()
    ): ArrayList<ContentProviderOperation> {
        val selection = createNickNameSelectionAndArgs(username, nickName)
        return createDirtyPlaysOperations(selection, timestamp)
    }

    fun createDirtyPlaysForNonUserPlayerOperations(
        oldName: String,
        timestamp: Long = System.currentTimeMillis()
    ): ArrayList<ContentProviderOperation> {
        val selection = createNonUserPlayerSelectionAndArgs(oldName)
        return createDirtyPlaysOperations(selection, timestamp)
    }

    private fun createDirtyPlaysOperations(
        selection: Pair<String, Array<String>>,
        timestamp: Long
    ): ArrayList<ContentProviderOperation> {
        val internalIds = context.contentResolver.queryLongs(
            Plays.buildPlayersByPlayUri(),
            BaseColumns._ID,
            "(${selection.first}) AND ${Plays.Columns.UPDATE_TIMESTAMP.whereZeroOrNull()} AND ${Plays.Columns.DELETE_TIMESTAMP.whereZeroOrNull()} AND ${Plays.Columns.DIRTY_TIMESTAMP.whereZeroOrNull()}",
            selection.second
        )
        val batch = arrayListOf<ContentProviderOperation>()
        internalIds.filter { it != INVALID_ID.toLong() }.forEach {
            batch += ContentProviderOperation
                .newUpdate(Plays.buildPlayUri(it))
                .withValue(Plays.Columns.UPDATE_TIMESTAMP, timestamp)
                .build()
        }
        return batch
    }

    fun createNickNameUpdateOperation(username: String, nickName: String): ContentProviderOperation {
        val selection = createNickNameSelectionAndArgs(username, nickName)
        return ContentProviderOperation
            .newUpdate(Plays.buildPlayersByPlayUri())
            .withSelection(selection.first, selection.second)
            .withValue(PlayPlayers.Columns.NAME, nickName)
            .build()
    }

    suspend fun countNickNameUpdatePlays(username: String, nickName: String): Int = withContext(Dispatchers.IO) {
        val selection = createNickNameSelectionAndArgs(username, nickName)
        context.contentResolver.queryCount(
            Plays.buildPlayersByPlayUri(),
            selection.first,
            selection.second
        )
    }

    /**
     * Change player records from the old name to the new name (username  must be blank)
     */
    fun createRenameUpdateOperation(oldName: String, newName: String): ContentProviderOperation {
        val selection = createNonUserPlayerSelectionAndArgs(oldName)
        return ContentProviderOperation
            .newUpdate(Plays.buildPlayersByPlayUri())
            .withValue(PlayPlayers.Columns.NAME, newName)
            .withSelection(selection.first, selection.second)
            .build()
    }

    fun createAddUsernameOperation(playerName: String, username: String): ContentProviderOperation {
        val selection = createNonUserPlayerSelectionAndArgs(playerName)
        return ContentProviderOperation
            .newUpdate(Plays.buildPlayersByPlayUri())
            .withValue(PlayPlayers.Columns.USER_NAME, username)
            .withSelection(selection.first, selection.second)
            .build()
    }

    suspend fun update(internalId: Long, values: ContentValues) = withContext(Dispatchers.IO) {
        val rowsUpdated = context.contentResolver.update(Plays.buildPlayUri(internalId), values, null, null)
        if (rowsUpdated == 1) {
            Timber.d("Updated play _ID=$internalId")
        } else {
            Timber.w("Upserted $rowsUpdated plays when trying to set _ID=$internalId")
        }
    }

    /**
     * Create an operation to delete the colors of the specified player
     */
    fun createDeletePlayerColorsOperation(playerName: String): ContentProviderOperation {
        return ContentProviderOperation.newDelete(PlayerColors.buildPlayerUri(playerName)).build()
    }

    /**
     * Select a player with the specified username AND nick name
     */
    private fun createNickNameSelectionAndArgs(username: String, nickName: String) =
        "${PlayPlayers.Columns.USER_NAME}=? AND play_players.${PlayPlayers.Columns.NAME}!=?" to arrayOf(username, nickName)

    /**
     * Select a player with the specified name and no username
     */
    private fun createNonUserPlayerSelectionAndArgs(playerName: String) =
        "play_players.${PlayPlayers.Columns.NAME}=? AND (${PlayPlayers.Columns.USER_NAME}=? OR ${PlayPlayers.Columns.USER_NAME} IS NULL)" to arrayOf(
            playerName,
            ""
        )

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
            fun find(resolver: ContentResolver, playId: Int): PlaySyncCandidate? {
                if (playId <= 0) {
                    Timber.i("Can't sync a play without a play ID.")
                    return null
                }
                val cursor = resolver.query(
                    Plays.CONTENT_URI,
                    arrayOf(
                        BaseColumns._ID,
                        Plays.Columns.SYNC_HASH_CODE,
                        Plays.Columns.DELETE_TIMESTAMP,
                        Plays.Columns.UPDATE_TIMESTAMP,
                        Plays.Columns.DIRTY_TIMESTAMP
                    ),
                    "${Plays.Columns.PLAY_ID}=?",
                    arrayOf(playId.toString()),
                    null
                )
                return cursor?.use {
                    if (it.moveToFirst()) {
                        PlaySyncCandidate(
                            internalId = it.getLongOrNull(0) ?: INVALID_ID.toLong(),
                            syncHashCode = it.getIntOrNull(1) ?: 0,
                            deleteTimestamp = it.getLongOrNull(2) ?: 0L,
                            updateTimestamp = it.getLongOrNull(3) ?: 0L,
                            dirtyTimestamp = it.getLongOrNull(4) ?: 0L,
                        )
                    } else {
                        null
                    }
                }
            }
        }
    }
}
