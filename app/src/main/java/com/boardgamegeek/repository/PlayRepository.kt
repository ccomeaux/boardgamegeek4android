package com.boardgamegeek.repository

import android.app.PendingIntent
import android.content.ContentProviderOperation
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.core.content.contentValuesOf
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.auth.AccountUtils
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.db.GameDao
import com.boardgamegeek.db.PlayDao
import com.boardgamegeek.entities.*
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.mappers.mapToEntity
import com.boardgamegeek.pref.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.Plays
import com.boardgamegeek.provider.BggContract.PlayerColors
import com.boardgamegeek.provider.BggContract.PlayPlayers
import com.boardgamegeek.ui.PlayStatsActivity
import com.boardgamegeek.util.NotificationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.min

class PlayRepository(val application: BggApplication) {
    private val playDao = PlayDao(application)
    private val gameDao = GameDao(application)
    private val collectionDao = CollectionDao(application)
    private val prefs: SharedPreferences by lazy { application.preferences() }
    private val syncPrefs: SharedPreferences by lazy { SyncPrefs.getPrefs(application.applicationContext) }
    private val username: String? by lazy { AccountUtils.getUsername(application) }
    private val bggService = Adapter.createForXml()

    enum class SortBy(val daoSortBy: PlayDao.PlaysSortBy) {
        DATE(PlayDao.PlaysSortBy.DATE),
        LOCATION(PlayDao.PlaysSortBy.LOCATION),
        GAME(PlayDao.PlaysSortBy.GAME),
        LENGTH(PlayDao.PlaysSortBy.LENGTH),
    }

    suspend fun loadPlay(internalId: Long): PlayEntity? = playDao.loadPlay(internalId)

    suspend fun refreshPlay(
        internalId: Long,
        playId: Int,
        gameId: Int,
        timestamp: Long = System.currentTimeMillis()
    ): PlayEntity? =
        withContext(Dispatchers.IO) {
            var page = 1
            if (username.isNullOrBlank() ||
                internalId == BggContract.INVALID_ID.toLong() ||
                playId == BggContract.INVALID_ID ||
                gameId == BggContract.INVALID_ID
            ) {
                null
            } else {
                var returnedPlay: PlayEntity? = null
                do {
                    val result = bggService.playsByGame(username, gameId, page++)
                    val plays = result.plays.mapToEntity(timestamp)
                    playDao.save(plays, timestamp)
                    Timber.i("Synced plays for game ID %s (page %,d)", gameId, page)
                    if (returnedPlay == null) returnedPlay = plays.find { it.playId == playId }
                } while (result.hasMorePages() && returnedPlay == null)
                returnedPlay
            }
        }

    suspend fun getPlays(sortBy: SortBy = SortBy.DATE) = playDao.loadPlays(sortBy.daoSortBy)

    suspend fun getPendingPlays() = playDao.loadPendingPlays()

    suspend fun getDraftPlays() = playDao.loadDraftPlays()

    suspend fun getUpdatingPlays() = playDao.loadPlays(selection = playDao.createPendingUpdatePlaySelectionAndArgs(), includePlayers = true)

    suspend fun getDeletingPlays() = playDao.loadPlays(selection = playDao.createPendingDeletePlaySelectionAndArgs(), includePlayers = true)

    suspend fun loadPlaysByGame(gameId: Int) = playDao.loadPlaysByGame(gameId, PlayDao.PlaysSortBy.DATE)

    suspend fun loadPlaysByLocation(location: String) = playDao.loadPlaysByLocation(location)

    suspend fun loadPlaysByUsername(username: String) = playDao.loadPlaysByUsername(username)

    suspend fun loadPlaysByPlayerName(playerName: String) = playDao.loadPlaysByPlayerName(playerName)

    suspend fun loadPlaysByPlayer(name: String, gameId: Int, isUser: Boolean) = playDao.loadPlaysByPlayerAndGame(name, gameId, isUser)

