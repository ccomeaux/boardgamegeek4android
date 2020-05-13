package com.boardgamegeek.db

import android.content.ContentProviderOperation
import android.net.Uri
import androidx.lifecycle.LiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.auth.AccountUtils
import com.boardgamegeek.entities.*
import com.boardgamegeek.extensions.*
import com.boardgamegeek.livedata.AbsentLiveData
import com.boardgamegeek.livedata.RegisteredLiveData
import com.boardgamegeek.provider.BggContract.*
import timber.log.Timber

class PlayDao(private val context: BggApplication) {
    enum class PlaysSortBy {
        DATE, LOCATION, GAME, LENGTH
    }

    fun loadPlays(sortBy: PlaysSortBy): LiveData<List<PlayEntity>> {
        val uri = Plays.CONTENT_URI
        return RegisteredLiveData(context, uri, true) {
            return@RegisteredLiveData loadPlays(uri, createPlaySelectionAndArgs(), sortBy)
        }
    }

    fun loadPendingPlays(): LiveData<List<PlayEntity>> {
        val uri = Plays.CONTENT_URI
        return RegisteredLiveData(context, uri, true) {
            return@RegisteredLiveData loadPlays(uri, createPendingPlaySelectionAndArgs())
        }
    }

    fun loadDraftPlays(): LiveData<List<PlayEntity>> {
        val uri = Plays.CONTENT_URI
        return RegisteredLiveData(context, uri, true) {
            return@RegisteredLiveData loadPlays(uri, createDraftPlaySelectionAndArgs())
        }
    }

    fun loadPlaysByGame(gameId: Int, sortBy: PlaysSortBy = PlaysSortBy.DATE): LiveData<List<PlayEntity>> {
        if (gameId == INVALID_ID) return AbsentLiveData.create()
        val uri = Plays.CONTENT_URI
        return RegisteredLiveData(context, uri, false) {
            return@RegisteredLiveData loadPlays(uri, createGamePlaySelectionAndArgs(gameId), sortBy)
        }
    }

    fun loadPlaysByLocation(locationName: String): LiveData<List<PlayEntity>> {
        if (locationName.isBlank()) return AbsentLiveData.create()
        val uri = Plays.CONTENT_URI
        return RegisteredLiveData(context, uri, false) {
            return@RegisteredLiveData loadPlays(uri, createLocationPlaySelectionAndArgs(locationName))
        }
    }

    fun loadPlaysByUsername(username: String): LiveData<List<PlayEntity>> {
        if (username.isBlank()) return AbsentLiveData.create()
        val uri = Plays.buildPlayersByPlayUri()
        return RegisteredLiveData(context, uri, false) {
            return@RegisteredLiveData loadPlays(uri, createUsernamePlaySelectionAndArgs(username))
        }
    }

    fun loadPlaysByPlayerName(playerName: String): LiveData<List<PlayEntity>> {
        if (playerName.isBlank()) return AbsentLiveData.create()
        val uri = Plays.buildPlayersByPlayUri()
        return RegisteredLiveData(context, uri, false) {
            return@RegisteredLiveData loadPlays(uri, createPlayerNamePlaySelectionAndArgs(playerName))
        }
    }

