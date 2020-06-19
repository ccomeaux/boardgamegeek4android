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
import androidx.lifecycle.Transformations
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.auth.AccountUtils
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.db.GameDao
import com.boardgamegeek.db.PlayDao
import com.boardgamegeek.entities.*
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.model.PlaysResponse
import com.boardgamegeek.livedata.RefreshableResourceLoader
import com.boardgamegeek.mappers.PlayMapper
import com.boardgamegeek.model.Play
import com.boardgamegeek.model.persister.PlayPersister
import com.boardgamegeek.pref.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.tasks.CalculatePlayStatsTask
import com.boardgamegeek.ui.PlayStatsActivity
import com.boardgamegeek.util.NotificationUtils
import com.boardgamegeek.util.RateLimiter
import retrofit2.Call
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

open class PlayRefresher

class PlayRepository(val application: BggApplication) : PlayRefresher() {
    private val playDao = PlayDao(application)
    private val gameDao = GameDao(application)
    private val collectionDao = CollectionDao(application)
    private val prefs: SharedPreferences by lazy { application.preferences() }

    fun getPlays(sortBy: PlayDao.PlaysSortBy = PlayDao.PlaysSortBy.DATE): LiveData<RefreshableResource<List<PlayEntity>>> {
        return object : PlayRefreshableResourceLoader(application) {
            override fun loadFromDatabase(): LiveData<List<PlayEntity>> {
                return playDao.loadPlays(sortBy)
            }
        }.asLiveData()
    }

    fun getPendingPlays(): LiveData<RefreshableResource<List<PlayEntity>>> {
        return object : PlayRefreshableResourceLoader(application) {
            override fun loadFromDatabase(): LiveData<List<PlayEntity>> {
                return playDao.loadPendingPlays()
            }
        }.asLiveData()
    }

    fun getDraftPlays(): LiveData<RefreshableResource<List<PlayEntity>>> {
        return object : PlayRefreshableResourceLoader(application) {
            override fun loadFromDatabase(): LiveData<List<PlayEntity>> {
                return playDao.loadDraftPlays()
            }
        }.asLiveData()
    }

    fun loadPlaysByGame(gameId: Int): LiveData<RefreshableResource<List<PlayEntity>>> {
        return object : PlayRefreshableResourceLoader(application) {
            override fun loadFromDatabase(): LiveData<List<PlayEntity>> {
                return playDao.loadPlaysByGame(gameId, PlayDao.PlaysSortBy.DATE)
            }

        }.asLiveData()
    }

    fun loadPlaysByLocation(location: String): LiveData<RefreshableResource<List<PlayEntity>>> {
        return object : PlayRefreshableResourceLoader(application) {
            override fun loadFromDatabase(): LiveData<List<PlayEntity>> {
                return playDao.loadPlaysByLocation(location)
            }

        }.asLiveData()
    }

    fun loadPlaysByUsername(username: String): LiveData<RefreshableResource<List<PlayEntity>>> {
        return object : PlayRefreshableResourceLoader(application) {
            override fun loadFromDatabase(): LiveData<List<PlayEntity>> {
                return playDao.loadPlaysByUsername(username)
            }
        }.asLiveData()
    }

    fun loadPlaysByPlayerName(playerName: String): LiveData<RefreshableResource<List<PlayEntity>>> {
        return object : PlayRefreshableResourceLoader(application) {
            override fun loadFromDatabase(): LiveData<List<PlayEntity>> {
                return playDao.loadPlaysByPlayerName(playerName)
            }
        }.asLiveData()
    }

    fun loadForStatsAsLiveData(includeIncomplete: Boolean, includeExpansions: Boolean, includeAccessories: Boolean):
            LiveData<List<GameForPlayStatEntity>> {
        // TODO use PlayDao if either of these is false
        // val isOwnedSynced = PreferencesUtils.isStatusSetToSync(application, BggService.COLLECTION_QUERY_STATUS_OWN)
        // val isPlayedSynced = PreferencesUtils.isStatusSetToSync(application, BggService.COLLECTION_QUERY_STATUS_PLAYED)

        return Transformations.map(gameDao.loadPlayInfoAsLiveData(
                includeIncomplete,
                includeExpansions,
                includeAccessories))
        {
            return@map filterGamesOwned(it)
        }
    }

