package com.boardgamegeek.repository

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.StringRes
import androidx.work.*
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.db.*
import com.boardgamegeek.db.PlayerColorDao.Companion.TYPE_PLAYER
import com.boardgamegeek.db.PlayerColorDao.Companion.TYPE_USER
import com.boardgamegeek.db.model.GameColorsEntity
import com.boardgamegeek.db.model.PlayerColorsEntity
import com.boardgamegeek.model.*
import com.boardgamegeek.model.PlayStats
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.PhpApi
import com.boardgamegeek.io.model.PlaysResponse
import com.boardgamegeek.io.safeApiCall
import com.boardgamegeek.mappers.mapToEntity
import com.boardgamegeek.mappers.mapToModel
import com.boardgamegeek.mappers.mapToFormBodyForDelete
import com.boardgamegeek.mappers.mapToFormBodyForUpsert
import com.boardgamegeek.model.Location.Companion.applySort
import com.boardgamegeek.model.Player.Companion.applySort
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.pref.SyncPrefs.Companion.TIMESTAMP_PLAYS_NEWEST_DATE
import com.boardgamegeek.pref.SyncPrefs.Companion.TIMESTAMP_PLAYS_OLDEST_DATE
import com.boardgamegeek.pref.clearPlaysTimestamps
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import com.boardgamegeek.ui.PlayStatsActivity
import com.boardgamegeek.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.min

