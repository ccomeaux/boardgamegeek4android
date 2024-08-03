package com.boardgamegeek.repository

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.text.format.DateUtils
import androidx.annotation.StringRes
import androidx.work.*
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.db.*
import com.boardgamegeek.db.model.GameColorsEntity
import com.boardgamegeek.db.model.PlayerColorsEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.PhpApi
import com.boardgamegeek.io.model.PlaysResponse
import com.boardgamegeek.io.safeApiCall
import com.boardgamegeek.mappers.mapToEntity
import com.boardgamegeek.mappers.mapToFormBodyForDelete
import com.boardgamegeek.mappers.mapToFormBodyForUpsert
import com.boardgamegeek.mappers.mapToModel
import com.boardgamegeek.model.*
import com.boardgamegeek.model.Location.Companion.applySort
import com.boardgamegeek.model.Player.Companion.applySort
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.pref.SyncPrefs.Companion.TIMESTAMP_PLAYS_OLDEST_DATE
import com.boardgamegeek.pref.clearPlaysTimestamps
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import com.boardgamegeek.ui.PlayStatsActivity
import com.boardgamegeek.work.PlayUploadWorker
import com.boardgamegeek.work.SyncPlaysWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Calendar
import java.util.concurrent.TimeUnit

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

    fun loadPlaysFlow(): Flow<List<Play>> {
        return playDao.loadPlaysFlow()
            .map {
                it.map { entity -> entity.mapToModel() }.filterNot { play -> play.deleteTimestamp > 0L }
            }
            .flowOn(Dispatchers.Default)
            .conflate()
    }

    fun loadAllPlaysFlow(): Flow<List<Play>> {
        return playDao.loadPlaysFlow()
            .map {
                it.map { entity -> entity.mapToModel() }
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

    fun loadPlaysByGameFlow(gameId: Int): Flow<List<Play>> {
        return playDao.loadPlaysForGameFlow(gameId)
            .map { list -> list.map { it.mapToModel() }.filterNot { it.deleteTimestamp > 0L } }
            .flowOn(Dispatchers.Default)
    }

    fun loadPlaysByLocationFlow(location: String): Flow<List<Play>> {
        return playDao.loadPlaysForLocationFlow(location).map { list ->
            list.map { it.mapToModel() }.filterNot { it.deleteTimestamp > 0L }
        }
    }

    fun loadPlaysByUsernameFlow(username: String): Flow<List<Play>> {
        return playDao.loadPlaysForUserFlow(username).map { list ->
            list.map { it.mapToModel() }.filterNot { it.deleteTimestamp > 0L }
        }
    }

    fun loadPlaysByPlayerNameFlow(playerName: String): Flow<List<Play>> {
        return playDao.loadPlaysForPlayerFlow(playerName).map { list ->
            list.map { it.mapToModel() }.filterNot { it.deleteTimestamp > 0L }
        }
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

    fun loadPlayersByGameFlow(gameId: Int): Flow<List<PlayPlayer>> {
        return playDao.loadPlayersForGameFlow(gameId)
            .map { list -> list.map { it.player.mapToModel() } }
            .flowOn(Dispatchers.Default)
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
        withContext(Dispatchers.IO) { playerColorDao.loadColorsForUser(username) }.map { it.mapToModel() }
    }

    fun loadUserColorsFlow(username: String) =
        playerColorDao.loadColorsForUserFlow(username).map { list -> list.map { it.mapToModel() } }

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
            .applySort(sortBy)
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

    suspend fun refreshPlaysForGame(gameId: Int, maximumPageCount: Int = Int.MAX_VALUE): String? = withContext(Dispatchers.IO) {
        if (gameId == INVALID_ID)
            return@withContext context.getString(R.string.msg_refresh_plays_invalid_id)

        val username = prefs[AccountPreferences.KEY_USERNAME, ""]
        if (username.isNullOrBlank())
            return@withContext context.getString(R.string.msg_refresh_plays_auth_error)

        val syncInitiatedTimestamp = System.currentTimeMillis()
        var page = 0
        do {
            page++
            if (page > maximumPageCount) break
            val response = safeApiCall(context) { api.playsByGame(username, gameId, page) }
            if (response.isSuccess) {
                val plays = withContext(Dispatchers.Default) { response.getOrNull()?.plays.mapToModel(syncInitiatedTimestamp) }
                saveFromSync(plays, syncInitiatedTimestamp)
                Timber.i("Synced ${plays.size} plays for game $gameId (page $page)")
            } else {
                return@withContext response.exceptionOrNull()?.localizedMessage
            }
        } while (response.isSuccess && response.getOrNull()?.hasMorePages() == true)

        if (maximumPageCount == Int.MAX_VALUE) {
            playDao.deleteUnupdatedPlaysForGame(gameId, syncInitiatedTimestamp).also {
                Timber.i("Deleted $it plays from game $gameId")
            }
            gameDao.updatePlayTimestamp(gameId, syncInitiatedTimestamp)
        }

        calculateStats()
        null
    }

    suspend fun refreshRecentPlays(): String? = withContext(Dispatchers.IO) {
        val username = prefs[AccountPreferences.KEY_USERNAME, ""]
        if (username.isNullOrBlank())
            return@withContext context.getString(R.string.msg_refresh_plays_auth_error)

        val syncInitiatedTimestamp = System.currentTimeMillis()
        val response = safeApiCall(context) { api.plays(username, null, null, 1) }
        if (response.isSuccess) {
            val plays = response.getOrNull()?.plays.mapToModel(syncInitiatedTimestamp)
            saveFromSync(plays, syncInitiatedTimestamp)
            Timber.i("Synced ${plays.size} most recent plays")
            val date = plays.minOf { it.dateInMillis }
            if (date != 0L) {
                deleteUnupdatedPlaysSince(syncInitiatedTimestamp, date.addDay(1))
            }
            prefs[PREFERENCES_KEY_SYNC_PLAYS_DISABLED_TIMESTAMP] = syncInitiatedTimestamp
            calculateStats()
            null
        } else {
            response.exceptionOrNull()?.localizedMessage
        }
    }

    suspend fun refreshPlaysForDate(timeInMillis: Long): String? = withContext(Dispatchers.Default) {
        val username = prefs[AccountPreferences.KEY_USERNAME, ""]
        if (timeInMillis > 0L && !username.isNullOrBlank()) {
            var page = 1
            do {
                val timestamp = System.currentTimeMillis()
                val result = safeApiCall(context) { api.plays(username, timeInMillis.asDateForApi(), timeInMillis.asDateForApi(), page++) }
                if (result.isSuccess) {
                    val playsPage = result.getOrNull()?.plays.mapToModel(timestamp)
                    saveFromSync(playsPage, timestamp)
                    Timber.i("Synced plays for %s (page %,d)", timeInMillis.asDateForApi(), page - 1)
                } else return@withContext result.exceptionOrNull()?.localizedMessage ?: "Error"
            } while (result.getOrNull()?.hasMorePages() == true)

            calculateStats()
        }
        null
    }

    suspend fun downloadPlays(fromDate: Long?, toDate: Long?, page: Int, timestamp: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        val username = prefs[AccountPreferences.KEY_USERNAME, ""]
        if (username.isNullOrBlank()) {
            emptyList<Play>() to false
        } else {
            val response = api.plays(username, fromDate.asDateForApi(), toDate.asDateForApi(), page)
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

    suspend fun logPlay(play: Play) = withContext(Dispatchers.IO) {
        if (play.updateTimestamp > 0L) {
            // remember details about the play if it's being uploaded for the first time
            if (!play.isSynced) {
                // remember the location and players to be used in the next play
                val lastPlayDate = prefs[KEY_LAST_PLAY_DATE, 0L] ?: 0L
                if (play.dateInMillis >= lastPlayDate) {
                    prefs[KEY_LAST_PLAY_DATE] = play.dateInMillis
                    prefs[KEY_LAST_PLAY_TIME] = System.currentTimeMillis()
                    prefs[KEY_LAST_PLAY_LOCATION] = play.location
                    prefs.putLastPlayPlayers(play.players)
                }

                if (play.players.isNotEmpty()) {
                    gameDao.loadGame(play.gameId)?.let {
                        // update game's custom sort order
                        gameDao.updateCustomPlayerSort(play.gameId, play.arePlayersCustomSorted())

                        // update game colors
                        val existingColors = gameColorDao.loadColorsForGame(play.gameId).map { it.color }
                        play.players.map { it.color }.distinct().forEach {
                            if (!existingColors.contains(it)) {
                                gameColorDao.insert(listOf(GameColorsEntity(internalId = 0L, gameId = play.gameId, it)))
                            }
                        }
                    }

                    // save nicknames for players if they don't have one already
                    play.players.filter { it.username.isNotBlank() && it.name.isNotBlank() }.forEach { player ->
                        userDao.loadUser(player.username)?.let {
                            if (it.playNickname.isNullOrBlank()) {
                                userDao.updateNickname(player.username, player.name)
                            }
                        }
                    }
                }
            }

            enqueueUploadRequest(play.internalId)
        }
    }

    // endregion

    // region Upload

    private suspend fun tryUpload(play: Play): Result<PlayUploadResult> {
        return try {
           uploadPlay(play).also {
               if (it.isFailure) Timber.w(it.exceptionOrNull()?.localizedMessage ?: "unknown failure trying to upload play $play")
           }
        } catch (ex: Exception) {
            enqueueUploadRequest(play.internalId)
            Result.failure(Exception(context.getString(R.string.msg_play_queued_for_upload), ex))
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

    suspend fun saveFromSync(plays: List<Play>, syncTimestamp: Long) = withContext(Dispatchers.IO) {
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
                    Timber.i("No change in play ${play.playId}.")
                    unchangedCount++
                }
                else -> {
                    upsert(play.copy(internalId = candidate.internalId), syncTimestamp)
                    updateCount++
                }
            }
        }
        Timber.i(
            "Saved %1\$,d plays: %2\$,d updated, %3\$,d inserted, %4$,d unchanged, %5$,d dirty, %6$,d errors",
            plays.size,
            updateCount,
            insertCount,
            unchangedCount,
            dirtyCount,
            errorCount
        )
    }

    @Suppress("SameParameterValue")
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
            val allPlays = withContext(Dispatchers.IO) { playDao.loadPlaysForGame(gameId) }
                .filterNot { (it.deleteTimestamp ?: 0L) > 0L }
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
        playerColorDao.deleteColorsForPlayer(PlayerColorsEntity.TYPE_PLAYER, oldName)

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
        playerColorDao.deleteColorsForPlayer(PlayerColorsEntity.TYPE_PLAYER, playerName)

        internalIds
    }

    suspend fun renameLocation(
        oldLocationName: String,
        newLocationName: String,
    ): List<Long> = withContext(Dispatchers.IO) {
        val internalIds = mutableListOf<Long>()
        val plays = playDao.loadPlaysForLocation(oldLocationName).map { it.mapToModel() }
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
                        if (type == PlayerType.USER) PlayerColorsEntity.TYPE_USER else PlayerColorsEntity.TYPE_PLAYER,
                        name,
                        color,
                        index + 1,
                    )
                }
                playerColorDao.upsertColorsForPlayer(entities)
            }
        }
    }

    suspend fun upsert(play: Play, syncTimestamp: Long = 0L): Long = withContext(Dispatchers.IO) {
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
        playDao.deleteUnupdatedPlaysAfterDate(playDate.asDateForApi(), syncTimestamp).also {
            Timber.i("Deleted $it plays since ${playDate.asDate()} that haven't been updated since ${syncTimestamp.asTime()} ($syncTimestamp)")
        }
    }

    suspend fun deleteUnupdatedPlaysBefore(syncTimestamp: Long, playDate: Long) = withContext(Dispatchers.IO) {
        playDao.deleteUnupdatedPlaysBeforeDate(playDate.asDateForApi(), syncTimestamp).also {
            Timber.i("Deleted $it plays before ${playDate.asDate()} that haven't been updated since ${syncTimestamp.asTime()} ($syncTimestamp)")
        }
    }

    suspend fun deletePlays() = withContext(Dispatchers.IO) {
        syncPrefs.clearPlaysTimestamps()
        playDao.deleteAll()
        gameDao.resetPlaySync()
    }

    // endregion

    // region Stats

    suspend fun calculateStats() {
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
        val playedGamesMap = plays.groupingBy { it.objectId to it.itemName }.fold(0) { accumulator, element ->
            accumulator + element.quantity
        }
        val items = collectionDao.loadAll().map { it.mapToModel() }
        val games = gameDao.loadGameSubtypes()
        val allGamesForStats = playedGamesMap.map { game ->
            val itemPairs = items.filter { it.gameId == game.key.first }
            val gameSubtype = games.find { it.gameId == game.key.first }
            GameForPlayStats(
                id = game.key.first,
                name = game.key.second,
                playCount = game.value,
                isOwned = itemPairs.any { it.own },
                bggRank = itemPairs.minOfOrNull { it.rank } ?: CollectionItem.RANK_UNKNOWN,
                subtype = gameSubtype?.subtype.toSubtype()
            )
        }
        allGamesForStats.filter {
            it.subtype == Game.Subtype.UNKNOWN ||
                    it.subtype == Game.Subtype.BOARDGAME ||
                    (it.subtype == Game.Subtype.BOARDGAME_ACCESSORY && includeAccessories) ||
                    (it.subtype == Game.Subtype.BOARDGAME_EXPANSION && includeExpansions)
        }
    }

    private suspend fun loadPlayersForStats(includeIncompletePlays: Boolean): List<Player> = withContext(Dispatchers.Default) {
        val username = context.preferences()[AccountPreferences.KEY_USERNAME, ""]
        playDao.loadPlayers()
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
                @StringRes val messageId = if (hIndex > old)
                    R.string.sync_notification_h_index_increase
                else
                    R.string.sync_notification_h_index_decrease
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

    private fun Long.asDate() = this.formatDateTime(context, flags = DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_ALL)

    private fun Long.asTime() = this.formatDateTime(context, flags = DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_ALL or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME)

    companion object {
        private const val NOTIFICATION_ID_PLAY_STATS_GAME_H_INDEX = 0
        private const val NOTIFICATION_ID_PLAY_STATS_PLAYER_H_INDEX = 1
    }
}