    suspend fun refreshPlays() = withContext(Dispatchers.IO) {
        val newestTimestamp = syncPrefs.getPlaysNewestTimestamp()
        val oldestTimestamp = syncPrefs.getPlaysOldestTimestamp()
        val syncInitiatedTimestamp = System.currentTimeMillis()

        var page = 1
        do {
            val response = bggService.plays(
                username,
                newestTimestamp?.asDateForApi(),
                null,
                page++
            )
            val plays = response.plays.mapToEntity(syncInitiatedTimestamp)
            playDao.save(plays, syncInitiatedTimestamp)

            val newestDate = plays.maxByOrNull { it.dateInMillis }?.dateInMillis ?: 0L
            if (newestDate > syncPrefs.getPlaysNewestTimestamp() ?: 0L) {
                syncPrefs.setPlaysNewestTimestamp(newestDate)
            }

            Timber.i("Synced %,d new plays on page %,d", plays.size, page - 1)
        } while (response.hasMorePages())

        if (oldestTimestamp > 0) {
            page = 1
            do {
                val response = bggService.plays(
                    username,
                    null,
                    oldestTimestamp.asDateForApi(),
                    page++
                )
                val plays = response.plays.mapToEntity(syncInitiatedTimestamp)
                playDao.save(plays, syncInitiatedTimestamp)

                val oldestDate = plays.minByOrNull { it.dateInMillis }?.dateInMillis ?: Long.MAX_VALUE
                if (oldestDate < SyncPrefs.getPrefs(application).getPlaysOldestTimestamp()) {
                    syncPrefs.setPlaysOldestTimestamp(oldestDate)
                }

                Timber.i("Synced %,d old plays on page %,d", plays.size, page - 1)
            } while (response.hasMorePages())
        } else {
            Timber.i("Not syncing old plays; already caught up.")
        }

        newestTimestamp?.let { playDao.deleteUnupdatedPlaysSince(syncInitiatedTimestamp, it) }
        if (oldestTimestamp > 0L) {
            playDao.deleteUnupdatedPlaysBefore(syncInitiatedTimestamp, oldestTimestamp)
        } else {
            syncPrefs.setPlaysOldestTimestamp(0L)
        }
        calculatePlayStats()
    }

    suspend fun refreshPlays(timeInMillis: Long) = withContext(Dispatchers.IO) {
        if (timeInMillis <= 0L && !username.isNullOrBlank()) {
            emptyList()
        } else {
            val plays = mutableListOf<PlayEntity>()
            val timestamp = System.currentTimeMillis()
            val date = timeInMillis.asDateForApi()
            var page = 1
            do {
                val response = bggService.playsByDate(username, date, date, page++)
                val playsPage = response.plays.mapToEntity(timestamp)
                playDao.save(playsPage, timestamp)
                plays += playsPage
                Timber.i("Synced plays for %s (page %,d)", timeInMillis.asDateForApi(), page)
            } while (response.hasMorePages())

            calculatePlayStats()

            plays
        }
    }

    suspend fun loadForStats(
        includeIncompletePlays: Boolean,
        includeExpansions: Boolean,
        includeAccessories: Boolean
    ): List<GameForPlayStatEntity> {
        // TODO use PlayDao if either of these is false
        // val isOwnedSynced = PreferencesUtils.isStatusSetToSync(application, BggService.COLLECTION_QUERY_STATUS_OWN)
        // val isPlayedSynced = PreferencesUtils.isStatusSetToSync(application, BggService.COLLECTION_QUERY_STATUS_PLAYED)
        val playInfo = gameDao.loadPlayInfo(includeIncompletePlays, includeExpansions, includeAccessories)
        return filterGamesOwned(playInfo)
    }

    private suspend fun filterGamesOwned(playInfo: List<GameForPlayStatEntity>): List<GameForPlayStatEntity> = withContext(Dispatchers.Default) {
        val items = collectionDao.load()
        val games = mutableListOf<GameForPlayStatEntity>()
        playInfo.forEach { game ->
            games += game.copy(isOwned = items.any { item -> item.gameId == game.id && item.own })
        }
        games.toList()
    }

    suspend fun loadPlayers(sortBy: PlayDao.PlayerSortBy = PlayDao.PlayerSortBy.NAME): List<PlayerEntity> {
        return playDao.loadPlayers(Plays.buildPlayersByUniquePlayerUri(), sortBy = sortBy)
    }

    suspend fun loadPlayersByGame(gameId: Int): List<PlayPlayerEntity> {
        return playDao.loadPlayersByGame(gameId)
    }

    suspend fun loadPlayersForStats(includeIncompletePlays: Boolean): List<PlayerEntity> {
        return playDao.loadPlayersForStats(includeIncompletePlays)
    }

    suspend fun loadUserPlayer(username: String): PlayerEntity? {
        return playDao.loadUserPlayer(username)
    }

    suspend fun loadNonUserPlayer(playerName: String): PlayerEntity? {
        return playDao.loadNonUserPlayer(playerName)
    }

