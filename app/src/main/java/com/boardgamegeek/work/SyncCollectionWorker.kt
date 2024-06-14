package com.boardgamegeek.work

import android.content.Context
import android.content.SharedPreferences
import android.text.format.DateUtils
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.BggService
import com.boardgamegeek.pref.*
import com.boardgamegeek.repository.GameCollectionRepository
import com.boardgamegeek.repository.GameRepository
import com.boardgamegeek.util.RemoteConfig
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import retrofit2.HttpException
import timber.log.Timber
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.days

@HiltWorker
class SyncCollectionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val gameCollectionRepository: GameCollectionRepository,
    private val gameRepository: GameRepository,
) : CoroutineWorker(appContext, workerParams) {
    private val prefs: SharedPreferences by lazy { appContext.preferences() }
    private val syncPrefs: SharedPreferences by lazy { SyncPrefs.getPrefs(appContext) }
    private var quickSync = false

    private val statusDescriptions = applicationContext.resources.getStringArray(R.array.pref_sync_status_values)
        .zip(applicationContext.resources.getStringArray(R.array.pref_sync_status_entries))
        .toMap()

    override suspend fun doWork(): Result {
        quickSync = inputData.getBoolean(QUICK_SYNC, false)
        val gamesFetchMax = RemoteConfig.getBoolean(RemoteConfig.KEY_SYNC_ENABLED)
        if (!gamesFetchMax)
            return Result.success(workDataOf(ERROR_MESSAGE to applicationContext.getString(R.string.msg_refresh_not_enabled)))

        if (!Authenticator.isSignedIn(applicationContext))
            return Result.success(workDataOf(ERROR_MESSAGE to applicationContext.getString(R.string.msg_refresh_collection_item_auth_error)))

        refreshCollection()
        if (isStopped) return Result.failure(workDataOf(STOPPED_REASON to "Canceled after refreshing collection"))
        syncUnupdatedCollection()?.let { return Result.failure(it) }
        if (isStopped) return Result.failure(workDataOf(STOPPED_REASON to "Canceled after syncing unupdated collection"))
        removeGames()
        if (isStopped) return Result.failure(workDataOf(STOPPED_REASON to "Canceled after removing old games"))
        downloadGames()?.let { return Result.failure(it) }
        return Result.success()
    }

    private suspend fun refreshCollection(): Data? {
        Timber.i("Refreshing collection")
        if (!prefs.isCollectionSetToSync()) {
            Timber.i("Collection not set to sync")
            return null
        }

        setForeground(createForegroundInfo(applicationContext.getString(R.string.sync_notification_title_collection)))

        return if (syncPrefs.getCurrentCollectionSyncTimestamp() == 0L) {
            val fetchIntervalInDays = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_COLLECTION_FETCH_INTERVAL_DAYS)
            val lastCompleteSync = syncPrefs.getLastCompleteCollectionTimestamp()
            if (lastCompleteSync == 0L || lastCompleteSync.isOlderThan(fetchIntervalInDays.days)) {
                Timber.i("It's been more than $fetchIntervalInDays days since we synced completely [${lastCompleteSync.toDateTime()}]; syncing entire collection")
                syncPrefs.setCurrentCollectionSyncTimestamp()
                syncCompleteCollection()
            } else {
                Timber.i("It's been less than $fetchIntervalInDays days since we synced completely [${lastCompleteSync.toDateTime()}]; syncing recently modified collection instead")
                syncCollectionModifiedSince()
            }
        } else {
            Timber.i("Continuing an in-progress sync of entire collection")
            syncCompleteCollection()
        }
    }

    private suspend fun syncCompleteCollection(): Data? {
        setForeground(createForegroundInfo(applicationContext.getString(R.string.sync_notification_collection_full)))

        val statuses: List<String> = prefs.getSyncStatusesOrDefault().toMutableList().apply {
            // Played games should be synced first - they don't respect the "exclude" flag
            if (remove(BggService.COLLECTION_QUERY_STATUS_PLAYED)) {
                add(0, BggService.COLLECTION_QUERY_STATUS_PLAYED)
            }
        }

        for (i in statuses.indices) {
            val status = statuses[i]
            val excludedStatuses = (0 until i).map { statuses[it] }
            syncCompleteCollectionByStatus(null, status, excludedStatuses)
            syncCompleteCollectionByStatus(BggService.ThingSubtype.BOARDGAME_ACCESSORY, status, excludedStatuses)
        }

        if (isStopped) return workDataOf(STOPPED_REASON to "Complete collection sync task cancelled before item deletion, aborting")

        deleteUnusedItems()

        syncPrefs.setLastCompleteCollectionTimestamp(syncPrefs.getCurrentCollectionSyncTimestamp())
        syncPrefs.setCurrentCollectionSyncTimestamp(0L)

        Timber.i("Complete collection synced successfully")
        return null
    }

    private suspend fun syncCompleteCollectionByStatus(
        subtype: BggService.ThingSubtype? = null,
        status: String,
        excludedStatuses: List<String>
    ): Data? {
        val statusDescription = statusDescriptions[status]
        val subtypeDescription = subtype.getDescription(applicationContext)

        if (syncPrefs.getCompleteCollectionSyncTimestamp(subtype, status) > syncPrefs.getCurrentCollectionSyncTimestamp()) {
            Timber.i("Skipping $statusDescription collection $subtypeDescription that have already been synced in the current sync request.")
            return null
        }

        if (isStopped) return workDataOf(STOPPED_REASON to "Complete collection sync stopped before status=[$statusDescription], subtype=[$subtypeDescription]")

        Timber.i("Syncing $statusDescription collection $subtypeDescription while excluding statuses [${excludedStatuses.formatList()}]")

        val contentText = applicationContext.getString(R.string.sync_notification_collection_detail, statusDescription, subtypeDescription)
        setForeground(createForegroundInfo(contentText))

        val updatedTimestamp = System.currentTimeMillis()
        val result = performSync(updatedTimestamp, subtype, null, status, excludedStatuses, errorMessage = contentText)
        if (result != null) syncPrefs.setCompleteCollectionSyncTimestamp(subtype, status, updatedTimestamp)
        return result
    }

    private suspend fun syncCollectionModifiedSince(): Data? {
        Timber.i("Starting to sync recently modified collection")
        return try {
            syncCollectionModifiedSinceBySubtype(null)?.let { return it }

            if (isStopped) return workDataOf(STOPPED_REASON to "Sync stopped before recently modified accessories, aborting")
            delay(RemoteConfig.getLong(RemoteConfig.KEY_SYNC_COLLECTION_FETCH_PAUSE_MILLIS))

            syncCollectionModifiedSinceBySubtype(BggService.ThingSubtype.BOARDGAME_ACCESSORY)?.let { return it }

            syncPrefs.setPartialCollectionSyncLastCompletedAt()
            Timber.i("Syncing recently modified collection completed successfully")
            null
        } catch (e: Exception) {
            handleException(applicationContext.getString(R.string.sync_notification_collection_partial), e)
        }
    }

    private suspend fun syncCollectionModifiedSinceBySubtype(subtype: BggService.ThingSubtype?): Data? {
        Timber.i("Starting to sync recently modified subtype [${subtype?.code ?: "<none>"}]")
        val previousSyncTimestamp = syncPrefs.getPartialCollectionSyncLastCompletedAt(subtype)
        if (previousSyncTimestamp > syncPrefs.getPartialCollectionSyncLastCompletedAt()) {
            Timber.i("Subtype [${subtype?.code ?: "<none>"}] has been synced in the current sync request; aborting")
            return null
        }

        val contentText = applicationContext.getString(
            R.string.sync_notification_collection_since,
            subtype.getDescription(applicationContext),
            previousSyncTimestamp.toDateTime()
        )
        setForeground(createForegroundInfo(contentText))

        val updatedTimestamp = System.currentTimeMillis()
        val result = performSync(updatedTimestamp, subtype, previousSyncTimestamp, errorMessage = contentText)
        if (result == null) syncPrefs.setPartialCollectionSyncLastCompletedAt(subtype, updatedTimestamp)
        return result
    }

    private suspend fun syncUnupdatedCollection(): Data? {
        Timber.i("Starting to sync unupdated collection")
        setForeground(createForegroundInfo(applicationContext.getString(R.string.sync_notification_collection_unupdated)))
        return try {
            val gameList = gameCollectionRepository.loadUnupdatedItems()
            Timber.i("Found %,d unupdated collection items to update", gameList.size)

            val maxFetches = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_COLLECTION_FETCH_MAX)
                .coerceIn(1, if (quickSync) 1 else 100)
            val chunkSize = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_COLLECTION_GAMES_PER_FETCH)
                .coerceIn(1, if (quickSync) 8 else 32)
            val chunkedGames = gameList.toList().chunked(chunkSize)
            chunkedGames.forEachIndexed { numberOfFetches, games ->
                if (numberOfFetches >= maxFetches) return null
                if (numberOfFetches > 0) delay(RemoteConfig.getLong(RemoteConfig.KEY_SYNC_COLLECTION_FETCH_PAUSE_MILLIS))
                if (isStopped) return workDataOf(STOPPED_REASON to "Unupdated collection item sync stopped during index=$numberOfFetches")

                val gameDescription = games.map { it.game.gameName }.formatList()
                listOf(null, BggService.ThingSubtype.BOARDGAME_ACCESSORY).forEach { subtype ->
                    val contentText = applicationContext.getString(
                        R.string.sync_notification_collection_update_games,
                        games.size,
                        subtype.getDescription(applicationContext),
                        gameDescription
                    )
                    setForeground(createForegroundInfo(contentText))
                    performSync(subtype = subtype, gameIds = games.map { it.game.gameId }, errorMessage = contentText)?.let { return it }
                }
            }
            Timber.i("Unupdated collection synced successfully")
            null
        } catch (e: Exception) {
            handleException(applicationContext.getString(R.string.sync_notification_collection_unupdated), e)
        }
    }

    private suspend fun performSync(
        updatedTimestamp: Long = System.currentTimeMillis(),
        subtype: BggService.ThingSubtype? = null,
        sinceTimestamp: Long? = null,
        status: String? = null,
        excludedStatuses: List<String>? = null,
        gameIds: List<Int>? = null,
        errorMessage: String = "",
    ): Data? {
        val options = mutableMapOf(
            BggService.COLLECTION_QUERY_KEY_STATS to "1",
            BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE to "1",
        )
        sinceTimestamp?.let {
            options[BggService.COLLECTION_QUERY_KEY_MODIFIED_SINCE] = BggService.COLLECTION_QUERY_DATE_TIME_FORMAT.format(Date(it))
        }
        status?.let { options[it] = "1" }
        subtype?.let { options[BggService.COLLECTION_QUERY_KEY_SUBTYPE] = it.code }
        excludedStatuses?.let { for (excludedStatus in it) options[excludedStatus] = "0" }
        gameIds?.let { options[BggService.COLLECTION_QUERY_KEY_ID] = it.joinToString(",") }

        return try {
            val result = gameCollectionRepository.refresh(options, updatedTimestamp)
            if (result.isSuccess) {
                val subtypeDescription = subtype.getDescription(applicationContext).lowercase()
                val stat = if (status != null) " of status $status" else ""
                val modified = if (sinceTimestamp != null) " modified since ${sinceTimestamp.toDateTime()}" else ""
                val games = if (gameIds != null) " of game IDs of ${gameIds.formatList()}" else ""
                Timber.i("Saved ${result.getOrNull() ?: 0} collection $subtypeDescription" + stat + modified + games)
                null
            } else handleException(errorMessage, result.exceptionOrNull())
        } catch (e: Exception) {
            handleException(errorMessage, e)
        }
    }

    private suspend fun deleteUnusedItems() {
        val timestamp = syncPrefs.getCurrentCollectionSyncTimestamp()
        Timber.i("Deleting collection items not updated since ${timestamp.toDateTime()}")
        val count = gameCollectionRepository.deleteUnupdatedItems(timestamp)
        Timber.i("Deleted $count old collection items")
        // TODO: delete thumbnail images associated with this list (both collection and game)
    }

    private suspend fun removeGames() {
        Timber.i("Removing games not in the collection")
        setForeground(createForegroundInfo(applicationContext.getString(R.string.sync_notification_collection_missing)))

        val sinceTimestamp = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_GAMES_DELETE_VIEW_HOURS).hoursAgo()
        Timber.i("Finding games to delete that aren't in the collection and have not been viewed since ${sinceTimestamp.toDateTime()}")

        val games = gameRepository.loadDeletableGames(sinceTimestamp, prefs.isStatusSetToSync(COLLECTION_STATUS_PLAYED))
        if (games.isNotEmpty()) {
            Timber.i("Found ${games.size} games to delete: ${games.map { "[${it.first}] ${it.second}" }}")
            setForeground(createForegroundInfo(applicationContext.resources.getQuantityString(R.plurals.sync_notification_games_remove, games.size, games.size)))

            var count = 0
            // NOTE: We're deleting one at a time, because a batch doesn't perform the game/collection join
            for ((gameId, gameName) in games) {
                Timber.i("Deleting game $gameName [$gameId]")
                count += gameRepository.delete(gameId)
            }
            Timber.i("Deleted $count games")
        } else {
            Timber.i("No games need deleting")
        }
    }

    private suspend fun downloadGames(): Data? {
        val timestamp = System.currentTimeMillis()

        val gamesFetchMaxUnupdated = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_GAMES_FETCH_MAX_UNUPDATED)
            .coerceIn(1, if (quickSync) 8 else Int.MAX_VALUE)
        Timber.i("Refreshing $gamesFetchMaxUnupdated games that are missing details in the collection")
        setForeground(createForegroundInfo(applicationContext.getString(R.string.sync_notification_games_unupdated)))
        val games = gameRepository.loadUnupdatedGames(gamesFetchMaxUnupdated)
        refreshGames(games)?.let { return it }

        if (isStopped) return workDataOf(STOPPED_REASON to "Cancelled while refreshing games")

        val gamesFetchMax = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_GAMES_FETCH_MAX)
            .coerceIn(1, if (quickSync) 4 else Int.MAX_VALUE)
        Timber.i("Refreshing $gamesFetchMax oldest games in the collection")
        setForeground(createForegroundInfo(applicationContext.getString(R.string.sync_notification_games_oldest)))
        val staleGames = gameRepository.loadOldestUpdatedGames(gamesFetchMax, timestamp)
        refreshGames(staleGames)?.let { return it }

        return null
    }

    private suspend fun refreshGames(games: List<Pair<Int, String>>): Data? {
        var fetchSize = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_GAMES_PER_FETCH)
            .coerceIn(1..32)
        var data = refreshGameChunks(games.chunked(fetchSize))
        while (data != null && fetchSize > 1) {
            if (isStopped) return workDataOf(STOPPED_REASON to "Cancelled while refreshing games")
            Timber.w("Failed fetching chunks of size $fetchSize; trying again with size ${fetchSize / 2}")
            fetchSize /= 2
            data = refreshGameChunks(games.chunked(fetchSize))
        }
        return data
    }

    private suspend fun refreshGameChunks(games: List<List<Pair<Int, String>>>): Data? {
        var updatedCount = 0
        try {
            games.forEachIndexed { index, gameChunk ->
                Timber.i("Refreshing game chunk ${(index + 1)} of ${games.size} - $gameChunk")
                if (index > 0)
                    delay(RemoteConfig.getLong(RemoteConfig.KEY_SYNC_GAMES_FETCH_PAUSE_MILLIS))
                val result = gameRepository.refreshGame(*gameChunk.map { it.first }.toIntArray())
                if (result.isSuccess) {
                    updatedCount += result.getOrElse { gameChunk.size }
                    Timber.i("Refreshed game chunk ${(index + 1)} of ${games.size} - $gameChunk")
                } else {
                    result.exceptionOrNull()?.let {
                        return handleException(applicationContext.getString(R.string.sync_notification_games_oldest), it)
                    }
                }
                if (isStopped) return workDataOf(STOPPED_REASON to "Cancelled while refreshing games")
            }
        } catch (e: Exception) {
            return handleException(applicationContext.getString(R.string.sync_notification_games_unupdated), e)
        }
        Timber.i("Refreshed $updatedCount games")
        return null
    }

    private fun handleException(contentText: String, throwable: Throwable?): Data {
        if (throwable is CancellationException) {
            Timber.i("Canceling collection sync")
        } else {
            Timber.e(throwable)
            val bigText = if (throwable is HttpException)
                throwable.code().asHttpErrorMessage(applicationContext)
            else
                throwable?.localizedMessage  ?: "Unknown exception while syncing collection"
            applicationContext.notifySyncError(contentText, bigText)
        }
        return workDataOf(ERROR_MESSAGE to (throwable?.message ?: "Unknown exception while syncing collection"))
    }

    private fun Long.toDateTime() = this.formatDateTime(
        applicationContext,
        flags = DateUtils.FORMAT_ABBREV_ALL or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME
    )

    private fun createForegroundInfo(contentText: String): ForegroundInfo {
        return applicationContext.createForegroundInfo(R.string.sync_notification_title_collection, NOTIFICATION_ID_COLLECTION, id, contentText)
    }

    companion object {
        const val UNIQUE_WORK_NAME = "com.boardgamegeek.COLLECTION.DOWNLOAD"
        const val ERROR_MESSAGE = "ERROR_MESSAGE"
        const val STOPPED_REASON = "STOPPED_REASON"
        private const val QUICK_SYNC = "QUICK_SYNC"

        fun requestSync(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<SyncCollectionWorker>()
                .setConstraints(context.createWorkConstraints(true))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }

        fun buildQuickRequest(context: Context) = OneTimeWorkRequestBuilder<SyncCollectionWorker>()
            .setInputData(workDataOf(QUICK_SYNC to true)) // limits the number of games downloaded
            .setConstraints(context.createWorkConstraints(true))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
            .build()
    }
}