    private fun loadPlays(uri: Uri, selection: Pair<String?, Array<String>?>, sortBy: PlaysSortBy = PlaysSortBy.DATE): ArrayList<PlayEntity> {
        val list = arrayListOf<PlayEntity>()
        val sortOrder = when (sortBy) {
            PlaysSortBy.DATE -> ""
            PlaysSortBy.LOCATION -> Plays.LOCATION.ascending()
            PlaysSortBy.GAME -> Plays.ITEM_NAME.ascending()
            PlaysSortBy.LENGTH -> Plays.LENGTH.descending()
        }
        context.contentResolver.load(uri,
                arrayOf(Plays._ID,
                        Plays.PLAY_ID,
                        Plays.DATE,
                        Plays.OBJECT_ID,
                        Plays.ITEM_NAME,
                        Plays.QUANTITY,
                        Plays.LENGTH,
                        Plays.LOCATION,
                        Plays.INCOMPLETE,
                        Plays.NO_WIN_STATS,
                        Plays.COMMENTS,
                        Plays.SYNC_TIMESTAMP,
                        Plays.PLAYER_COUNT,
                        Plays.DIRTY_TIMESTAMP,
                        Plays.UPDATE_TIMESTAMP,
                        Plays.DELETE_TIMESTAMP,
                        Plays.START_TIME,
                        Games.THUMBNAIL_URL,
                        Games.IMAGE_URL,
                        Games.HERO_IMAGE_URL,
                        Games.UPDATED_PLAYS
                ),
                selection.first,
                selection.second,
                sortOrder
        )?.use {
            if (it.moveToFirst()) {
                do {
                    list.add(PlayEntity(
                            internalId = it.getLong(Plays._ID),
                            playId = it.getInt(Plays.PLAY_ID),
                            rawDate = it.getString(Plays.DATE),
                            gameId = it.getInt(Plays.OBJECT_ID),
                            gameName = it.getString(Plays.ITEM_NAME),
                            quantity = it.getIntOrNull(Plays.QUANTITY) ?: 1,
                            length = it.getIntOrNull(Plays.LENGTH) ?: 0,
                            location = it.getStringOrEmpty(Plays.LOCATION),
                            incomplete = it.getInt(Plays.INCOMPLETE) == 1,
                            noWinStats = it.getInt(Plays.NO_WIN_STATS) == 1,
                            comments = it.getStringOrEmpty(Plays.COMMENTS),
                            syncTimestamp = it.getLong(Plays.SYNC_TIMESTAMP),
                            playerCount = it.getInt(Plays.PLAYER_COUNT),
                            dirtyTimestamp = it.getLong(Plays.DIRTY_TIMESTAMP),
                            updateTimestamp = it.getLong(Plays.UPDATE_TIMESTAMP),
                            deleteTimestamp = it.getLong(Plays.DELETE_TIMESTAMP),
                            startTime = it.getLong(Plays.START_TIME),
                            imageUrl = it.getStringOrEmpty(Games.IMAGE_URL),
                            thumbnailUrl = it.getStringOrEmpty(Games.THUMBNAIL_URL),
                            heroImageUrl = it.getStringOrEmpty(Games.HERO_IMAGE_URL),
                            updatedPlaysTimestamp = it.getLongOrZero(Games.UPDATED_PLAYS)
                    ))
                } while (it.moveToNext())
            }
        }
        return list
    }

    private fun createPlaySelectionAndArgs() =
            Plays.DELETE_TIMESTAMP.whereZeroOrNull() to emptyArray<String>()

    private fun createPendingPlaySelectionAndArgs() =
            "${Plays.DELETE_TIMESTAMP}>0 OR ${Plays.UPDATE_TIMESTAMP}>0" to emptyArray<String>()

    private fun createDraftPlaySelectionAndArgs() =
            "${Plays.DIRTY_TIMESTAMP}>0" to emptyArray<String>()

    private fun createGamePlaySelectionAndArgs(gameId: Int) =
            "${Plays.OBJECT_ID}=? AND ${Plays.DELETE_TIMESTAMP.whereZeroOrNull()}" to arrayOf(gameId.toString())

    private fun createLocationPlaySelectionAndArgs(locationName: String) =
            "${Plays.LOCATION}=? AND ${Plays.DELETE_TIMESTAMP.whereZeroOrNull()}" to arrayOf(locationName)

    private fun createUsernamePlaySelectionAndArgs(username: String) =
            "${PlayPlayers.USER_NAME}=? AND ${Plays.DELETE_TIMESTAMP.whereZeroOrNull()}" to arrayOf(username)