    suspend fun loadUserColors(username: String): List<PlayerColorEntity> {
        return playDao.loadColors(PlayerColors.buildUserUri(username))
    }

    suspend fun loadPlayerColors(playerName: String): List<PlayerColorEntity> {
        return playDao.loadColors(PlayerColors.buildPlayerUri(playerName))
    }

    suspend fun savePlayerColors(playerName: String, colors: List<PlayerColorEntity>?) {
        playDao.saveColors(PlayerColors.buildPlayerUri(playerName), colors)
    }

    suspend fun saveUserColors(username: String, colors: List<PlayerColorEntity>?) {
        playDao.saveColors(PlayerColors.buildUserUri(username), colors)
    }

    suspend fun loadUserPlayerDetail(username: String): List<PlayerDetailEntity> {
        return playDao.loadPlayerDetail(
            Plays.buildPlayerUri(),
            "${PlayPlayers.USER_NAME}=?",
            arrayOf(username)
        )
    }

    suspend fun loadNonUserPlayerDetail(playerName: String): List<PlayerDetailEntity> {
        return playDao.loadPlayerDetail(
            Plays.buildPlayerUri(),
            "${PlayPlayers.USER_NAME.whereEqualsOrNull()} AND play_players.${PlayPlayers.NAME}=?",
            arrayOf("", playerName)
        )
    }

    suspend fun loadLocations(sortBy: PlayDao.LocationSortBy = PlayDao.LocationSortBy.NAME): List<LocationEntity> {
        return playDao.loadLocations(sortBy)
    }

    suspend fun delete(internalId: Long): Boolean {
        return playDao.delete(internalId)
    }

    suspend fun markAsSynced(internalId: Long, playId: Int) {
        playDao.update(
            internalId,
            contentValuesOf(
                Plays.PLAY_ID to playId,
                Plays.DIRTY_TIMESTAMP to 0,
                Plays.UPDATE_TIMESTAMP to 0,
                Plays.DELETE_TIMESTAMP to 0,
            )
        )
    }

    suspend fun markAsDiscarded(internalId: Long) {
        playDao.update(
            internalId,
            contentValuesOf(
                Plays.DELETE_TIMESTAMP to 0,
                Plays.UPDATE_TIMESTAMP to 0,
                Plays.DIRTY_TIMESTAMP to 0,
            )
        )
    }

    suspend fun markAsUpdated(internalId: Long) {
        playDao.update(
            internalId,
            contentValuesOf(
                Plays.UPDATE_TIMESTAMP to System.currentTimeMillis(),
                Plays.DELETE_TIMESTAMP to 0,
                Plays.DIRTY_TIMESTAMP to 0,
            )
        )
    }

    suspend fun markAsDeleted(internalId: Long) {
        playDao.update(
            internalId,
            contentValuesOf(
                Plays.DELETE_TIMESTAMP to System.currentTimeMillis(),
                Plays.UPDATE_TIMESTAMP to 0,
                Plays.DIRTY_TIMESTAMP to 0,
            )
        )
    }

    suspend fun updateGamePlayCount(gameId: Int) = withContext(Dispatchers.Default) {
        val allPlays = playDao.loadPlaysByGame(gameId)
        val playCount = allPlays
            .filter { it.deleteTimestamp == 0L }
            .sumOf { it.quantity }
        gameDao.update(gameId, contentValuesOf(BggContract.Collection.NUM_PLAYS to playCount))
    }

    suspend fun loadPlayersByLocation(location: String = ""): List<PlayerEntity> = withContext(Dispatchers.IO) {
        playDao.loadPlayersByLocation(location)
    }

    suspend fun updatePlaysWithNickName(username: String, nickName: String): Int = withContext(Dispatchers.IO) {
        val count = playDao.countNickNameUpdatePlays(username, nickName)
        val batch = arrayListOf<ContentProviderOperation>()
        batch += playDao.createDirtyPlaysForUserAndNickNameOperations(username, nickName)
        batch += playDao.createNickNameUpdateOperation(username, nickName)
        application.contentResolver.applyBatch(batch) // is this better for DAO?
        count
    }

    suspend fun renamePlayer(oldName: String, newName: String) = withContext(Dispatchers.IO) {
        val batch = arrayListOf<ContentProviderOperation>()
        batch += playDao.createDirtyPlaysForNonUserPlayerOperations(oldName)
        batch += playDao.createRenameUpdateOperation(oldName, newName)
        batch += playDao.createCopyPlayerColorsOperations(oldName, newName)
        batch += playDao.createDeletePlayerColorsOperation(oldName)
        application.contentResolver.applyBatch(batch)// is this better for DAO?
    }

