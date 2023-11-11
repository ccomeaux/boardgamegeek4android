package com.boardgamegeek.repository

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.content.contentValuesOf
import androidx.work.*
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.db.*
import com.boardgamegeek.db.PlayerColorDao.Companion.TYPE_PLAYER
import com.boardgamegeek.db.PlayerColorDao.Companion.TYPE_USER
import com.boardgamegeek.db.model.PlayBasic
import com.boardgamegeek.db.model.PlayerColorsEntity
import com.boardgamegeek.model.*
import com.boardgamegeek.model.PlayStats
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.PhpApi
import com.boardgamegeek.mappers.mapToEntity
import com.boardgamegeek.mappers.mapToModel
import com.boardgamegeek.mappers.mapToFormBodyForDelete
import com.boardgamegeek.mappers.mapToFormBodyForUpsert
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.pref.SyncPrefs.Companion.TIMESTAMP_PLAYS_NEWEST_DATE
import com.boardgamegeek.pref.SyncPrefs.Companion.TIMESTAMP_PLAYS_OLDEST_DATE
import com.boardgamegeek.pref.clearPlaysTimestamps
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import com.boardgamegeek.ui.PlayStatsActivity
import com.boardgamegeek.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.min

class PlayRepository(
    val context: Context,
    private val api: BggService,
    private val phpApi: PhpApi,
    private val playDao2: PlayDao2,
    private val playerColorDao: PlayerColorDao,
    private val userDao: UserDao,
) {
    private val playDao = PlayDao(context)
    private val gameDao = GameDao(context)
    private val collectionDao = CollectionDao(context)
    private val prefs: SharedPreferences by lazy { context.preferences() }
    private val syncPrefs: SharedPreferences by lazy { SyncPrefs.getPrefs(context.applicationContext) }

    suspend fun loadPlay(internalId: Long): Play? {
        return if (internalId == INVALID_ID.toLong()) null
        else playDao2.loadPlayWithPlayers(internalId).mapToModel()
    }

    suspend fun refreshPlay(
        internalId: Long,
        playId: Int,
        gameId: Int,
        timestamp: Long = System.currentTimeMillis()
    ): Play? =
        withContext(Dispatchers.IO) {
            val username = prefs[AccountPreferences.KEY_USERNAME, ""]
            if (username.isNullOrBlank() ||
                internalId == INVALID_ID.toLong() ||
                playId == INVALID_ID ||
                gameId == INVALID_ID
            ) {
                null
            } else {
                var page = 1
                var returnedPlay: Play?
                do {
                    val result = api.playsByGame(username, gameId, page++)
                    val plays = result.plays.mapToModel(timestamp)
                    saveFromSync(plays, timestamp)
                    Timber.i("Synced plays for game ID %s (page %,d)", gameId, page)
                    returnedPlay = plays.find { it.playId == playId }
                } while (result.hasMorePages() && returnedPlay == null)
                returnedPlay
            }
        }

    suspend fun refreshPlaysForGame(gameId: Int) = withContext(Dispatchers.Default) {
        val username = prefs[AccountPreferences.KEY_USERNAME, ""]
        if (gameId != INVALID_ID && !username.isNullOrBlank()) {
            val timestamp = System.currentTimeMillis()
            var page = 1
            do {
                val response = api.playsByGame(username, gameId, page++)
                val playsPage = response.plays.mapToModel(timestamp)
                saveFromSync(playsPage, timestamp)
            } while (response.hasMorePages())

            playDao2.deleteUnupdatedPlaysForGame(gameId, timestamp)
            gameDao.updatePlaysSyncedTimestamp(gameId, timestamp)
            calculatePlayStats()
        }
    }

    suspend fun refreshPartialPlaysForGame(gameId: Int) = withContext(Dispatchers.IO) {
        val username = prefs[AccountPreferences.KEY_USERNAME, ""]
        if (!username.isNullOrBlank()) {
            val timestamp = System.currentTimeMillis()
            val response = api.playsByGame(username, gameId, 1)
            val plays = response.plays.mapToModel(timestamp)
            saveFromSync(plays, timestamp)
            calculatePlayStats()
        }
    }

    /**
     * Upload the play to BGG. Returns the status (new, update, or error). If successful, returns the new Play ID and the new total number of plays,
     * an error message if not.
     */
    suspend fun upsertPlay(play: Play): Result<PlayUploadResult> {
        if (play.updateTimestamp == 0L)
            Result.success(PlayUploadResult.noOp(play))
        val response = phpApi.play(play.mapToFormBodyForUpsert().build())
        return if (response.hasAuthError()) {
            Authenticator.clearPassword(context)
            Result.failure(Exception(context.getString(R.string.msg_play_update_auth_error)))
        } else if (response.hasInvalidIdError()) {
            Result.failure(Exception(context.getString(R.string.msg_play_update_bad_id)))
        } else if (!response.error.isNullOrBlank()) {
            Result.failure(Exception(response.error))
        } else {
            markAsSynced(play.internalId, response.playId)
            Result.success(PlayUploadResult.upsert(play, response.playId, response.numberOfPlays))
        }
    }

    suspend fun deletePlay(play: Play): Result<PlayUploadResult> {
        if (play.deleteTimestamp == 0L)
            return Result.success(PlayUploadResult.noOp(play))
        if (play.internalId == INVALID_ID.toLong())
            return Result.success(PlayUploadResult.noOp(play))
        if (play.playId == INVALID_ID) {
            playDao2.delete(play.internalId)
            return Result.success(PlayUploadResult.delete(play))
        }
        val response = phpApi.play(play.playId.mapToFormBodyForDelete().build())
        return if (response.hasAuthError()) {
            Authenticator.clearPassword(context)
            Result.failure(Exception(context.getString(R.string.msg_play_update_auth_error)))
        } else if (response.hasInvalidIdError()) {
            playDao2.delete(play.internalId)
            Result.success(PlayUploadResult.delete(play))
        } else if (!response.error.isNullOrBlank()) {
            Result.failure(Exception(response.error))
        } else if (!response.success) {
            Result.failure(Exception(context.getString(R.string.msg_play_delete_unsuccessful)))
        } else {
            playDao2.delete(play.internalId)
            Result.success(PlayUploadResult.delete(play))
        }
    }

    suspend fun loadPlays() = playDao2.loadPlays().map { it.mapToModel() }.filterNot { it.deleteTimestamp > 0L }

    suspend fun loadUpdatingPlays() = playDao2.loadUpdatingPlays().map { it.mapToModel() }

    suspend fun loadDeletingPlays() = playDao2.loadDeletingPlays().map { it.mapToModel() }

    suspend fun loadPlaysByGame(gameId: Int): List<Play> {
        return if (gameId == INVALID_ID) emptyList()
        else playDao2.loadPlaysForGame(gameId).map { it.mapToModel() }.filterNot { it.deleteTimestamp > 0L }
    }

    suspend fun loadPlaysByLocation(location: String) =
        playDao2.loadPlaysForLocation(location).map { it.mapToModel() }.filterNot { it.deleteTimestamp > 0L }

    suspend fun loadPlaysByUsername(username: String) =
        playDao2.loadPlaysForUser(username).map { it.mapToModel() }.filterNot { it.deleteTimestamp > 0L }

    suspend fun loadPlaysByPlayerName(playerName: String) =
        playDao2.loadPlaysForPlayer(playerName).map { it.mapToModel() }.filterNot { it.deleteTimestamp > 0L }

    suspend fun loadPlaysByPlayer(name: String, gameId: Int, isUser: Boolean): List<Play> {
        if (name.isBlank() || gameId == INVALID_ID) return emptyList()
        val plays = if (isUser) {
            playDao2.loadPlaysForUserAndGame(name, gameId)
        } else {
            playDao2.loadPlaysForPlayerAndGame(name, gameId)
        }
        return plays.map { it.mapToModel() }.filterNot { it.deleteTimestamp > 0L }
    }

    suspend fun refreshPlays() = withContext(Dispatchers.IO) {
        val syncInitiatedTimestamp = System.currentTimeMillis()
        val username = prefs[AccountPreferences.KEY_USERNAME, ""]
        if (username.isNullOrBlank())
            return@withContext

        val newestTimestamp = syncPrefs[TIMESTAMP_PLAYS_NEWEST_DATE] ?: 0L
        val minDate = if (newestTimestamp == 0L) null else newestTimestamp.asDateForApi()
        var page = 1
        do {
            val response = api.plays(username, minDate, null, page++)
            val plays = response.plays.mapToModel(syncInitiatedTimestamp)
            saveFromSync(plays, syncInitiatedTimestamp)

            plays.maxOfOrNull { it.dateInMillis }?.let {
                if (it > (syncPrefs[TIMESTAMP_PLAYS_NEWEST_DATE] ?: 0L)) {
                    syncPrefs[TIMESTAMP_PLAYS_NEWEST_DATE] = it
                }
            }
            plays.minOfOrNull { it.dateInMillis }?.let {
                if (it < (syncPrefs[TIMESTAMP_PLAYS_OLDEST_DATE] ?: Long.MAX_VALUE)) {
                    syncPrefs[TIMESTAMP_PLAYS_OLDEST_DATE] = it
                }
            }

            if (minDate == null) {
                Timber.i("Synced page %,d of the newest plays (%,d plays in this page)", page - 1, plays.size)
            } else {
                Timber.i("Synced page %,d of plays from %s or later (%,d plays in this page)", page - 1, minDate, plays.size)
            }
        } while (response.hasMorePages())
        if (minDate != null) {
            deleteUnupdatedPlaysSince(syncInitiatedTimestamp, newestTimestamp)
        }

        val oldestTimestamp = syncPrefs[TIMESTAMP_PLAYS_OLDEST_DATE] ?: Long.MAX_VALUE
        if (oldestTimestamp > 0) {
            page = 1
            val maxDate = if (oldestTimestamp == Long.MAX_VALUE) null else oldestTimestamp.asDateForApi()
            do {
                val response = api.plays(username, null, maxDate, page++)
                val plays = response.plays.mapToModel(syncInitiatedTimestamp)
                saveFromSync(plays, syncInitiatedTimestamp)

                plays.minOfOrNull { it.dateInMillis }?.let {
                    if (it < (syncPrefs[TIMESTAMP_PLAYS_OLDEST_DATE] ?: Long.MAX_VALUE)) {
                        syncPrefs[TIMESTAMP_PLAYS_OLDEST_DATE] = it
                    }
                }
                if (maxDate == null) {
                    Timber.i("Synced page %,d of the oldest plays (%,d plays in this page)", page - 1, plays.size)
                } else {
                    Timber.i("Synced page %,d of plays from %s or earlier (%,d plays in this page)", page - 1, maxDate, plays.size)
                }
            } while (response.hasMorePages())
            if (oldestTimestamp != Long.MAX_VALUE) {
                deleteUnupdatedPlaysBefore(syncInitiatedTimestamp, oldestTimestamp)
            }
            syncPrefs[TIMESTAMP_PLAYS_OLDEST_DATE] = 0L
        } else {
            Timber.i("Not syncing old plays; already caught up.")
        }

        calculatePlayStats()
    }

    suspend fun deleteUnupdatedPlaysSince(syncTimestamp: Long, playDate: Long) =
        playDao2.deleteUnupdatedPlaysAfterDate(playDate.asDateForApi(), syncTimestamp)

    suspend fun deleteUnupdatedPlaysBefore(syncTimestamp: Long, playDate: Long) =
        playDao2.deleteUnupdatedPlaysBeforeDate(playDate.asDateForApi(), syncTimestamp)

    suspend fun refreshPlaysForDate(timeInMillis: Long) = withContext(Dispatchers.IO) {
        val username = prefs[AccountPreferences.KEY_USERNAME, ""]
        if (timeInMillis <= 0L && !username.isNullOrBlank()) {
            emptyList()
        } else {
            val plays = mutableListOf<Play>()
            val timestamp = System.currentTimeMillis()
            var page = 1
            do {
                val (playsPage, shouldContinue) = downloadPlays(timeInMillis, timeInMillis, page++)
                saveFromSync(playsPage, timestamp)
                plays += playsPage
                Timber.i("Synced plays for %s (page %,d)", timeInMillis.asDateForApi(), page)
            } while (shouldContinue)

            calculatePlayStats()

            plays
        }
    }

    suspend fun downloadPlays(fromDate: Long, toDate: Long, page: Int, timestamp: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        val from = if (fromDate > 0L) fromDate.asDateForApi() else null
        val to = if (toDate > 0L) toDate.asDateForApi() else null
        val username = prefs[AccountPreferences.KEY_USERNAME, ""]
        if (username.isNullOrBlank()) {
            emptyList<Play>() to false
        } else {
            val response = api.plays(username, from, to, page)
            response.plays.mapToModel(timestamp) to response.hasMorePages()
        }
    }

    suspend fun loadForStats(
        includeIncompletePlays: Boolean,
        includeExpansions: Boolean,
        includeAccessories: Boolean
    ): List<GameForPlayStats> = withContext(Dispatchers.Default) {
        val games = if (syncPrefs.isStatusSetToSync(COLLECTION_STATUS_PLAYED)) {
            gameDao.loadGamesForPlayStats(includeIncompletePlays, includeExpansions, includeAccessories).filter { it.playCount > 0 }
        } else {
            // If played games aren't synced, count the plays instead
            // We can't respect the expansion/accessory flags, so we include them all
            val allPlays = playDao2.loadPlays().filter { it.deleteTimestamp == 0L }
            val plays = if (includeIncompletePlays) allPlays else allPlays.filterNot { it.incomplete }
            val gameMap = plays.groupingBy { it.objectId to it.itemName }.fold(0) { accumulator, element ->
                accumulator + element.quantity
            }
            gameMap.map {
                GameForPlayStats(
                    id = it.key.first,
                    name = it.key.second,
                    playCount = it.value,
                )
            }
        }
        if (syncPrefs.isStatusSetToSync(COLLECTION_STATUS_OWN)) {
            val items = collectionDao.loadAll().map {
                it.second.mapToModel(it.first.mapToModel())
            }
            games.map { game ->
                val itemPairs = items.filter { it.gameId == game.id }
                game.copy(
                    isOwned = itemPairs.any { it.own },
                    bggRank = itemPairs.minOfOrNull { it.rank } ?: CollectionItem.RANK_UNKNOWN
                )
            }
        } else games
    }

    suspend fun loadPlayersByGame(gameId: Int): List<PlayPlayer> {
        if (gameId == INVALID_ID) return emptyList()
        return playDao2.loadPlayersForGame(gameId).map { it.player.mapToModel() }
    }

    suspend fun loadPlayerFavoriteColors(): Map<Player, String> {
        return playerColorDao.loadFavoritePlayerColors().associate {
            val player = if (it.playerType == PlayerColors.TYPE_USER)
                Player.createUser(it.playerName)
            else
                Player.createNonUser(it.playerName)
            player to it.playerColor
        }
    }

    suspend fun loadPlayer(name: String?, type: PlayerType): Player? {
        return when {
            name.isNullOrBlank() -> null
            type == PlayerType.USER -> playDao2.loadPlayersForUser(name).mapToModel()
            type == PlayerType.NON_USER -> playDao2.loadPlayersForPlayer(name).mapToModel()
            else -> null
        }
    }

    suspend fun loadPlayersForStats(includeIncompletePlays: Boolean): List<Player> {
        val username = context.preferences()[AccountPreferences.KEY_USERNAME, ""]
        return playDao2.loadPlayers()
            .asSequence()
            .filterNot { it.player.username == username }
            .filter { includeIncompletePlays || !it.incomplete }
            .groupBy { it.key() }
            .map { it.value.mapToModel() }
            .filterNotNull()
            .toList()
    }

    enum class PlayerType {
        USER,
        NON_USER,
    }

    suspend fun loadPlayerColors(name: String, type: PlayerType): List<PlayerColor> {
        return when (type) {
            PlayerType.USER -> loadUserColors(name)
            PlayerType.NON_USER -> loadNonUserColors(name)
        }
    }

    suspend fun loadUserColors(username: String) = playerColorDao.loadColorsForUser(username).map { it.mapToModel() }

    suspend fun loadNonUserColors(playerName: String) = playerColorDao.loadColorsForPlayer(playerName).map { it.mapToModel() }

    suspend fun savePlayerColors(name: String?, type: PlayerType, colors: List<String>?) {
        if (!name.isNullOrBlank()) {
            colors?.let { list ->
                val entities = list.filter { it.isNotBlank() }.mapIndexed { index, color ->
                    PlayerColorsEntity(
                        internalId = 0,
                        if (type == PlayerType.USER) TYPE_USER else TYPE_PLAYER,
                        name,
                        color,
                        index + 1,
                    )
                }
                playerColorDao.upsertColorsForPlayer(entities)
            }
        }
    }

    suspend fun loadPlayerUsedColors(name: String?, type: PlayerType): List<String> {
        return when {
            name.isNullOrBlank() -> emptyList()
            type == PlayerType.USER -> playDao2.loadPlayersForUser(name).mapNotNull { it.player.color }
            type == PlayerType.NON_USER -> playDao2.loadPlayersForPlayer(name).mapNotNull { it.player.color }
            else -> emptyList()
        }
    }

    enum class LocationSortBy {
        NAME, PLAY_COUNT
    }

    suspend fun loadLocations(sortBy: LocationSortBy = LocationSortBy.PLAY_COUNT): List<Location> {
        return when (sortBy) {
            LocationSortBy.PLAY_COUNT -> playDao2.loadLocations()
            LocationSortBy.NAME -> playDao2.loadLocations().sortedBy { it.name }
        }.map { it.mapToModel() }
    }

    suspend fun logQuickPlay(gameId: Int, gameName: String): Result<PlayUploadResult> {
        val playEntity = Play(
            gameId = gameId,
            gameName = gameName,
            dateInMillis = Calendar.getInstance().timeInMillis,
            updateTimestamp = System.currentTimeMillis()
        )
        val internalId = playDao.upsert(playEntity.mapToEntity())
        return logPlay(playEntity.copy(internalId = internalId))
    }

    suspend fun logPlay(play: Play): Result<PlayUploadResult> {
        return try {
            val result = upsertPlay(play)
            if (result.isSuccess) {
                updateGamePlayCount(play.gameId)
                calculatePlayStats()
            }
            result
        } catch (ex: Exception) {
            enqueueUploadRequest(play.internalId)
            return Result.failure(Exception(context.getString(R.string.msg_play_queued_for_upload), ex))
        }
    }

    fun enqueueUploadRequest(internalId: Long, tag: String? = null) {
        val workRequestBuilder = OneTimeWorkRequestBuilder<PlayUploadWorker>()
            .setInputData(workDataOf(PlayUploadWorker.INTERNAL_ID to internalId))
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(context.createWorkConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
        tag?.let { workRequestBuilder.addTag(it) }
        WorkManager.getInstance(context).enqueue(workRequestBuilder.build())
    }

    fun enqueueUploadRequest(internalIds: Collection<Long>) {
        if (internalIds.isEmpty()) return
        val workRequest = OneTimeWorkRequestBuilder<PlayUploadWorker>()
            .setInputData(workDataOf(PlayUploadWorker.INTERNAL_IDS to internalIds.toLongArray()))
            .setConstraints(context.createWorkConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }

    suspend fun saveFromSync(plays: List<Play>, syncTimestamp: Long) {
        Timber.i("Saving %d plays", plays.size)
        var updateCount = 0
        var insertCount = 0
        var unchangedCount = 0
        var dirtyCount = 0
        var errorCount = 0
        plays.forEach {
            val play = it.mapToEntity(syncTimestamp)
            when (saveFromSync(play)) {
                SaveStatus.UPDATED -> updateCount++
                SaveStatus.INSERTED -> insertCount++
                SaveStatus.DIRTY -> dirtyCount++
                SaveStatus.ERROR -> errorCount++
                SaveStatus.UNCHANGED -> unchangedCount++
            }
        }
        Timber.i(
            "Saved %1\$,d plays: updated %2$,d, inserted %3$,d, %4$,d unchanged, %5$,d dirty, %6$,d errors",
            plays.size,
            updateCount,
            insertCount,
            unchangedCount,
            dirtyCount,
            errorCount
        )
    }

    enum class SaveStatus {
        UPDATED, INSERTED, DIRTY, ERROR, UNCHANGED
    }

    private suspend fun saveFromSync(play: PlayBasic): SaveStatus = withContext(Dispatchers.IO) {
        val candidate = play.playId?.let { playDao2.loadPlay(it) }
        when {
            (play.playId == null || play.playId == 0) -> {
                Timber.i("Can't sync a play without a play ID.")
                SaveStatus.ERROR
            }
            candidate == null || candidate.internalId == INVALID_ID.toLong() -> {
                playDao.upsert(play, INVALID_ID.toLong())
                SaveStatus.INSERTED
            }
            (((candidate.dirtyTimestamp ?: 0L) > 0L || (candidate.deleteTimestamp ?: 0L) > 0L || (candidate.updateTimestamp ?: 0L) > 0L)) -> {
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
                playDao.upsert(play, candidate.internalId)
                SaveStatus.UPDATED
            }
        }
    }

    private suspend fun markAsSynced(internalId: Long, playId: Int): Boolean {
        if (internalId == INVALID_ID.toLong()) return false
        if (playId == INVALID_ID) return false
        return playDao2.markAsSynced(internalId, playId) > 0
    }

    suspend fun markAsDiscarded(internalId: Long) = playDao2.markAsDiscarded(internalId) > 0

    suspend fun markAsUpdated(internalId: Long) = playDao2.markAsUpdated(internalId) > 0

    suspend fun markAsDeleted(internalId: Long) = playDao2.markAsDeleted(internalId) > 0

    suspend fun updateGamePlayCount(gameId: Int) = withContext(Dispatchers.Default) {
        if (gameId != INVALID_ID) {
            val allPlays = loadPlaysByGame(gameId)
            val playCount = allPlays.sumOf { it.quantity }
            gameDao.updateGamePlayCount(gameId, playCount)
        }
    }

    enum class PlayerSortBy {
        NAME, PLAY_COUNT, WIN_COUNT
    }

    suspend fun loadPlayers(sortBy: PlayerSortBy = PlayerSortBy.PLAY_COUNT): List<Player> {
        val players = playDao2.loadPlayers()
        val grouping = players.groupBy { it.key() }
        val list = grouping.map { (_, value) ->
            value.firstOrNull()?.let {
                value.mapToModel()
            }
        }.filterNotNull()
        return when (sortBy) {
            PlayerSortBy.NAME -> list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, Player::name))
            PlayerSortBy.PLAY_COUNT -> list.sortedByDescending { it.playCount }
            PlayerSortBy.WIN_COUNT -> list.sortedByDescending { it.winCount }
        }
    }

    suspend fun loadPlayersByLocation(location: String = ""): List<Player> {
        val players = playDao2.loadPlayersForLocation(location)
        val grouping = players.groupBy { it.key() }
        return grouping.map { (_, value) ->
            value.mapToModel()
        }.filterNotNull().sortedByDescending { it.playCount }
    }

    suspend fun updatePlaysWithNickName(username: String, nickName: String): Collection<Long> = withContext(Dispatchers.IO) {
        val internalIds = mutableListOf<Long>()
        playDao2.loadPlayersForUser(username).forEach {
            if (it.player.name != nickName) {
                internalIds += it.player.internalPlayId
                playDao2.updateNickname(it.player.internalPlayId, it.player.internalId, nickName)
            }
        }
        internalIds
    }

    suspend fun renamePlayer(oldName: String, newName: String): Collection<Long> = withContext(Dispatchers.IO) {
        val internalIds = mutableListOf<Long>()
        playDao2.loadPlayersForPlayer(oldName).forEach {
            internalIds += it.player.internalPlayId
            playDao2.updateNickname(it.player.internalPlayId, it.player.internalId, newName)
        }

        val colors = loadNonUserColors(oldName).sortedByDescending { it.sortOrder }.map { it.description }
        savePlayerColors(newName, PlayerType.NON_USER, colors)
        playerColorDao.deleteColorsForPlayer(TYPE_PLAYER, oldName)

        internalIds
    }

    suspend fun addUsernameToPlayer(playerName: String, username: String): Collection<Long> = withContext(Dispatchers.IO) {
        val internalIds = mutableListOf<Long>()
        playDao2.loadPlayersForPlayer(playerName).forEach {
            internalIds += it.player.internalPlayId
            playDao2.updateUsername(it.player.internalPlayId, it.player.internalId, username)
        }

        val colors = loadNonUserColors(playerName).sortedByDescending { it.sortOrder }.map { it.description }
        savePlayerColors(username, PlayerType.USER, colors)
        playerColorDao.deleteColorsForPlayer(TYPE_PLAYER, playerName)

        internalIds
    }

    suspend fun renameLocation(
        oldLocationName: String,
        newLocationName: String,
    ): List<Long> = withContext(Dispatchers.IO) {
        val internalIds = mutableListOf<Long>()
        val plays = loadPlaysByLocation(oldLocationName)
        plays.forEach { play ->
            if (play.dirtyTimestamp > 0 || play.updateTimestamp > 0) {
                if (playDao2.updateLocation(play.internalId, newLocationName) > 0)
                    internalIds += play.internalId
            } else {
                if (playDao2.updateLocation(play.internalId, newLocationName, System.currentTimeMillis()) > 0)
                    internalIds += play.internalId
            }
        }
        internalIds
    }

    suspend fun save(play: Play): Long {
        val id = playDao.upsert(play.mapToEntity())

        // remember details about the play if it's being uploaded for the first time
        if (!play.isSynced && (play.updateTimestamp > 0)) {
            prefs[KEY_LAST_PLAY_DATE] = play.dateInMillis
            // if the play is "current" (for today and about to be synced), remember the location and players to be used in the next play
            val endTime = play.dateInMillis + min(60 * 24, play.length) * 60 * 1000
            val isToday = play.dateInMillis.isToday() || endTime.isToday()
            if (isToday) {
                prefs[KEY_LAST_PLAY_TIME] = System.currentTimeMillis()
                prefs[KEY_LAST_PLAY_LOCATION] = play.location
                prefs.putLastPlayPlayers(play.players)
            }

            gameDao.load(play.gameId)?.let {
                // update game's custom sort order
                play.arePlayersCustomSorted().let { // TODO only if players size > 0
                    gameDao.saveGamePlayerSortOrderToBatch(play.gameId, it)
                }

                // update game colors
                val existingColors = gameDao.loadPlayColors(play.gameId)
                play.players.distinctBy { it.color }.forEach {// TODO, just insert it with the right CONFLICT
                    if (!existingColors.contains(it.color))
                        gameDao.insertColor(play.gameId, it.color)
                }
            }

            // save nicknames for players if they don't have one already
            play.players.forEach { player ->
                if (player.username.isNotBlank() && player.name.isNotBlank()) {
                    userDao.loadUser(player.username)?.let {
                        if (it.playNickname.isNullOrBlank()) {
                            userDao.updateNickname(player.username, player.name)
                        }
                    }
                }
            }
        }

        return id
    }

    suspend fun resetPlays() {
        // resets the sync timestamps, removes the plays' hashcode, and request a sync
        syncPrefs.clearPlaysTimestamps()
        val count = playDao2.clearSyncHashCodes()
        Timber.i("Cleared the hashcode from %,d plays.", count)
        SyncPlaysWorker.requestSync(context)
    }

    suspend fun deletePlays() {
        syncPrefs.clearPlaysTimestamps()
        playDao2.deleteAll()
        gameDao.resetPlaySync()
    }

    suspend fun calculatePlayStats() = withContext(Dispatchers.Default) {
        if ((syncPrefs[TIMESTAMP_PLAYS_OLDEST_DATE, Long.MAX_VALUE] ?: Long.MAX_VALUE) == 0L) {
            val includeIncompletePlays = prefs[PlayStatPrefs.LOG_PLAY_STATS_INCOMPLETE, false] ?: false
            val includeExpansions = prefs[PlayStatPrefs.LOG_PLAY_STATS_EXPANSIONS, false] ?: false
            val includeAccessories = prefs[PlayStatPrefs.LOG_PLAY_STATS_ACCESSORIES, false] ?: false

            val games = loadForStats(includeIncompletePlays, includeExpansions, includeAccessories)
            val playStats = PlayStats(games, prefs.isStatusSetToSync(COLLECTION_STATUS_OWN))
            updateGameHIndex(playStats.hIndex)

            val players = loadPlayersForStats(includeIncompletePlays)
            val playerStats = PlayerStats(players)
            updatePlayerHIndex(playerStats.hIndex)
        }
    }

    fun updateGameHIndex(hIndex: HIndex) {
        updateHIndex(
            context,
            hIndex,
            PlayStatPrefs.KEY_GAME_H_INDEX,
            R.string.game,
            NOTIFICATION_ID_PLAY_STATS_GAME_H_INDEX
        )
    }

    fun updatePlayerHIndex(hIndex: HIndex) {
        updateHIndex(
            context,
            hIndex,
            PlayStatPrefs.KEY_PLAYER_H_INDEX,
            R.string.player,
            NOTIFICATION_ID_PLAY_STATS_PLAYER_H_INDEX
        )
    }

    private fun updateHIndex(
        context: Context,
        hIndex: HIndex,
        key: String,
        @StringRes typeResId: Int,
        notificationId: Int
    ) {
        if (hIndex.h != HIndex.INVALID_H_INDEX) {
            val old = HIndex(prefs[key, 0] ?: 0, prefs[key + PlayStatPrefs.KEY_H_INDEX_N_SUFFIX, 0] ?: 0)
            if (old != hIndex) {
                prefs[key] = hIndex.h
                prefs[key + PlayStatPrefs.KEY_H_INDEX_N_SUFFIX] = hIndex.n
                @StringRes val messageId =
                    if (hIndex.h > old.h || hIndex.h == old.h && hIndex.n < old.n) R.string.sync_notification_h_index_increase else R.string.sync_notification_h_index_decrease
                context.notify(
                    context.createNotificationBuilder(
                        R.string.title_play_stats,
                        NotificationChannels.STATS,
                        PlayStatsActivity::class.java
                    )
                        .setContentText(context.getText(messageId, context.getString(typeResId), hIndex.description))
                        .setContentIntent(
                            PendingIntent.getActivity(
                                context,
                                0,
                                Intent(context, PlayStatsActivity::class.java),
                                PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0,
                            )
                        ),
                    NotificationTags.PLAY_STATS,
                    notificationId
                )
            }
        }
    }

    companion object {
        private const val NOTIFICATION_ID_PLAY_STATS_GAME_H_INDEX = 0
        private const val NOTIFICATION_ID_PLAY_STATS_PLAYER_H_INDEX = 1
    }
}