    private fun createPlayerNamePlaySelectionAndArgs(playerName: String) =
            "${PlayPlayers.USER_NAME}='' AND play_players.${PlayPlayers.NAME}=? AND ${Plays.DELETE_TIMESTAMP.whereZeroOrNull()}" to arrayOf(playerName)

    fun loadPlayersForStats(includeIncompletePlays: Boolean): List<PlayerEntity> {
        val selection = arrayListOf<String>().apply {
            add(Plays.DELETE_TIMESTAMP.whereZeroOrNull())
            if (!AccountUtils.getUsername(context).isNullOrBlank()) {
                add(PlayPlayers.USER_NAME + "!=?")
            }
            if (!includeIncompletePlays) {
                add(Plays.INCOMPLETE.whereZeroOrNull())
            }
        }.joinTo(" AND ").toString()
        val selectionArgs = AccountUtils.getUsername(context)?.let { username ->
            when {
                username.isBlank() -> null
                else -> arrayOf(username)
            }
        }
        return loadPlayers(Plays.buildPlayersByUniquePlayerUri(), selection to selectionArgs, PlayerSortBy.PLAY_COUNT)
    }

    fun loadPlayersForStatsAsLiveData(includeIncompletePlays: Boolean): LiveData<List<PlayerEntity>> {
        return RegisteredLiveData(context, Plays.buildPlayersByUniquePlayerUri(), true) {
            return@RegisteredLiveData loadPlayersForStats(includeIncompletePlays)
        }
    }

    fun loadUserPlayerAsLiveData(username: String): LiveData<PlayerEntity> {
        val uri = Plays.buildPlayersByUniqueUserUri()
        return RegisteredLiveData(context, uri, true) {
            return@RegisteredLiveData loadPlayer(
                    uri,
                    "${PlayPlayers.USER_NAME}=? AND ${Plays.NO_WIN_STATS.whereZeroOrNull()}",
                    arrayOf(username))
        }
    }

    fun loadNonUserPlayerAsLiveData(playerName: String): LiveData<PlayerEntity> {
        val uri = Plays.buildPlayersByUniquePlayerUri()
        return RegisteredLiveData(context, uri, true) {
            return@RegisteredLiveData loadPlayer(
                    uri,
                    "${PlayPlayers.USER_NAME.whereEqualsOrNull()} AND play_players.${PlayPlayers.NAME}=? AND ${Plays.NO_WIN_STATS.whereZeroOrNull()}",
                    arrayOf("", playerName))
        }
    }

    private fun loadPlayer(uri: Uri, selection: String, selectionArgs: Array<String>): PlayerEntity? {
        context.contentResolver.load(
                uri,
                arrayOf(
                        PlayPlayers.NAME,
                        PlayPlayers.USER_NAME,
                        PlayPlayers.SUM_QUANTITY,
                        PlayPlayers.SUM_WINS
                ),
                selection,
                selectionArgs
        )?.use {
            return if (it.moveToFirst()) {
                PlayerEntity(
                        it.getStringOrEmpty(PlayPlayers.NAME),
                        it.getStringOrEmpty(PlayPlayers.USER_NAME),
                        it.getIntOrZero(PlayPlayers.SUM_QUANTITY),
                        it.getIntOrZero(PlayPlayers.SUM_WINS)
                )
            } else null
        }
        return null
    }

    fun loadPlayerColorsAsLiveData(playerName: String): LiveData<List<PlayerColorEntity>> {
        val uri = PlayerColors.buildPlayerUri(playerName)
        return RegisteredLiveData(context, uri, true) {
            return@RegisteredLiveData loadColors(uri)
        }
    }

    fun loadPlayerColors(playerName: String): List<PlayerColorEntity> {
        val uri = PlayerColors.buildPlayerUri(playerName)
        return loadColors(uri)
    }

    fun loadUserColorsAsLiveData(username: String): LiveData<List<PlayerColorEntity>> {
        val uri = PlayerColors.buildUserUri(username)
        return RegisteredLiveData(context, uri, true) {
            return@RegisteredLiveData loadColors(uri)
        }
    }