class PlayRepository(
    val context: Context,
    private val api: BggService,
    private val phpApi: PhpApi,
    private val playDao: PlayDao,
    private val playerColorDao: PlayerColorDao,
    private val userDao: UserDao,
    private val gameColorDao: GameColorDao,
    private val gameDao: GameDao,
    private val collectionDao: CollectionDao,
) {
    enum class PlayerType {
        USER,
        NON_USER,
    }

    private val prefs: SharedPreferences by lazy { context.preferences() }
    private val syncPrefs: SharedPreferences by lazy { SyncPrefs.getPrefs(context.applicationContext) }

    // region Load

    suspend fun loadPlays(): List<Play> = withContext(Dispatchers.Default) {
        withContext(Dispatchers.IO) { playDao.loadPlays() }.map { it.mapToModel() }.filterNot { it.deleteTimestamp > 0L }
    }

    fun loadPlaysFlow(): Flow<List<Play>> {
        return playDao.loadPlaysFlow()
            .map {
                it.map { entity -> entity.mapToModel() }.filterNot { play -> play.deleteTimestamp > 0L }
            }
            .flowOn(Dispatchers.Default)
            .conflate()
    }

    suspend fun loadUpdatingPlays(): List<Play> = withContext(Dispatchers.Default) {
        withContext(Dispatchers.IO) { playDao.loadUpdatingPlays() }.map { it.mapToModel() }
    }

    suspend fun loadDeletingPlays(): List<Play> = withContext(Dispatchers.Default) {
        withContext(Dispatchers.IO) { playDao.loadDeletingPlays() }.map { it.mapToModel() }
    }

    suspend fun loadPlay(internalId: Long): Play? = withContext(Dispatchers.Default) {
        if (internalId == INVALID_ID.toLong()) null
        else withContext(Dispatchers.IO) { playDao.loadPlayWithPlayers(internalId) }?.mapToModel()
    }

    fun loadPlayFlow(internalId: Long): Flow<Play?> {
        return playDao.loadPlayWithPlayersFlow(internalId).map { it?.mapToModel() }.flowOn(Dispatchers.Default)
    }

    suspend fun loadPlaysByGame(gameId: Int): List<Play> = withContext(Dispatchers.Default) {
        if (gameId == INVALID_ID) emptyList()
        else withContext(Dispatchers.IO) { playDao.loadPlaysForGame(gameId) }
            .map { it.mapToModel() }
            .filterNot { it.deleteTimestamp > 0L }
    }

    suspend fun loadPlaysByLocation(location: String): List<Play> = withContext(Dispatchers.Default) {
        withContext(Dispatchers.IO) { playDao.loadPlaysForLocation(location) }
            .map { it.mapToModel() }
            .filterNot { it.deleteTimestamp > 0L }
    }

    suspend fun loadPlaysByUsername(username: String): List<Play> = withContext(Dispatchers.Default) {
        withContext(Dispatchers.IO) { playDao.loadPlaysForUser(username) }
            .map { it.mapToModel() }
            .filterNot { it.deleteTimestamp > 0L }
    }

    suspend fun loadPlaysByPlayerName(playerName: String): List<Play> = withContext(Dispatchers.Default) {
        withContext(Dispatchers.IO) { playDao.loadPlaysForPlayer(playerName) }
            .map { it.mapToModel() }
            .filterNot { it.deleteTimestamp > 0L }
    }

    suspend fun loadPlaysByPlayer(name: String, gameId: Int, isUser: Boolean): List<Play> = withContext(Dispatchers.Default) {
        if (name.isBlank() || gameId == INVALID_ID) emptyList()
        else {
            val plays = withContext(Dispatchers.IO) {
                if (isUser) {
                    playDao.loadPlaysForUserAndGame(name, gameId)
                } else {
                    playDao.loadPlaysForPlayerAndGame(name, gameId)
                }
            }
            plays.map { it.mapToModel() }.filterNot { it.deleteTimestamp > 0L }
        }
    }

    suspend fun loadPlayers(sortBy: Player.SortType = Player.SortType.PLAY_COUNT): List<Player> = withContext(Dispatchers.Default) {
        val players = withContext(Dispatchers.IO) { playDao.loadPlayers() }
        val grouping = players.groupBy { it.key() }
        grouping.map { (_, value) ->
            value.firstOrNull()?.let {
                value.mapToModel()
            }
        }
            .filterNotNull()
            .applySort(sortBy)
    }

    fun loadPlayersFlow(sortBy: Player.SortType = Player.SortType.PLAY_COUNT): Flow<List<Player>> {
        return playDao.loadPlayersFlow()
            .map { list ->
                list.groupBy { player -> player.key() }
                    .map { (_, value) ->
                        value.firstOrNull()?.let {
                            value.mapToModel()
                        }
                    }
                    .filterNotNull()
                    .applySort(sortBy)
            }
    }

    suspend fun loadPlayersByLocation(location: String = ""): List<Player> = withContext(Dispatchers.Default) {
        val players = withContext(Dispatchers.IO) { playDao.loadPlayersForLocation(location) }
        val grouping = players.groupBy { it.key() }
        grouping.map { (_, value) -> value.mapToModel() }
            .filterNotNull()
            .sortedByDescending { it.playCount }
    }

    suspend fun loadPlayersByGame(gameId: Int): List<PlayPlayer> = withContext(Dispatchers.Default) {
        if (gameId == INVALID_ID) emptyList()
        else withContext(Dispatchers.IO) { playDao.loadPlayersForGame(gameId) }.map { it.player.mapToModel() }
    }

    suspend fun loadPlayer(name: String?, type: PlayerType): Player? = withContext(Dispatchers.Default) {
        withContext(Dispatchers.IO) {
            when {
                name.isNullOrBlank() -> null
                type == PlayerType.USER -> playDao.loadPlayersForUser(name)
                type == PlayerType.NON_USER -> playDao.loadPlayersForPlayer(name)
                else -> null
            }
        }?.mapToModel()
    }

    suspend fun loadPlayerFavoriteColors(): Map<Player, String> = withContext(Dispatchers.IO) {
        playerColorDao.loadFavoritePlayerColors().associate {
            val player = when (it.playerType) {
                PlayerColorsEntity.TYPE_USER -> Player.createUser(it.playerName)
                else -> Player.createNonUser(it.playerName)
            }
            player to it.playerColor
        }
    }

    suspend fun loadPlayerColors(name: String, type: PlayerType): List<PlayerColor> {
        return when (type) {
            PlayerType.USER -> loadUserColors(name)
            PlayerType.NON_USER -> loadNonUserColors(name)
        }
    }

    suspend fun loadUserColors(username: String) = withContext(Dispatchers.Default) {
        withContext(Dispatchers.IO) { playerColorDao.loadColorsForUser(username).map { it.mapToModel() } }
    }

    suspend fun loadNonUserColors(playerName: String) = withContext(Dispatchers.Default) {
        withContext(Dispatchers.IO) { playerColorDao.loadColorsForPlayer(playerName).map { it.mapToModel() } }
    }

    suspend fun loadPlayerUsedColors(name: String?, type: PlayerType): List<String> = withContext(Dispatchers.Default) {
        withContext(Dispatchers.IO) {
            when {
                name.isNullOrBlank() -> emptyList()
                type == PlayerType.USER -> playDao.loadPlayersForUser(name)
                type == PlayerType.NON_USER -> playDao.loadPlayersForPlayer(name)
                else -> emptyList()
            }
        }.mapNotNull { it.player.color }
    }

    suspend fun loadLocations(sortBy: Location.SortType = Location.SortType.PLAY_COUNT): List<Location> = withContext(Dispatchers.Default) {
        withContext(Dispatchers.IO) { playDao.loadLocations() }
            .map { it.mapToModel() }
            .sortedWith(
                when (sortBy) {
                    Location.SortType.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                    Location.SortType.PLAY_COUNT -> compareByDescending<Location> { it.playCount }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                }
            )
    }

    fun loadLocationsFlow(sortBy: Location.SortType = Location.SortType.PLAY_COUNT): Flow<List<Location>> {
        return playDao.loadLocationsFlow()
            .map { it.map { location -> location.mapToModel() }.applySort(sortBy) }
            .flowOn(Dispatchers.Default)
            .conflate()
    }

    // endregion

    // region Download

    suspend fun refreshPlay(
        play: Play,
        timestamp: Long = System.currentTimeMillis()
    ): String? = withContext(Dispatchers.Default) {
        val username = prefs[AccountPreferences.KEY_USERNAME, ""].orEmpty()
        if (username.isNotBlank() && play.internalId != INVALID_ID.toLong() && play.playId != INVALID_ID && play.gameId != INVALID_ID) {
            var page = 1
            var result: Result<PlaysResponse>?
            do {
                result = safeApiCall(context) { api.playsByGame(username, play.gameId, page++) }
                if (result.isSuccess) {
                    val plays = result.getOrNull()?.plays.mapToModel(timestamp)
                    saveFromSync(plays, timestamp)
                    Timber.i("Synced plays for game ID %s (page %,d)", play.gameId, page)
                    if (plays.any { it.playId == play.playId })
                        break
                } else {
                    return@withContext result.exceptionOrNull()?.localizedMessage ?: "Error"
                }
            } while (result?.getOrNull()?.hasMorePages() == true)
        }
        null
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

            playDao.deleteUnupdatedPlaysForGame(gameId, timestamp)
            gameDao.updatePlayTimestamp(gameId, timestamp)
            calculateStats()
        }
    }

    suspend fun refreshPartialPlaysForGame(gameId: Int) = withContext(Dispatchers.IO) {
        val username = prefs[AccountPreferences.KEY_USERNAME, ""]
        if (!username.isNullOrBlank()) {
            val timestamp = System.currentTimeMillis()
            val response = api.playsByGame(username, gameId, 1)
            val plays = response.plays.mapToModel(timestamp)
            saveFromSync(plays, timestamp)
            calculateStats()
        }
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

        calculateStats()
    }

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

            calculateStats()

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

    // endregion

    // region Log

    suspend fun logQuickPlay(gameId: Int, gameName: String): Result<PlayUploadResult> {
        val play = Play(
            gameId = gameId,
            gameName = gameName,
            dateInMillis = Calendar.getInstance().timeInMillis,
            updateTimestamp = System.currentTimeMillis()
        )
        val internalId = upsert(play)
        return tryUpload(play.copy(internalId = internalId))
    }

    suspend fun logPlay(play: Play): Long = withContext(Dispatchers.IO) {
        val id = upsert(play)

        if (play.updateTimestamp > 0L) tryUpload(play)

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

            gameDao.loadGame(play.gameId)?.let {
                // update game's custom sort order
                if (play.players.isNotEmpty()) {
                    gameDao.updateCustomPlayerSort(play.gameId, play.arePlayersCustomSorted())
                }

                // update game colors
                val existingColors = gameColorDao.loadColorsForGame(play.gameId).map { it.color }
                play.players.distinctBy { it.color }.forEach {// TODO just insert it with the right CONFLICT
                    if (!existingColors.contains(it.color)) {
                        gameColorDao.insert(listOf(GameColorsEntity(internalId = 0L, gameId = play.gameId, it.color)))
                    }
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

        id
    }

    // endregion

    // region Upload

    private suspend fun tryUpload(play: Play): Result<PlayUploadResult> {
        return try {
            val result = uploadPlay(play)
            if (result.isSuccess) {
                updateGamePlayCount(play.gameId)
                calculateStats()
            }
            result
        } catch (ex: Exception) {
            enqueueUploadRequest(play.internalId)
            return Result.failure(Exception(context.getString(R.string.msg_play_queued_for_upload), ex))
        }
    }

    /**
     * Upload the play to BGG. Returns the status (new, update, or error). If successful, returns the new Play ID and the new total number of plays,
     * an error message if not.
     */
    suspend fun uploadPlay(play: Play): Result<PlayUploadResult> {
        if (play.updateTimestamp == 0L)
            return Result.success(PlayUploadResult.noOp(play))
        val result = safeApiCall(context) { phpApi.play(play.mapToFormBodyForUpsert().build()) }
        result.exceptionOrNull()?.let {
            return Result.failure(it)
        }
        val response = result.getOrNull()
        return when {
            response == null -> {
                Result.failure(Exception(context.getString(R.string.msg_play_update_null_response)))
            }
            response.hasAuthError() -> {
                Authenticator.clearPassword(context)
                Result.failure(Exception(context.getString(R.string.msg_play_update_auth_error)))
            }
            response.hasInvalidIdError() -> {
                Result.failure(Exception(context.getString(R.string.msg_play_update_bad_id)))
            }
            !response.error.isNullOrBlank() -> {
                Result.failure(Exception(response.error))
            }
            else -> {
                markAsSynced(play.internalId, response.playId)
                updateGamePlayCount(play.gameId)
                calculateStats()
                Result.success(PlayUploadResult.upsert(play, response.playId, response.numberOfPlays))
            }
        }
    }

    suspend fun deletePlay(play: Play): Result<PlayUploadResult> {
        if (play.deleteTimestamp == 0L)
            return Result.success(PlayUploadResult.noOp(play))
        if (play.internalId == INVALID_ID.toLong())
            return Result.success(PlayUploadResult.noOp(play))
        if (play.playId == INVALID_ID) {
            withContext(Dispatchers.IO) { playDao.delete(play.internalId) }
            return Result.success(PlayUploadResult.delete(play))
        }
        val result = safeApiCall(context) { phpApi.play(play.playId.mapToFormBodyForDelete().build()) }
        result.exceptionOrNull()?.let {
            return Result.failure(it)
        }
        val response = result.getOrNull()
        return when {
            response == null -> {
                Result.failure(Exception(context.getString(R.string.msg_play_update_null_response)))
            }
            response.hasAuthError() -> {
                Authenticator.clearPassword(context)
                Result.failure(Exception(context.getString(R.string.msg_play_update_auth_error)))
            }
            response.hasInvalidIdError() -> {
                playDao.delete(play.internalId)
                Result.success(PlayUploadResult.delete(play))
            }
            !response.error.isNullOrBlank() -> {
                Result.failure(Exception(response.error))
            }
            !response.success -> {
                Result.failure(Exception(context.getString(R.string.msg_play_delete_unsuccessful)))
            }
            else -> {
                playDao.delete(play.internalId)
                Result.success(PlayUploadResult.delete(play))
            }
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

    // endregion

    // region Save

    suspend fun saveFromSync(plays: List<Play>, syncTimestamp: Long) {
        Timber.i("Saving %d plays", plays.size)
        var updateCount = 0
        var insertCount = 0
        var unchangedCount = 0
        var dirtyCount = 0
        var errorCount = 0
        plays.forEach { play ->
            if (play.playId == INVALID_ID || play.playId == 0) {
                Timber.i("Can't sync a play without a play ID.")
                errorCount++
            }
            val candidate = play.playId.let { playDao.loadPlay(it) }
            when {
                candidate == null || candidate.internalId == INVALID_ID.toLong() -> {
                    upsert(play, syncTimestamp)
                    insertCount++
                }
                (((candidate.dirtyTimestamp ?: 0L) > 0L || (candidate.deleteTimestamp ?: 0L) > 0L || (candidate.updateTimestamp ?: 0L) > 0L)) -> {
                    Timber.i("Not saving during the sync; local play is modified.")
                    dirtyCount++
                }
                candidate.syncHashCode == play.generateSyncHashCode() -> {
                    playDao.updateSyncTimestamp(candidate.internalId, syncTimestamp)
                    unchangedCount++
                }
                else -> {
                    upsert(play.copy(internalId = candidate.internalId), syncTimestamp)
                    updateCount++
                }
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

    private suspend fun markAsSynced(internalId: Long, playId: Int): Boolean = withContext(Dispatchers.IO) {
        if (internalId == INVALID_ID.toLong() || playId == INVALID_ID) false
        else playDao.markAsSynced(internalId, playId) > 0
    }

    suspend fun markAsDiscarded(internalId: Long) = withContext(Dispatchers.IO) {
        if (internalId == INVALID_ID.toLong()) false
        else playDao.markAsDiscarded(internalId) > 0
    }

    suspend fun markAsUpdated(internalId: Long) = withContext(Dispatchers.IO) {
        if (internalId == INVALID_ID.toLong()) false
        else playDao.markAsUpdated(internalId) > 0
    }

    suspend fun markAsDeleted(internalId: Long) = withContext(Dispatchers.IO) {
        if (internalId == INVALID_ID.toLong()) false
        else playDao.markAsDeleted(internalId) > 0
    }

    suspend fun updateGamePlayCount(gameId: Int) = withContext(Dispatchers.Default) {
        if (gameId != INVALID_ID) {
            val allPlays = loadPlaysByGame(gameId)
            val playCount = allPlays.sumOf { it.quantity }
            withContext(Dispatchers.IO) { gameDao.updatePlayCount(gameId, playCount) }
        }
    }

    suspend fun updatePlaysWithNickName(username: String, nickName: String): Collection<Long> = withContext(Dispatchers.IO) {
        val internalIds = mutableListOf<Long>()
        playDao.loadPlayersForUser(username).forEach {
            if (it.player.name != nickName) {
                internalIds += it.player.internalPlayId
                playDao.updateNickname(it.player.internalPlayId, it.player.internalId, nickName)
            }
        }
        internalIds
    }

    suspend fun renamePlayer(oldName: String, newName: String): Collection<Long> = withContext(Dispatchers.IO) {
        val internalIds = mutableListOf<Long>()
        playDao.loadPlayersForPlayer(oldName).forEach {
            internalIds += it.player.internalPlayId
            playDao.updateNickname(it.player.internalPlayId, it.player.internalId, newName)
        }

        val colors = loadNonUserColors(oldName).sortedByDescending { it.sortOrder }.map { it.description }
        savePlayerColors(newName, PlayerType.NON_USER, colors)
        playerColorDao.deleteColorsForPlayer(TYPE_PLAYER, oldName)

        internalIds
    }

    suspend fun addUsernameToPlayer(playerName: String, username: String): Collection<Long> = withContext(Dispatchers.IO) {
        val internalIds = mutableListOf<Long>()
        playDao.loadPlayersForPlayer(playerName).forEach {
            internalIds += it.player.internalPlayId
            playDao.updateUsername(it.player.internalPlayId, it.player.internalId, username)
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
                if (playDao.updateLocation(play.internalId, newLocationName) > 0)
                    internalIds += play.internalId
            } else {
                if (playDao.updateLocation(play.internalId, newLocationName, System.currentTimeMillis()) > 0)
                    internalIds += play.internalId
            }
        }
        internalIds
    }

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

    private suspend fun upsert(play: Play, syncTimestamp: Long = 0L) = withContext(Dispatchers.IO) {
        playDao.upsert(play.mapToEntity(syncTimestamp), play.players.map { it.mapToEntity() })
    }

    // endregion

    suspend fun resetPlays() {
        // resets the sync timestamps, removes the plays' hashcode, and request a sync
        syncPrefs.clearPlaysTimestamps()
        val count = playDao.clearSyncHashCodes()
        Timber.i("Cleared the hashcode from %,d plays.", count)
        SyncPlaysWorker.requestSync(context)
    }

    // region Delete

    suspend fun deleteUnupdatedPlaysSince(syncTimestamp: Long, playDate: Long) = withContext(Dispatchers.IO) {
        playDao.deleteUnupdatedPlaysAfterDate(playDate.asDateForApi(), syncTimestamp)
    }

    suspend fun deleteUnupdatedPlaysBefore(syncTimestamp: Long, playDate: Long) = withContext(Dispatchers.IO) {
        playDao.deleteUnupdatedPlaysBeforeDate(playDate.asDateForApi(), syncTimestamp)
    }

    suspend fun deletePlays() = withContext(Dispatchers.IO) {
        syncPrefs.clearPlaysTimestamps()
        playDao.deleteAll()
        gameDao.resetPlaySync()
    }

    // endregion

    // region Stats

    suspend fun calculateStats() = withContext(Dispatchers.Default) {
        if ((syncPrefs[TIMESTAMP_PLAYS_OLDEST_DATE, Long.MAX_VALUE] ?: Long.MAX_VALUE) == 0L) {
            calculatePlayStats()
            calculatePlayerStats()
        }
    }

    suspend fun calculatePlayStats(): PlayStats {
        val includeIncompletePlays = prefs[PlayStatPrefs.LOG_PLAY_STATS_INCOMPLETE, false] ?: false
        val includeExpansions = prefs[PlayStatPrefs.LOG_PLAY_STATS_EXPANSIONS, false] ?: false
        val includeAccessories = prefs[PlayStatPrefs.LOG_PLAY_STATS_ACCESSORIES, false] ?: false

        val games = loadForStats(includeIncompletePlays, includeExpansions, includeAccessories)
        return PlayStats.fromList(games, prefs.isStatusSetToSync(COLLECTION_STATUS_OWN)).also {
            updateHIndex(it.hIndex, HIndexType.Game, R.string.game, NOTIFICATION_ID_PLAY_STATS_GAME_H_INDEX)
        }
    }

    suspend fun calculatePlayerStats(): PlayerStats {
        val includeIncompletePlays = prefs[PlayStatPrefs.LOG_PLAY_STATS_INCOMPLETE, false] ?: false
        val players = loadPlayersForStats(includeIncompletePlays)
        return PlayerStats.fromList(players).also {
            updateHIndex(it.hIndex, HIndexType.Player, R.string.player, NOTIFICATION_ID_PLAY_STATS_PLAYER_H_INDEX)
        }
    }

    private suspend fun loadForStats(
        includeIncompletePlays: Boolean,
        includeExpansions: Boolean,
        includeAccessories: Boolean
    ): List<GameForPlayStats> = withContext(Dispatchers.Default) {
        // TODO make this more efficient (I think we can get it all in 1 query)
        val allPlays = playDao.loadPlays().filter { it.deleteTimestamp == 0L }
        val plays = if (includeIncompletePlays) allPlays else allPlays.filterNot { it.incomplete }
        val gameMap = plays.groupingBy { it.objectId to it.itemName }.fold(0) { accumulator, element ->
            accumulator + element.quantity
        }
        val items = collectionDao.loadAll().map { it.mapToModel() }
        val filteredItems = items.filter {
            it.subtype == null || it.subtype == Game.Subtype.BOARDGAME ||
                    (it.subtype == Game.Subtype.BOARDGAME_EXPANSION && includeExpansions) ||
                    (it.subtype == Game.Subtype.BOARDGAME_ACCESSORY && includeAccessories)
        }
        gameMap.filterKeys { it.first in filteredItems.map { item -> item.gameId } }.map { game ->
            val itemPairs = items.filter { it.gameId == game.key.first }
            GameForPlayStats(
                id = game.key.first,
                name = game.key.second,
                playCount = game.value,
                isOwned = itemPairs.any { it.own },
                bggRank = itemPairs.minOfOrNull { it.rank } ?: CollectionItem.RANK_UNKNOWN
            )
        }
    }

    private suspend fun loadPlayersForStats(includeIncompletePlays: Boolean): List<Player> {
        val username = context.preferences()[AccountPreferences.KEY_USERNAME, ""]
        return playDao.loadPlayers()
            .asSequence()
            .filterNot { it.player.username == username }
            .filter { includeIncompletePlays || !it.incomplete }
            .groupBy { it.key() }
            .map { it.value.mapToModel() }
            .filterNotNull()
            .sortedByDescending { it.playCount }
            .toList()
    }

    private fun updateHIndex(hIndex: HIndex, type: HIndexType, @StringRes typeResId: Int, notificationId: Int) {
        if (hIndex.isValid()) {
            val old = prefs.getHIndex(type)
            if (old != hIndex) {
                prefs.setHIndex(type, hIndex)
                @StringRes val messageId =
                    if (hIndex > old) R.string.sync_notification_h_index_increase else R.string.sync_notification_h_index_decrease
                context.notify(
                    context.createNotificationBuilder(
                        R.string.title_play_stats, NotificationChannels.STATS, PlayStatsActivity::class.java
                    ).setContentText(context.getText(messageId, context.getString(typeResId), hIndex.description)).setContentIntent(
                        PendingIntent.getActivity(
                            context,
                            0,
                            Intent(context, PlayStatsActivity::class.java),
                            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0,
                        )
                    ), NotificationTags.PLAY_STATS, notificationId
                )
            }
        }
    }

    // endregion

    companion object {
        private const val NOTIFICATION_ID_PLAY_STATS_GAME_H_INDEX = 0
        private const val NOTIFICATION_ID_PLAY_STATS_PLAYER_H_INDEX = 1
    }
}