    data class RenameLocationResults(val oldLocationName: String, val newLocationName: String, val count: Int)

    suspend fun renameLocation(
        oldLocationName: String,
        newLocationName: String,
    ): RenameLocationResults = withContext(Dispatchers.IO) {
        val batch = arrayListOf<ContentProviderOperation>()

        val values = contentValuesOf(Plays.LOCATION to newLocationName)
        batch.add(
            ContentProviderOperation
                .newUpdate(Plays.CONTENT_URI)
                .withValues(values)
                .withSelection(
                    "${Plays.LOCATION}=? AND (${Plays.UPDATE_TIMESTAMP.greaterThanZero()} OR ${Plays.DIRTY_TIMESTAMP.greaterThanZero()})",
                    arrayOf(oldLocationName)
                ).build()
        )

        values.put(Plays.UPDATE_TIMESTAMP, System.currentTimeMillis())
        batch.add(
            ContentProviderOperation
                .newUpdate(Plays.CONTENT_URI)
                .withValues(values)
                .withSelection(
                    "${Plays.LOCATION}=? AND ${Plays.UPDATE_TIMESTAMP.whereZeroOrNull()} AND ${Plays.DELETE_TIMESTAMP.whereZeroOrNull()} AND ${Plays.DIRTY_TIMESTAMP.whereZeroOrNull()}",
                    arrayOf(oldLocationName)
                ).build()
        )

        val results = application.contentResolver.applyBatch(batch)
        RenameLocationResults(oldLocationName, newLocationName, results.sumOf { it.count ?: 0 })
    }

    suspend fun addUsernameToPlayer(playerName: String, username: String) = withContext(Dispatchers.IO) {
        // TODO verify username is good
        val batch = arrayListOf<ContentProviderOperation>()
        batch += playDao.createDirtyPlaysForNonUserPlayerOperations(playerName)
        batch += playDao.createAddUsernameOperation(playerName, username)
        batch += playDao.createCopyPlayerColorsToUserOperations(playerName, username)
        batch += playDao.createDeletePlayerColorsOperation(playerName)
        application.contentResolver.applyBatch(batch)
    }

    suspend fun save(play: PlayEntity): Long {
        val id = playDao.save(play)

        // if the play is "current" (for today and about to be synced), remember the location and players to be used in the next play
        val isUpdating = play.updateTimestamp > 0
        val endTime = play.dateInMillis + min(60 * 24, play.length) * 60 * 1000
        val isToday = play.dateInMillis.isToday() || endTime.isToday()
        if (!play.isSynced && isUpdating && isToday) {
            prefs.putLastPlayTime(System.currentTimeMillis())
            prefs.putLastPlayLocation(play.location)
            prefs.putLastPlayPlayerEntities(play.players)
        }

        return id
    }

    suspend fun calculatePlayStats() = withContext(Dispatchers.Default) {
        if (SyncPrefs.getPrefs(application).isPlaysSyncUpToDate()) {
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
            application,
            hIndex,
            PlayStats.KEY_GAME_H_INDEX,
            R.string.game,
            NOTIFICATION_ID_PLAY_STATS_GAME_H_INDEX
        )
    }

    fun updatePlayerHIndex(hIndex: HIndexEntity) {
        updateHIndex(
            application,
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
                NotificationUtils.notify(
                    context, NotificationUtils.TAG_PLAY_STATS, notificationId,
                    NotificationUtils.createNotificationBuilder(
                        context,
                        R.string.title_play_stats,
                        NotificationUtils.CHANNEL_ID_STATS,
                        PlayStatsActivity::class.java
                    )
                        .setContentText(context.getText(messageId, context.getString(typeResId), hIndex.description))
                        .setContentIntent(
                            PendingIntent.getActivity(
                                context,
                                0,
                                Intent(context, PlayStatsActivity::class.java),
                                PendingIntent.FLAG_UPDATE_CURRENT,
                            )
                        )
                )
            }
        }
    }

    companion object {
        private const val NOTIFICATION_ID_PLAY_STATS_GAME_H_INDEX = 0
        private const val NOTIFICATION_ID_PLAY_STATS_PLAYER_H_INDEX = 1
    }
}