    fun loadUserColors(playerName: String): List<PlayerColorEntity> {
        val uri = PlayerColors.buildUserUri(playerName)
        return loadColors(uri)
    }

    private fun loadColors(uri: Uri): List<PlayerColorEntity> {
        val results = arrayListOf<PlayerColorEntity>()
        context.contentResolver.load(
                uri,
                arrayOf(
                        PlayerColors._ID,
                        PlayerColors.PLAYER_COLOR,
                        PlayerColors.PLAYER_COLOR_SORT_ORDER
                )
        )?.use {
            if (it.moveToFirst()) {
                do {
                    results += PlayerColorEntity(
                            it.getStringOrEmpty(PlayerColors.PLAYER_COLOR),
                            it.getIntOrNull(PlayerColors.PLAYER_COLOR_SORT_ORDER) ?: 0
                    )
                } while (it.moveToNext())
            }
        }
        return results
    }

    fun loadUserPlayerDetail(username: String): List<PlayerDetailEntity> {
        val uri = Plays.buildPlayerUri()
        return loadPlayerDetail(uri,
                "${PlayPlayers.USER_NAME}=?",
                arrayOf(username))
    }

    fun loadNonUserPlayerDetail(playerName: String): List<PlayerDetailEntity> {
        val uri = Plays.buildPlayerUri()
        return loadPlayerDetail(uri,
                "${PlayPlayers.USER_NAME.whereEqualsOrNull()} AND play_players.${PlayPlayers.NAME}=?",
                arrayOf("", playerName))
    }

    private fun loadPlayerDetail(uri: Uri, selection: String, selectionArgs: Array<String>): List<PlayerDetailEntity> {
        val list = mutableListOf<PlayerDetailEntity>()
        context.contentResolver.load(
                uri,
                arrayOf(
                        PlayPlayers._ID,
                        PlayPlayers.NAME,
                        PlayPlayers.USER_NAME,
                        PlayPlayers.COLOR
                ),
                selection,
                selectionArgs
        )?.use {
            if (it.moveToFirst()) {
                do {
                    list.add(PlayerDetailEntity(
                            it.getLongOrNull(PlayPlayers._ID) ?: INVALID_ID.toLong(),
                            it.getStringOrEmpty(PlayPlayers.NAME),
                            it.getStringOrEmpty(PlayPlayers.USER_NAME),
                            it.getStringOrEmpty(PlayPlayers.COLOR)
                    ))
                } while (it.moveToNext())
            } else list
        }
        return list
    }

    enum class LocationSortBy {
        NAME, PLAY_COUNT
    }

    fun loadLocationsAsLiveData(sortBy: LocationSortBy = LocationSortBy.NAME): LiveData<List<LocationEntity>> {
        return RegisteredLiveData(context, Plays.buildLocationsUri(), false) {
            return@RegisteredLiveData loadLocations(sortBy)
        }
    }

    private fun loadLocations(sortBy: LocationSortBy = LocationSortBy.NAME): List<LocationEntity> {
        val results = arrayListOf<LocationEntity>()
        val sortOrder = when (sortBy) {
            LocationSortBy.NAME -> ""
            LocationSortBy.PLAY_COUNT -> Plays.SUM_QUANTITY.descending()
        }
        context.contentResolver.load(
                Plays.buildLocationsUri(),
                arrayOf(
                        Plays._ID,
                        Plays.LOCATION,
                        Plays.SUM_QUANTITY
                ),
                sortOrder = sortOrder
        )?.use {
            if (it.moveToFirst()) {
                do {
                    results += LocationEntity(
                            it.getStringOrEmpty(Plays.LOCATION),
                            it.getIntOrNull(Plays.SUM_QUANTITY) ?: 0
                    )
                } while (it.moveToNext())
            }
        }
        return results
    }

    enum class PlayerSortBy {
        NAME, PLAY_COUNT, WIN_COUNT
    }