    fun loadForStats(includeIncompletePlays: Boolean, includeExpansions: Boolean, includeAccessories: Boolean): List<GameForPlayStatEntity> {
        val playInfo = gameDao.loadPlayInfo(includeIncompletePlays, includeExpansions, includeAccessories)
        return filterGamesOwned(playInfo)
    }

    private fun filterGamesOwned(playInfo: List<GameForPlayStatEntity>): List<GameForPlayStatEntity> {
        val items = collectionDao.load()
        val games = mutableListOf<GameForPlayStatEntity>()
        playInfo.forEach { game ->
            games += game.copy(isOwned = items.any { item -> item.gameId == game.id && item.own })
        }
        return games.toList()
    }

    fun loadPlayers(sortBy: PlayDao.PlayerSortBy = PlayDao.PlayerSortBy.NAME): LiveData<List<PlayerEntity>> {
        return playDao.loadPlayersAsLiveData(sortBy)
    }

    fun loadPlayersByGame(gameId: Int): LiveData<List<PlayPlayerEntity>> {
        return playDao.loadPlayersByGame(gameId)
    }

    fun loadPlayersForStats(includeIncompletePlays: Boolean): List<PlayerEntity> {
        return playDao.loadPlayersForStats(includeIncompletePlays)
    }

    fun loadPlayersForStatsAsLiveData(includeIncompletePlays: Boolean): LiveData<List<PlayerEntity>> {
        return playDao.loadPlayersForStatsAsLiveData(includeIncompletePlays)
    }

    fun loadUserPlayer(username: String): LiveData<PlayerEntity> {
        return playDao.loadUserPlayerAsLiveData(username)
    }

    fun loadNonUserPlayer(playerName: String): LiveData<PlayerEntity> {
        return playDao.loadNonUserPlayerAsLiveData(playerName)
    }

    fun loadUserColorsAsLiveData(username: String): LiveData<List<PlayerColorEntity>> {
        return playDao.loadUserColorsAsLiveData(username)
    }

    fun loadUserColors(username: String): List<PlayerColorEntity> {
        return playDao.loadUserColors(username)
    }

    fun loadPlayerColorsAsLiveData(playerName: String): LiveData<List<PlayerColorEntity>> {
        return playDao.loadPlayerColorsAsLiveData(playerName)
    }

