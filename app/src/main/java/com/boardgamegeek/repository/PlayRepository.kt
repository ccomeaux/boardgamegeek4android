package com.boardgamegeek.repository

import android.app.PendingIntent
import android.content.ContentProviderOperation
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.core.content.contentValuesOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.auth.AccountUtils
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.db.GameDao
import com.boardgamegeek.db.PlayDao
import com.boardgamegeek.entities.*
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.mappers.PlayMapper
import com.boardgamegeek.pref.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.tasks.CalculatePlayStatsTask
import com.boardgamegeek.ui.PlayStatsActivity
import com.boardgamegeek.util.NotificationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.*
import kotlin.math.min

class PlayRepository(val application: BggApplication) {
    private val playDao = PlayDao(application)
    private val gameDao = GameDao(application)
    private val collectionDao = CollectionDao(application)
    private val playMapper = PlayMapper()
    private val prefs: SharedPreferences by lazy { application.preferences() }
    private val syncPrefs: SharedPreferences by lazy { SyncPrefs.getPrefs(application.applicationContext) }
    private val username: String? by lazy { AccountUtils.getUsername(application) }
    private val bggService = Adapter.createForXml()

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
                    val result = bggService.playsByGameC(username, gameId, page++)
                    val plays = playMapper.map(result.plays)
                    playDao.save(plays, timestamp)
                    Timber.i("Synced plays for game ID %s (page %,d)", gameId, page)
                    if (returnedPlay == null) returnedPlay = plays.find { it.playId == playId }
                } while (result.hasMorePages() && returnedPlay == null)
                returnedPlay
            }
        }

    suspend fun getPlays(sortBy: PlayDao.PlaysSortBy = PlayDao.PlaysSortBy.DATE) = playDao.loadPlays(sortBy)

    suspend fun getPendingPlays() = playDao.loadPendingPlays()

    suspend fun getDraftPlays() = playDao.loadDraftPlays()

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
            val plays = playMapper.map(response.plays)
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
                val plays = playMapper.map(response.plays)
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
        CalculatePlayStatsTask(application).executeAsyncTask()
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
        return playDao.loadPlayers(BggContract.Plays.buildPlayersByUniquePlayerUri(), sortBy = sortBy)
    }

    fun loadPlayersByGame(gameId: Int): LiveData<List<PlayPlayerEntity>> {
        return playDao.loadPlayersByGame(gameId)
    }

    suspend fun loadPlayersForStats(includeIncompletePlays: Boolean): List<PlayerEntity> {
        return playDao.loadPlayersForStats(includeIncompletePlays)
    }

    fun loadUserPlayer(username: String): LiveData<PlayerEntity> {
        return playDao.loadUserPlayerAsLiveData(username)
    }

    fun loadNonUserPlayer(playerName: String): LiveData<PlayerEntity> {
        return playDao.loadNonUserPlayerAsLiveData(playerName)
    }

    suspend fun loadUserColors(username: String): List<PlayerColorEntity> {
        return playDao.loadUserColors(username)
    }

    suspend fun loadPlayerColors(playerName: String): List<PlayerColorEntity> {
        return playDao.loadPlayerColors(playerName)
    }

    fun savePlayerColors(playerName: String, colors: List<PlayerColorEntity>?) {
        application.appExecutors.diskIO.execute {
            playDao.savePlayerColors(playerName, colors)
        }
    }

    fun saveUserColors(username: String, colors: List<PlayerColorEntity>?) {
        application.appExecutors.diskIO.execute {
            playDao.saveUserColors(username, colors)
        }
    }

    fun loadUserPlayerDetail(username: String): List<PlayerDetailEntity> {
        return playDao.loadUserPlayerDetail(username)
    }

    fun loadNonUserPlayerDetail(playerName: String): List<PlayerDetailEntity> {
        return playDao.loadNonUserPlayerDetail(playerName)
    }

    suspend fun loadLocations(sortBy: PlayDao.LocationSortBy = PlayDao.LocationSortBy.NAME): List<LocationEntity> {
        return playDao.loadLocations(sortBy)
    }

    suspend fun markAsDiscarded(internalId: Long): Long = withContext(Dispatchers.IO) {
        playDao.upsert(
            internalId,
            contentValuesOf(
                BggContract.Plays.DELETE_TIMESTAMP to 0,
                BggContract.Plays.UPDATE_TIMESTAMP to 0,
                BggContract.Plays.DIRTY_TIMESTAMP to 0,
            )
        )
        internalId
    }

    suspend fun markAsUpdated(internalId: Long): Long = withContext(Dispatchers.IO) {
        playDao.upsert(
            internalId,
            contentValuesOf(
                BggContract.Plays.UPDATE_TIMESTAMP to System.currentTimeMillis(),
                BggContract.Plays.DELETE_TIMESTAMP to 0,
                BggContract.Plays.DIRTY_TIMESTAMP to 0,
            )
        )
        internalId
    }

    suspend fun markAsDeleted(internalId: Long): Long = withContext(Dispatchers.IO) {
        playDao.upsert(
            internalId,
            contentValuesOf(
                BggContract.Plays.DELETE_TIMESTAMP to System.currentTimeMillis(),
                BggContract.Plays.UPDATE_TIMESTAMP to 0,
                BggContract.Plays.DIRTY_TIMESTAMP to 0,
            )
        )
        internalId
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

    fun renameLocation(
        oldLocationName: String,
        newLocationName: String,
        resultLiveData: MutableLiveData<RenameLocationResults>? = null
    ) {
        val batch = ArrayList<ContentProviderOperation>()

        val values = contentValuesOf(BggContract.Plays.LOCATION to newLocationName)
        var cpo = ContentProviderOperation
            .newUpdate(BggContract.Plays.CONTENT_URI)
            .withValues(values)
            .withSelection(
                "${BggContract.Plays.LOCATION}=? AND (${BggContract.Plays.UPDATE_TIMESTAMP.greaterThanZero()} OR ${BggContract.Plays.DIRTY_TIMESTAMP.greaterThanZero()})",
                arrayOf(oldLocationName)
            )
        batch.add(cpo.build())

        values.put(BggContract.Plays.UPDATE_TIMESTAMP, System.currentTimeMillis())
        cpo = ContentProviderOperation
            .newUpdate(BggContract.Plays.CONTENT_URI)
            .withValues(values)
            .withSelection(
                "${BggContract.Plays.LOCATION}=? AND ${BggContract.Plays.UPDATE_TIMESTAMP.whereZeroOrNull()} AND ${BggContract.Plays.DELETE_TIMESTAMP.whereZeroOrNull()} AND ${BggContract.Plays.DIRTY_TIMESTAMP.whereZeroOrNull()}",
                arrayOf(oldLocationName)
            )
        batch.add(cpo.build())
        application.appExecutors.diskIO.execute {
            val results = application.contentResolver.applyBatch(batch)
            val result = RenameLocationResults(oldLocationName, newLocationName, results.sumBy { it.count ?: 0 })
            resultLiveData?.postValue(result)
        }
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

    fun save(play: PlayEntity, insertedId: MutableLiveData<Long>) {
        application.appExecutors.diskIO.execute {
            val id = playDao.save(play)

            // if the play is "current" (for today and about to be synced), remember some things, like the location and players to be used in the next play
            val isUpdating = play.updateTimestamp > 0
            val endTime = play.dateInMillis + min(60 * 24, play.length) * 60 * 1000
            val isToday = play.dateInMillis.isToday() || endTime.isToday()
            if (!play.isSynced && isUpdating && isToday) {
                prefs.putLastPlayTime(System.currentTimeMillis())
                prefs.putLastPlayLocation(play.location)
                prefs.putLastPlayPlayerEntities(play.players)
            }

            insertedId.postValue(id)
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
                                PendingIntent.FLAG_UPDATE_CURRENT
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