    fun loadPlayersAsLiveData(sortBy: PlayerSortBy = PlayerSortBy.NAME): LiveData<List<PlayerEntity>> {
        val uri = Plays.buildPlayersByUniquePlayerUri()
        return RegisteredLiveData(context, uri, false) {
            return@RegisteredLiveData loadPlayers(uri, sortBy = sortBy)
        }
    }

    private fun loadPlayers(
            uri: Uri,
            selection: Pair<String?, Array<String>?>? = null,
            sortBy: PlayerSortBy = PlayerSortBy.NAME
    ): List<PlayerEntity> {
        val results = arrayListOf<PlayerEntity>()
        val sortOrder = when (sortBy) {
            PlayerSortBy.NAME -> PlayPlayers.NAME.collateNoCase()
            PlayerSortBy.PLAY_COUNT -> Plays.SUM_QUANTITY.descending()
            PlayerSortBy.WIN_COUNT -> Plays.SUM_WINS.descending()
        }
        context.contentResolver.load(
                uri,
                arrayOf(
                        PlayPlayers.NAME,
                        PlayPlayers.USER_NAME,
                        PlayPlayers.SUM_QUANTITY,
                        PlayPlayers.SUM_WINS
                ),
                selection?.first,
                selection?.second,
                sortOrder
        )?.use {
            if (it.moveToFirst()) {
                do {
                    results += PlayerEntity(
                            it.getStringOrEmpty(PlayPlayers.NAME),
                            it.getStringOrEmpty(PlayPlayers.USER_NAME),
                            it.getIntOrNull(PlayPlayers.SUM_QUANTITY) ?: 0,
                            it.getIntOrNull(PlayPlayers.SUM_WINS) ?: 0
                    )
                } while (it.moveToNext())
            }
        }
        return results
    }

    fun loadPlayersByGame(gameId: Int): LiveData<List<PlayPlayerEntity>> {
        // be sure to exclude deleted plays!
        if (gameId == INVALID_ID) return AbsentLiveData.create()
        val uri = Plays.buildPlayersUri()
        return RegisteredLiveData(context, uri, false) {
            return@RegisteredLiveData loadPlayPlayers(uri, createGamePlaySelectionAndArgs(gameId))
        }
    }

    private fun loadPlayPlayers(
            uri: Uri,
            selection: Pair<String?, Array<String>?>? = null,
            sortBy: PlayerSortBy = PlayerSortBy.NAME
    ): List<PlayPlayerEntity> {
        val results = arrayListOf<PlayPlayerEntity>()
        val defaultSortOrder = "${PlayPlayers.START_POSITION.ascending()}, ${PlayPlayers.NAME.collateNoCase()}"
        val sortOrder = when (sortBy) {
            PlayerSortBy.NAME -> defaultSortOrder
            PlayerSortBy.PLAY_COUNT -> "${Plays.SUM_QUANTITY.descending()}, $defaultSortOrder"
            PlayerSortBy.WIN_COUNT -> "${Plays.SUM_WINS.descending()}, $defaultSortOrder"
        }
        context.contentResolver.load(
                uri,
                arrayOf(
                        PlayPlayers.NAME,
                        PlayPlayers.USER_NAME,
                        PlayPlayers.START_POSITION,
                        PlayPlayers.COLOR,
                        PlayPlayers.SCORE,
                        PlayPlayers.RATING,
                        PlayPlayers.USER_ID,
                        PlayPlayers.NEW,
                        PlayPlayers.WIN,
                        PlayPlayers.PLAY_ID
                ),
                selection?.first,
                selection?.second,
                sortOrder
        )?.use {
            if (it.moveToFirst()) {
                do {
                    results += PlayPlayerEntity(
                            it.getStringOrEmpty(PlayPlayers.NAME),
                            it.getStringOrEmpty(PlayPlayers.USER_NAME),
                            it.getStringOrEmpty(PlayPlayers.START_POSITION),
                            it.getStringOrEmpty(PlayPlayers.COLOR),
                            it.getStringOrEmpty(PlayPlayers.SCORE),
                            it.getDoubleOrZero(PlayPlayers.RATING),
                            it.getStringOrEmpty(PlayPlayers.USER_ID),
                            it.getBoolean(PlayPlayers.NEW),
                            it.getBoolean(PlayPlayers.WIN),
                            it.getIntOrNull(PlayPlayers.PLAY_ID) ?: INVALID_ID
                    )
                } while (it.moveToNext())
            }
        }
        return results
    }