    fun loadPlayerColors(playerName: String): List<PlayerColorEntity> {
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

    fun loadLocations(sortBy: PlayDao.LocationSortBy = PlayDao.LocationSortBy.NAME): LiveData<List<LocationEntity>> {
        return playDao.loadLocationsAsLiveData(sortBy)
    }

    fun markAsDeleted(internalId: Long) {
        val values = contentValuesOf(
                BggContract.Plays.DELETE_TIMESTAMP to System.currentTimeMillis(),
                BggContract.Plays.UPDATE_TIMESTAMP to 0,
                BggContract.Plays.DIRTY_TIMESTAMP to 0
        )
        application.appExecutors.diskIO.execute {
            application.contentResolver.update(BggContract.Plays.buildPlayUri(internalId), values, null, null)
        }
    }

    fun updatePlaysWithNickName(username: String, nickName: String): Int {
        val count = playDao.countNickNameUpdatePlays(username, nickName)
        val batch = arrayListOf<ContentProviderOperation>()
        batch += playDao.createDirtyPlaysForUserAndNickNameOperations(username, nickName)
        batch += playDao.createNickNameUpdateOperation(username, nickName)
        application.appExecutors.diskIO.execute {
            application.contentResolver.applyBatch(batch)
        }
        return count
    }

    fun renamePlayer(oldName: String, newName: String) {
        val batch = arrayListOf<ContentProviderOperation>()
        batch += playDao.createDirtyPlaysForNonUserPlayerOperations(oldName)
        batch += playDao.createRenameUpdateOperation(oldName, newName)
        batch += playDao.createCopyPlayerColorsOperations(oldName, newName)
        batch += playDao.createDeletePlayerColorsOperation(oldName)
        application.appExecutors.diskIO.execute {
            application.contentResolver.applyBatch(batch)
        }
    }

    fun renameLocation(oldLocationName: String, newLocationName: String, count: MutableLiveData<Int>? = null) {
        val batch = ArrayList<ContentProviderOperation>()

        val values = contentValuesOf(BggContract.Plays.LOCATION to newLocationName)
        var cpo = ContentProviderOperation
                .newUpdate(BggContract.Plays.CONTENT_URI)
                .withValues(values)
                .withSelection("${BggContract.Plays.LOCATION}=? AND (${BggContract.Plays.UPDATE_TIMESTAMP.greaterThanZero()} OR ${BggContract.Plays.DIRTY_TIMESTAMP.greaterThanZero()})", arrayOf(oldLocationName))
        batch.add(cpo.build())

        values.put(BggContract.Plays.UPDATE_TIMESTAMP, System.currentTimeMillis())
        cpo = ContentProviderOperation
                .newUpdate(BggContract.Plays.CONTENT_URI)
                .withValues(values)
                .withSelection("${BggContract.Plays.LOCATION}=? AND ${BggContract.Plays.UPDATE_TIMESTAMP.whereZeroOrNull()} AND ${BggContract.Plays.DELETE_TIMESTAMP.whereZeroOrNull()} AND ${BggContract.Plays.DIRTY_TIMESTAMP.whereZeroOrNull()}", arrayOf(oldLocationName))
        batch.add(cpo.build())
        application.appExecutors.diskIO.execute {
            val results = application.contentResolver.applyBatch(batch)
            count?.postValue(results.sumBy { it.count })
        }
    }

    fun addUsernameToPlayer(playerName: String, username: String) {
        // TODO verify username is good
        val batch = arrayListOf<ContentProviderOperation>()
        batch += playDao.createDirtyPlaysForNonUserPlayerOperations(playerName)
        batch += playDao.createAddUsernameOperation(playerName, username)
        batch += playDao.createCopyPlayerColorsToUserOperations(playerName, username)
        batch += playDao.createDeletePlayerColorsOperation(playerName)
        application.appExecutors.diskIO.execute {
            application.contentResolver.applyBatch(batch)
        }
    }

    fun updateGameHIndex(hIndex: HIndexEntity) {
        updateHIndex(application, hIndex, PlayStats.KEY_GAME_H_INDEX, R.string.game, NOTIFICATION_ID_PLAY_STATS_GAME_H_INDEX)
    }

    fun updatePlayerHIndex(hIndex: HIndexEntity) {
        updateHIndex(application, hIndex, PlayStats.KEY_PLAYER_H_INDEX, R.string.player, NOTIFICATION_ID_PLAY_STATS_PLAYER_H_INDEX)
    }

    private fun updateHIndex(context: Context, hIndex: HIndexEntity, key: String, @StringRes typeResId: Int, notificationId: Int) {
        if (hIndex.h != HIndexEntity.INVALID_H_INDEX) {
            val old = HIndexEntity(prefs[key, 0] ?: 0, prefs[key + PlayStats.KEY_H_INDEX_N_SUFFIX, 0] ?: 0)
            if (old != hIndex) {
                prefs[key] = hIndex.h
                prefs[key + PlayStats.KEY_H_INDEX_N_SUFFIX] = hIndex.n
                @StringRes val messageId = if (hIndex.h > old.h || hIndex.h == old.h && hIndex.n < old.n) R.string.sync_notification_h_index_increase else R.string.sync_notification_h_index_decrease
                NotificationUtils.notify(context, NotificationUtils.TAG_PLAY_STATS, notificationId,
                        NotificationUtils.createNotificationBuilder(context, R.string.title_play_stats, NotificationUtils.CHANNEL_ID_STATS, PlayStatsActivity::class.java)
                                .setContentText(context.getText(messageId, context.getString(typeResId), hIndex.description))
                                .setContentIntent(PendingIntent.getActivity(context, 0, Intent(context, PlayStatsActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT)))
            }
        }
    }

    companion object {
        private const val NOTIFICATION_ID_PLAY_STATS_GAME_H_INDEX = 0
        private const val NOTIFICATION_ID_PLAY_STATS_PLAYER_H_INDEX = 1

    }

    abstract class PlayRefreshableResourceLoader(application: BggApplication) : RefreshableResourceLoader<List<PlayEntity>, PlaysResponse>(application) {
        private val username: String? by lazy {
            AccountUtils.getUsername(application)
        }
        private val syncPrefs: SharedPreferences by lazy { SyncPrefs.getPrefs(application.applicationContext) }
        private val prefs: SharedPreferences by lazy { application.preferences() }

        private val persister = PlayPersister(application)
        private var syncInitiatedTimestamp = 0L
        private val newestTimestamp = syncPrefs.getPlaysNewestTimestamp()
        private val oldestTimestamp = syncPrefs.getPlaysOldestTimestamp()
        private val mapper = PlayMapper()
        private var refreshingNewest = false
        private var lastNewPage = 0
        private val playsRateLimiter = RateLimiter<Int>(10, TimeUnit.MINUTES)
        private val playDao = PlayDao(application)

        override val typeDescriptionResId: Int
            get() = R.string.title_plays

        override fun shouldRefresh(data: List<PlayEntity>?): Boolean {
            return prefs[PREFERENCES_KEY_SYNC_PLAYS, false] == true && playsRateLimiter.shouldProcess(0)
        }

        override fun createCall(page: Int): Call<PlaysResponse> {
            if (page == 1) {
                syncInitiatedTimestamp = System.currentTimeMillis()
                refreshingNewest = true
            }
            if (refreshingNewest) lastNewPage = page
            return if (refreshingNewest) {
                Adapter.createForXml().plays(username,
                        newestTimestamp?.asDateForApi(),
                        null,
                        page)
            } else {
                Adapter.createForXml().plays(username,
                        null,
                        oldestTimestamp.asDateForApi(),
                        page - lastNewPage)
            }
        }

        override fun saveCallResult(result: PlaysResponse) {
            val plays = mapper.map(result.plays)
            persister.save(plays, syncInitiatedTimestamp)
            updateTimestamps(plays)
            Timber.i("Synced page %,d of plays", 1)
        }

        override fun hasMorePages(result: PlaysResponse): Boolean {
            return when {
                result.hasMorePages() -> true
                refreshingNewest -> {
                    refreshingNewest = false
                    oldestTimestamp > 0L
                }
                else -> false
            }
        }

        override fun onRefreshSucceeded() {
            newestTimestamp?.let { playDao.deleteUnupdatedPlaysSince(syncInitiatedTimestamp, it) }
            if (oldestTimestamp > 0L) {
                playDao.deleteUnupdatedPlaysBefore(syncInitiatedTimestamp, oldestTimestamp)
            } else {
                syncPrefs.setPlaysOldestTimestamp(0L)
            }
            CalculatePlayStatsTask(application).executeAsyncTask()
        }

        override fun onRefreshFailed() {
            playsRateLimiter.reset(0)
        }

        override fun onRefreshCancelled() {
            playsRateLimiter.reset(0)
        }

        private fun updateTimestamps(plays: List<Play>?) {
            val newestDate = plays?.maxBy { it.dateInMillis }?.dateInMillis ?: 0L
            if (newestDate > syncPrefs.getPlaysNewestTimestamp() ?: 0L) {
                syncPrefs.setPlaysNewestTimestamp(newestDate)
            }
            val oldestDate = plays?.minBy { it.dateInMillis }?.dateInMillis ?: Long.MAX_VALUE
            if (oldestDate < SyncPrefs.getPrefs(application).getPlaysOldestTimestamp()) {
                syncPrefs.setPlaysOldestTimestamp(oldestDate)
            }
        }
    }
}
