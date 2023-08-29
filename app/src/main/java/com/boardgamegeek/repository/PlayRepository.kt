package com.boardgamegeek.repository

import android.app.PendingIntent
import android.content.ContentProviderOperation
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.content.contentValuesOf
import androidx.work.*
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.db.GameDao
import com.boardgamegeek.db.PlayDao
import com.boardgamegeek.entities.*
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.PhpApi
import com.boardgamegeek.mappers.mapToEntity
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
) {
    private val playDao = PlayDao(context)
    private val gameDao = GameDao(context)
    private val collectionDao = CollectionDao(context)
    private val prefs: SharedPreferences by lazy { context.preferences() }
    private val syncPrefs: SharedPreferences by lazy { SyncPrefs.getPrefs(context.applicationContext) }

    suspend fun loadPlay(internalId: Long) = playDao.loadPlay(internalId)?.mapToEntity()

    suspend fun refreshPlay(
        internalId: Long,
        playId: Int,
        gameId: Int,
        timestamp: Long = System.currentTimeMillis()
    ): PlayEntity? =
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
                var returnedPlay: PlayEntity?
                do {
                    val result = api.playsByGame(username, gameId, page++)
                    val plays = result.plays.mapToEntity(timestamp)
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
                val playsPage = response.plays.mapToEntity(timestamp)
                saveFromSync(playsPage, timestamp)
            } while (response.hasMorePages())

            playDao.deleteUnupdatedPlays(gameId, timestamp)
            gameDao.update(gameId, contentValuesOf(Games.Columns.UPDATED_PLAYS to System.currentTimeMillis()))
            calculatePlayStats()
        }
    }

    suspend fun refreshPartialPlaysForGame(gameId: Int) = withContext(Dispatchers.IO) {
        val username = prefs[AccountPreferences.KEY_USERNAME, ""]
        if (!username.isNullOrBlank()) {
            val timestamp = System.currentTimeMillis()
            val response = api.playsByGame(username, gameId, 1)
            val plays = response.plays.mapToEntity(timestamp)
            saveFromSync(plays, timestamp)
            calculatePlayStats()
        }
    }

    /**
     * Upload the play to BGG. Returns the status (new, update, or error). If successful, returns the new Play ID and the new total number of plays,
     * an error message if not.
     */
    suspend fun upsertPlay(play: PlayEntity): Result<PlayUploadResult> {
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

    suspend fun deletePlay(play: PlayEntity): Result<PlayUploadResult> {
        if (play.deleteTimestamp == 0L)
            return Result.success(PlayUploadResult.noOp(play))
        if (play.internalId == INVALID_ID.toLong())
            return Result.success(PlayUploadResult.noOp(play))
        if (play.playId == INVALID_ID) {
            playDao.delete(play.internalId)
            return Result.success(PlayUploadResult.delete(play))
        }
        val response = phpApi.play(play.playId.mapToFormBodyForDelete().build())
        return if (response.hasAuthError()) {
            Authenticator.clearPassword(context)
            Result.failure(Exception(context.getString(R.string.msg_play_update_auth_error)))
        } else if (response.hasInvalidIdError()) {
            playDao.delete(play.internalId)
            Result.success(PlayUploadResult.delete(play))
        } else if (!response.error.isNullOrBlank()) {
            Result.failure(Exception(response.error))
        } else if (!response.success) {
            Result.failure(Exception(context.getString(R.string.msg_play_delete_unsuccessful)))
        } else {
            playDao.delete(play.internalId)
            Result.success(PlayUploadResult.delete(play))
        }
    }

    suspend fun loadPlays() = playDao.loadPlays().map { it.mapToEntity() }

    suspend fun loadUpdatingPlays() = playDao.loadUpdatingPlays().map { it.mapToEntity() }

    suspend fun loadDeletingPlays() = playDao.loadDeletingPlays().map { it.mapToEntity() }

    suspend fun loadPlaysByGame(gameId: Int) = playDao.loadPlaysByGame(gameId, PlayDao.PlaysSortBy.DATE).map { it.mapToEntity() }

    suspend fun loadPlaysByLocation(location: String) = playDao.loadPlaysByLocation(location).map { it.mapToEntity() }

    suspend fun loadPlaysByUsername(username: String) = playDao.loadPlaysByUsername(username).map { it.mapToEntity() }

    suspend fun loadPlaysByPlayerName(playerName: String) = playDao.loadPlaysByPlayerName(playerName).map { it.mapToEntity() }

    suspend fun loadPlaysByPlayer(name: String, gameId: Int, isUser: Boolean) = playDao.loadPlaysByPlayerAndGame(name, gameId, isUser)

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
            val plays = response.plays.mapToEntity(syncInitiatedTimestamp)
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
                val plays = response.plays.mapToEntity(syncInitiatedTimestamp)
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
        playDao.deleteUnupdatedPlaysByDate(syncTimestamp, playDate, ">=")

    suspend fun deleteUnupdatedPlaysBefore(syncTimestamp: Long, playDate: Long) =
        playDao.deleteUnupdatedPlaysByDate(syncTimestamp, playDate, "<=")

    suspend fun refreshPlaysForDate(timeInMillis: Long) = withContext(Dispatchers.IO) {
        val username = prefs[AccountPreferences.KEY_USERNAME, ""]
        if (timeInMillis <= 0L && !username.isNullOrBlank()) {
            emptyList()
        } else {
            val plays = mutableListOf<PlayEntity>()
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
            emptyList<PlayEntity>() to false
        } else {
            val response = api.plays(username, from, to, page)
            response.plays.mapToEntity(timestamp) to response.hasMorePages()
        }
    }

    suspend fun loadForStats(
        includeIncompletePlays: Boolean,
        includeExpansions: Boolean,
        includeAccessories: Boolean
    ): List<GameForPlayStatEntity> = withContext(Dispatchers.IO) {
        val games = if (!syncPrefs.isStatusSetToSync(COLLECTION_STATUS_PLAYED)) {
            // If played games aren't synced, count the plays instead
            // We can't respect the expansion/accessory flags, so we include them all
            val allPlays = playDao.loadPlays().filter { it.deleteTimestamp == 0L }
            val plays = if (includeIncompletePlays) allPlays else allPlays.filterNot { it.incomplete }
            val gameMap = plays.groupingBy { it.objectId to it.itemName }.fold(0) { accumulator, element ->
                accumulator + element.quantity
            }
            gameMap.map {
                GameForPlayStatEntity(
                    id = it.key.first,
                    name = it.key.second,
                    playCount = it.value,
                )
            }
        } else gameDao.loadGamesForPlayStats(includeIncompletePlays, includeExpansions, includeAccessories).filter { it.playCount > 0 }
        if (syncPrefs.isStatusSetToSync(COLLECTION_STATUS_OWN)) {
            val items = collectionDao.load()
            games.map {
                val isOwned = items.any { item -> item.gameId == it.id && item.own }
                if (it.bggRank == GameRankEntity.RANK_UNKNOWN) {
                    items.find { item -> item.gameId == it.id && item.own }?.let { item ->
                        it.copy(isOwned = isOwned, bggRank = item.rank)
                    } ?: it
                } else {
                    it.copy(isOwned = isOwned)
                }
            }
        } else games
    }

    suspend fun loadPlayers(sortBy: PlayDao.PlayerSortBy = PlayDao.PlayerSortBy.NAME, includeDeletedPlays: Boolean = true) =
        playDao.loadPlayers(sortBy, includeDeletedPlays).map { it.mapToEntity() }

    suspend fun loadPlayersByGame(gameId: Int) = playDao.loadPlayersByGame(gameId).map { it.mapToEntity() }

    suspend fun loadPlayerFavoriteColors(): Map<PlayerEntity, String> {
        return playDao.loadAllPlayerColors().filter { it.playerColorSortOrder == 1 }.associate {
            val entity = if (it.playerType == PlayerColors.TYPE_USER)
                PlayerEntity.createUser(it.playerName)
            else
                PlayerEntity.createNonUser(it.playerName)
            entity to it.playerColor
        }
    }

    suspend fun loadPlayer(name: String?, type: PlayerType): PlayerEntity? {
        return when {
            name.isNullOrBlank() -> null
            type == PlayerType.USER -> playDao.loadUserPlayer(name)?.mapToEntity()
            type == PlayerType.NON_USER -> playDao.loadNonUserPlayer(name)?.mapToEntity()
            else -> null
        }
    }

    suspend fun loadPlayersForStats(includeIncompletePlays: Boolean): List<PlayerEntity> {
        val username = context.preferences()[AccountPreferences.KEY_USERNAME, ""]
        return playDao
            .loadPlayers(PlayDao.PlayerSortBy.PLAY_COUNT, false, includeIncompletePlays)
            .filterNot { it.username == username }
            .map { it.mapToEntity() }
    }

    enum class PlayerType {
        USER,
        NON_USER,
    }

    suspend fun loadPlayerColors(name: String, type: PlayerType): List<PlayerColorEntity> {
        return when (type) {
            PlayerType.USER -> loadUserColors(name)
            PlayerType.NON_USER -> loadNonUserColors(name)
        }
    }

    suspend fun loadUserColors(username: String) = playDao.loadUserColors(username).map { it.mapToEntity() }

    suspend fun loadNonUserColors(playerName: String) = playDao.loadNonUserColors(playerName).map { it.mapToEntity() }

    suspend fun saveNonUserColors(name: String?, type: PlayerType, colors: List<String>?) {
        if (!name.isNullOrBlank()) {
            when (type) {
                PlayerType.USER -> saveUserColors(name, colors)
                PlayerType.NON_USER -> saveNonUserColors(name, colors)
            }
        }
    }

    private suspend fun saveUserColors(username: String, colors: List<String>?) = playDao.saveUserColors(username, colors)

    private suspend fun saveNonUserColors(playerName: String, colors: List<String>?) = playDao.saveNonUserColors(playerName, colors)

    suspend fun loadPlayerUsedColors(name: String?, type: PlayerType): List<String> {
        return when {
            name.isNullOrBlank() -> emptyList()
            type == PlayerType.USER -> playDao.loadUserUsedColors(name)
            type == PlayerType.NON_USER -> playDao.loadNonUserUsedColors(name)
            else -> emptyList()
        }
    }

    suspend fun loadLocations(sortBy: PlayDao.LocationSortBy = PlayDao.LocationSortBy.NAME) = playDao.loadLocations(sortBy).map { it.mapToEntity() }

    suspend fun logQuickPlay(gameId: Int, gameName: String): Result<PlayUploadResult> {
        val playEntity = PlayEntity(
            gameId = gameId,
            gameName = gameName,
            dateInMillis = Calendar.getInstance().timeInMillis,
            updateTimestamp = System.currentTimeMillis()
        )
        val internalId = playDao.upsert(playEntity.mapToEntity())
        return logPlay(playEntity.copy(internalId = internalId))
    }

    suspend fun logPlay(playEntity: PlayEntity): Result<PlayUploadResult> {
        return try {
            val result = upsertPlay(playEntity)
            if (result.isSuccess) {
                updateGamePlayCount(playEntity.gameId)
                calculatePlayStats()
            }
            result
        } catch (ex: Exception) {
            enqueueUploadRequest(playEntity.internalId)
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

    suspend fun saveFromSync(plays: List<PlayEntity>, syncTimestamp: Long) {
        Timber.i("Saving %d plays", plays.size)
        var updateCount = 0
        var insertCount = 0
        var unchangedCount = 0
        var dirtyCount = 0
        var errorCount = 0
        plays.forEach {
            val play = it.mapToEntity(syncTimestamp)
            when (playDao.saveFromSync(play)) {
                PlayDao.SaveStatus.UPDATED -> updateCount++
                PlayDao.SaveStatus.INSERTED -> insertCount++
                PlayDao.SaveStatus.DIRTY -> dirtyCount++
                PlayDao.SaveStatus.ERROR -> errorCount++
                PlayDao.SaveStatus.UNCHANGED -> unchangedCount++
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

    private suspend fun markAsSynced(internalId: Long, playId: Int): Boolean {
        if (playId == INVALID_ID) return false
        return playDao.markAsSynced(internalId, playId)
    }

    suspend fun markAsDiscarded(internalId: Long) = playDao.markAsDiscarded(internalId)

    suspend fun markAsUpdated(internalId: Long) = playDao.markAsUpdated(internalId)

    suspend fun markAsDeleted(internalId: Long) = playDao.markAsDeleted(internalId)

    suspend fun updateGamePlayCount(gameId: Int) = withContext(Dispatchers.Default) {
        val allPlays = playDao.loadPlaysByGame(gameId)
        val playCount = allPlays
            .filter { it.deleteTimestamp == 0L }
            .sumOf { it.quantity }
        gameDao.update(gameId, contentValuesOf(Games.Columns.NUM_PLAYS to playCount))
    }

    suspend fun loadPlayersByLocation(location: String = "") = playDao.loadPlayersByLocation(location).map { it.mapToEntity() }

    suspend fun updatePlaysWithNickName(username: String, nickName: String): Collection<Long> = withContext(Dispatchers.IO) {
        val internalIds = mutableListOf<Long>()
        val plays = playDao.loadPlaysByUsername(username, true)
        plays.forEach { play ->
            play.players?.find { it.username == username && it.name != nickName }?.let { player ->
                val batch = arrayListOf<ContentProviderOperation>()
                if (play.updateTimestamp == 0L && play.dirtyTimestamp == 0L) {
                    batch += ContentProviderOperation
                        .newUpdate(Plays.buildPlayUri(play.internalId))
                        .withValue(Plays.Columns.UPDATE_TIMESTAMP, System.currentTimeMillis())
                        .build()
                }
                batch += ContentProviderOperation
                    .newUpdate(Plays.buildPlayerUri(play.internalId, player.internalId))
                    .withValue(PlayPlayers.Columns.NAME, nickName)
                    .build()
                context.contentResolver.applyBatch(batch)
                if (play.dirtyTimestamp == 0L) internalIds += play.internalId
            }
        }
        internalIds
    }

    suspend fun renamePlayer(oldName: String, newName: String): Collection<Long> = withContext(Dispatchers.IO) {
        val dirtyPlayIds = mutableListOf<Long>()
        val plays = playDao.loadPlaysByPlayerName(oldName, true)
        plays.forEach { play ->
            if (playDao.changePlayerName(play, oldName, newName)) {
                if (play.dirtyTimestamp == 0L) dirtyPlayIds += play.internalId
            }
        }
        val colors = loadNonUserColors(oldName).sortedByDescending { it.sortOrder }.map { it.description }
        saveNonUserColors(newName, colors)
        playDao.deleteColorsForPlayer(oldName)
        dirtyPlayIds
    }

    suspend fun addUsernameToPlayer(playerName: String, username: String): Collection<Long> = withContext(Dispatchers.IO) {
        val dirtyPlayIds = mutableListOf<Long>()
        val plays = playDao.loadPlaysByPlayerName(playerName, true)
        plays.forEach { play ->
            if (playDao.addUsernameToPlayer(play, playerName, username)) {
                if (play.dirtyTimestamp == 0L) dirtyPlayIds += play.internalId
            }
        }
        val colors = loadNonUserColors(playerName).sortedByDescending { it.sortOrder }.map { it.description }
        saveUserColors(username, colors)
        playDao.deleteColorsForPlayer(playerName)
        dirtyPlayIds
    }

    suspend fun renameLocation(
        oldLocationName: String,
        newLocationName: String,
    ): List<Long> = withContext(Dispatchers.IO) {
        val internalIds = mutableListOf<Long>()
        val plays = playDao.loadPlaysByLocation(oldLocationName)
        plays.forEach { play ->
            if (playDao.updateLocation(play.internalId, newLocationName, play.dirtyTimestamp ?: 0L, play.updateTimestamp ?: 0L))
                internalIds += play.internalId
        }
        internalIds
    }

    suspend fun save(play: PlayEntity): Long {
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
                prefs.putLastPlayPlayerEntities(play.players)
            }
        }

        return id
    }

    suspend fun resetPlays() {
        // resets the sync timestamps, removes the plays' hashcode, and request a sync
        syncPrefs.clearPlaysTimestamps()
        val count = playDao.clearSyncHashCodes()
        Timber.i("Cleared the hashcode from %,d plays.", count)
        SyncPlaysWorker.requestSync(context)
    }

    suspend fun deletePlays() {
        syncPrefs.clearPlaysTimestamps()
        playDao.deleteAllPlays()
        gameDao.resetPlaySync()
    }

    suspend fun calculatePlayStats() = withContext(Dispatchers.Default) {
        if ((syncPrefs[TIMESTAMP_PLAYS_OLDEST_DATE, Long.MAX_VALUE] ?: Long.MAX_VALUE) == 0L) {
            val includeIncompletePlays = prefs[PlayStats.LOG_PLAY_STATS_INCOMPLETE, false] ?: false
            val includeExpansions = prefs[PlayStats.LOG_PLAY_STATS_EXPANSIONS, false] ?: false
            val includeAccessories = prefs[PlayStats.LOG_PLAY_STATS_ACCESSORIES, false] ?: false

            val playStats = loadForStats(includeIncompletePlays, includeExpansions, includeAccessories)
            val playStatsEntity = PlayStatsEntity(playStats, prefs.isStatusSetToSync(COLLECTION_STATUS_OWN))
            updateGameHIndex(playStatsEntity.hIndex)

            val playerStats = loadPlayersForStats(includeIncompletePlays)
            val playerStatsEntity = PlayerStatsEntity(playerStats)
            updatePlayerHIndex(playerStatsEntity.hIndex)
        }
    }

    fun updateGameHIndex(hIndex: HIndexEntity) {
        updateHIndex(
            context,
            hIndex,
            PlayStats.KEY_GAME_H_INDEX,
            R.string.game,
            NOTIFICATION_ID_PLAY_STATS_GAME_H_INDEX
        )
    }

    fun updatePlayerHIndex(hIndex: HIndexEntity) {
        updateHIndex(
            context,
            hIndex,
            PlayStats.KEY_PLAYER_H_INDEX,
            R.string.player,
            NOTIFICATION_ID_PLAY_STATS_PLAYER_H_INDEX
        )
    }

    private fun updateHIndex(
        context: Context,
        hIndex: HIndexEntity,
        key: String,
        @StringRes typeResId: Int,
        notificationId: Int
    ) {
        if (hIndex.h != HIndexEntity.INVALID_H_INDEX) {
            val old = HIndexEntity(prefs[key, 0] ?: 0, prefs[key + PlayStats.KEY_H_INDEX_N_SUFFIX, 0] ?: 0)
            if (old != hIndex) {
                prefs[key] = hIndex.h
                prefs[key + PlayStats.KEY_H_INDEX_N_SUFFIX] = hIndex.n
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