    fun savePlayerColors(playerName: String, colors: List<PlayerColorEntity>?) {
        saveColors(PlayerColors.buildPlayerUri(playerName), colors)
    }

    fun saveUserColors(username: String, colors: List<PlayerColorEntity>?) {
        saveColors(PlayerColors.buildUserUri(username), colors)
    }

    private fun saveColors(uri: Uri, colors: List<PlayerColorEntity>?) {
        val resolver = context.contentResolver
        resolver.delete(uri, null, null) // TODO change to batch
        if (colors != null && colors.isNotEmpty()) {
            val batch = ArrayList<ContentProviderOperation>()
            colors.forEach {
                if (it.description.isNotBlank()) {
                    val sortUri = PlayerColors.addSortUri(uri, it.sortOrder)
                    val builder = if (context.contentResolver.rowExists(sortUri)) {
                        ContentProviderOperation.newUpdate(sortUri)
                    } else {
                        ContentProviderOperation.newInsert(uri).withValue(PlayerColors.PLAYER_COLOR_SORT_ORDER, it.sortOrder)
                    }
                    batch.add(builder.withValue(PlayerColors.PLAYER_COLOR, it.description).build())
                }
            }
            resolver.applyBatch(batch)
        }
    }

    fun deleteUnupdatedPlaysSince(syncTimestamp: Long, playDate: Long): Int {
        return deleteUnupdatedPlaysByDate(syncTimestamp, playDate, ">=")
    }

    fun deleteUnupdatedPlaysBefore(syncTimestamp: Long, playDate: Long): Int {
        return deleteUnupdatedPlaysByDate(syncTimestamp, playDate, "<=")
    }

    private fun deleteUnupdatedPlaysByDate(syncTimestamp: Long, playDate: Long, dateComparator: String): Int {
        val selection = createDeleteSelectionAndArgs(syncTimestamp)
        return context.contentResolver.delete(Plays.CONTENT_URI,
                selection.first + " AND ${Plays.DATE}$dateComparator?",
                selection.second + playDate.asDateForApi())
    }

    fun deleteUnupdatedPlays(gameId: Int, syncTimestamp: Long) {
        val selection = createDeleteSelectionAndArgs(syncTimestamp)
        val count = context.contentResolver.delete(Plays.CONTENT_URI,
                selection.first + " AND ${Plays.OBJECT_ID}=?",
                selection.second + gameId.toString())
        Timber.d("Deleted %,d unupdated play(s) of game ID=%s", count, gameId)
    }

    private fun createDeleteSelectionAndArgs(syncTimestamp: Long): Pair<String, Array<String>> {
        val selection = arrayOf(
                "${Plays.SYNC_TIMESTAMP}<?",
                Plays.UPDATE_TIMESTAMP.whereZeroOrNull(),
                Plays.DELETE_TIMESTAMP.whereZeroOrNull(),
                Plays.DIRTY_TIMESTAMP.whereZeroOrNull()
        ).joinToString(" AND ")
        val selectionArgs = arrayOf(syncTimestamp.toString())
        return selection to selectionArgs
    }

    fun createCopyPlayerColorsOperations(oldName: String, newName: String): ArrayList<ContentProviderOperation> {
        val colors = loadColors(PlayerColors.buildPlayerUri(oldName))
        val batch = arrayListOf<ContentProviderOperation>()
        colors.forEach {
            batch.add(ContentProviderOperation
                    .newInsert(PlayerColors.buildPlayerUri(newName))
                    .withValue(PlayerColors.PLAYER_COLOR, it.description)
                    .withValue(PlayerColors.PLAYER_COLOR_SORT_ORDER, it.sortOrder)
                    .build())
        }
        return batch
    }

    fun createCopyPlayerColorsToUserOperations(playerName: String, username: String): ArrayList<ContentProviderOperation> {
        val colors = loadColors(PlayerColors.buildPlayerUri(playerName))
        val batch = arrayListOf<ContentProviderOperation>()
        colors.forEach {
            batch.add(ContentProviderOperation
                    .newInsert(PlayerColors.buildUserUri(username))
                    .withValue(PlayerColors.PLAYER_COLOR, it.description)
                    .withValue(PlayerColors.PLAYER_COLOR_SORT_ORDER, it.sortOrder)
                    .build())
        }
        return batch
    }

    fun createDirtyPlaysForUserAndNickNameOperations(username: String, nickName: String, timestamp: Long = System.currentTimeMillis()): ArrayList<ContentProviderOperation> {
        val selection = createNickNameSelectionAndArgs(username, nickName)
        return createDirtyPlaysOperations(selection, timestamp)
    }

    fun createDirtyPlaysForNonUserPlayerOperations(oldName: String, timestamp: Long = System.currentTimeMillis()): ArrayList<ContentProviderOperation> {
        val selection = createNonUserPlayerSelectionAndArgs(oldName)
        return createDirtyPlaysOperations(selection, timestamp)
    }

    private fun createDirtyPlaysOperations(selection: Pair<String, Array<String>>, timestamp: Long): ArrayList<ContentProviderOperation> {
        val internalIds = context.contentResolver.queryLongs(
                Plays.buildPlayersByPlayUri(),
                Plays._ID,
                "(${selection.first}) AND ${Plays.UPDATE_TIMESTAMP.whereZeroOrNull()} AND ${Plays.DELETE_TIMESTAMP.whereZeroOrNull()} AND ${Plays.DIRTY_TIMESTAMP.whereZeroOrNull()}",
                selection.second)
        val batch = arrayListOf<ContentProviderOperation>()
        internalIds.filter { it != INVALID_ID.toLong() }.forEach {
            batch += ContentProviderOperation
                    .newUpdate(Plays.buildPlayUri(it))
                    .withValue(Plays.UPDATE_TIMESTAMP, timestamp)
                    .build()
        }
        return batch
    }

    fun createNickNameUpdateOperation(username: String, nickName: String): ContentProviderOperation {
        val selection = createNickNameSelectionAndArgs(username, nickName)
        return ContentProviderOperation
                .newUpdate(Plays.buildPlayersByPlayUri())
                .withSelection(selection.first, selection.second)
                .withValue(PlayPlayers.NAME, nickName)
                .build()
    }

    fun countNickNameUpdatePlays(username: String, nickName: String): Int {
        val selection = createNickNameSelectionAndArgs(username, nickName)
        return context.contentResolver.queryCount(
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
                .withValue(PlayPlayers.NAME, newName)
                .withSelection(selection.first, selection.second)
                .build()
    }

    fun createAddUsernameOperation(playerName: String, username: String): ContentProviderOperation {
        val selection = createNonUserPlayerSelectionAndArgs(playerName)
        return ContentProviderOperation
                .newUpdate(Plays.buildPlayersByPlayUri())
                .withValue(PlayPlayers.USER_NAME, username)
                .withSelection(selection.first, selection.second)
                .build()
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
            "${PlayPlayers.USER_NAME}=? AND play_players.${PlayPlayers.NAME}!=?" to arrayOf(username, nickName)

    /**
     * Select a player with the specified name and no username
     */
    private fun createNonUserPlayerSelectionAndArgs(playerName: String) =
            "play_players.${PlayPlayers.NAME}=? AND (${PlayPlayers.USER_NAME}=? OR ${PlayPlayers.USER_NAME} IS NULL)" to arrayOf(playerName, "")
}
