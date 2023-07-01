package com.boardgamegeek.work

import android.content.Context
import android.content.SharedPreferences
import android.text.format.DateUtils
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.BggService
import com.boardgamegeek.pref.*
import com.boardgamegeek.repository.CollectionItemRepository
import com.boardgamegeek.repository.GameRepository
import com.boardgamegeek.util.RemoteConfig
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
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
    private val collectionItemRepository: CollectionItemRepository,
    private val gameRepository: GameRepository,
) : CoroutineWorker(appContext, workerParams) {
    private val prefs: SharedPreferences by lazy { appContext.preferences() }
    private val syncPrefs: SharedPreferences by lazy { SyncPrefs.getPrefs(appContext) }

    private val statusDescriptions = applicationContext.resources.getStringArray(R.array.pref_sync_status_values)
        .zip(applicationContext.resources.getStringArray(R.array.pref_sync_status_entries))
        .toMap()

    override suspend fun doWork(): Result {
        refreshCollection()
        if (isStopped) return Result.failure(workDataOf(STOPPED_REASON to "Canceled after refreshing collection"))
        syncUnupdatedCollection()
        if (isStopped) return Result.failure(workDataOf(STOPPED_REASON to "Canceled after syncing unupdated collection"))
        removeGames()
        if (isStopped) return Result.failure(workDataOf(STOPPED_REASON to "Canceled after removing old games"))
        downloadGames()
        return Result.success()
    }

    private suspend fun refreshCollection(): Result {
        Timber.i("Refreshing collection")
        if (!prefs.isCollectionSetToSync()) {
            Timber.i("Collection not set to sync")
            return Result.success()
        }

        return if (syncPrefs.getCurrentCollectionSyncTimestamp() == 0L) {
            val fetchIntervalInDays = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_COLLECTION_FETCH_INTERVAL_DAYS)
            val lastCompleteSync = syncPrefs.getLastCompleteCollectionTimestamp()
            if (lastCompleteSync > 0 && !lastCompleteSync.isOlderThan(fetchIntervalInDays.days)) {
                Timber.i("Not currently syncing the complete collection but it's been less than $fetchIntervalInDays days since we synced completely [${lastCompleteSync.toDateTime()}]; syncing recently modified collection instead")
                syncCollectionModifiedSince()
            } else {
                Timber.i("Not currently syncing the complete collection and it's been more than $fetchIntervalInDays days since we synced completely [${lastCompleteSync.toDateTime()}]; syncing entire collection")
                syncPrefs.setCurrentCollectionSyncTimestamp()
                syncCompleteCollection()
            }
        } else {
            Timber.i("Continuing an in-progress sync of entire collection")
            syncCompleteCollection()
        }
    }

    private suspend fun syncCompleteCollection(): Result {
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

        if (isStopped) return Result.failure(workDataOf(STOPPED_REASON to "Complete collection sync task cancelled before item deletion, aborting"))

        deleteUnusedItems()

        syncPrefs.setLastCompleteCollectionTimestamp(syncPrefs.getCurrentCollectionSyncTimestamp())
        syncPrefs.setPartialCollectionSyncLastCompletedAt(syncPrefs.getCurrentCollectionSyncTimestamp())
        syncPrefs.setCurrentCollectionSyncTimestamp(0L)

        Timber.i("Complete collection synced successfully")
        return Result.success()
    }

    private suspend fun syncCompleteCollectionByStatus(subtype: BggService.ThingSubtype? = null, status: String, excludedStatuses: List<String>): Result {
        val statusDescription = statusDescriptions[status]
        val subtypeDescription = subtype.getDescription(applicationContext)

        if (syncPrefs.getCompleteCollectionSyncTimestamp(subtype, status) > syncPrefs.getCurrentCollectionSyncTimestamp()) {
            Timber.i("Skipping $statusDescription collection $subtypeDescription that have already been synced in the current sync request.")
            return Result.success()
        }

        if (isStopped) return Result.failure(workDataOf(STOPPED_REASON to "Complete collection sync stopped before status=[$statusDescription], subtype=[$subtypeDescription]"))

        Timber.i("Syncing $statusDescription collection $subtypeDescription while excluding statuses [${excludedStatuses.formatList()}]")

        val contentText = applicationContext.getString(R.string.sync_notification_collection_detail, statusDescription, subtypeDescription)
        setForeground(createForegroundInfo(contentText))

        val timestamp = System.currentTimeMillis()
        val result = performSync(timestamp, subtype, null, status, excludedStatuses, errorMessage = contentText)
        if (result is Result.Success) syncPrefs.setCompleteCollectionSyncTimestamp(subtype, status, timestamp)
        return result
    }

    private suspend fun syncCollectionModifiedSince(): Result {
        Timber.i("Starting to sync recently modified collection")
        try {
            val itemResult = syncBySubtype(null)
            if (itemResult is Result.Failure) return itemResult

            if (isStopped) return Result.failure(workDataOf(STOPPED_REASON to "Sync stopped before recently modified accessories, aborting"))
            delay(RemoteConfig.getLong(RemoteConfig.KEY_SYNC_COLLECTION_FETCH_PAUSE_MILLIS))

            val accessoryResult = syncBySubtype(BggService.ThingSubtype.BOARDGAME_ACCESSORY)
            if (accessoryResult is Result.Failure) return accessoryResult

            syncPrefs.setPartialCollectionSyncLastCompletedAt()
            Timber.i("Syncing recently modified collection completed successfully")
            return Result.success()
        } catch (e: Exception) {
            return handleException(applicationContext.getString(R.string.sync_notification_collection_syncing), e)
        }
    }

    private suspend fun syncBySubtype(subtype: BggService.ThingSubtype?): Result {
        Timber.i("Starting to sync subtype [${subtype?.code ?: "<none>"}]")
        val lastStatusSync = syncPrefs.getPartialCollectionSyncLastCompletedAt(subtype)
        val lastPartialSync = syncPrefs.getPartialCollectionSyncLastCompletedAt()
        if (lastStatusSync > lastPartialSync) {
            Timber.i("Subtype [${subtype?.code ?: "<none>"}] has been synced in the current sync request; aborting")
            return Result.success()
        }

        val contentText = applicationContext.getString(R.string.sync_notification_collection_since, subtype.getDescription(applicationContext), lastStatusSync.toDateTime())
        setForeground(createForegroundInfo(contentText))

        val timestamp = System.currentTimeMillis()
        val result = performSync(timestamp, subtype, lastStatusSync, errorMessage = contentText)
        if (result is Result.Success) syncPrefs.setPartialCollectionSyncLastCompletedAt(subtype, timestamp)
        return result
    }

    private suspend fun syncUnupdatedCollection(): Result {
        Timber.i("Starting to sync unupdated collection")
        setForeground(createForegroundInfo(applicationContext.getString(R.string.sync_notification_collection_unupdated)))
        try {
            val gameList = collectionItemRepository.loadUnupdatedItems()
            Timber.i("Found %,d unupdated collection items to update", gameList.size)

            val chunkedGames = gameList.toList().chunked(RemoteConfig.getInt(RemoteConfig.KEY_SYNC_COLLECTION_GAMES_PER_FETCH))
            chunkedGames.forEachIndexed { numberOfFetches, games ->
                if (numberOfFetches >= RemoteConfig.getInt(RemoteConfig.KEY_SYNC_COLLECTION_FETCH_MAX)) return Result.success()
                if (numberOfFetches > 0) delay(RemoteConfig.getLong(RemoteConfig.KEY_SYNC_COLLECTION_FETCH_PAUSE_MILLIS))
                if (isStopped) return Result.failure(workDataOf(STOPPED_REASON to "Unupdated collection item sync stopped during index=$numberOfFetches"))

                val gameDescription = games.map { it.second }.toList().formatList()
                listOf(null, BggService.ThingSubtype.BOARDGAME_ACCESSORY).forEach { subtype ->
                    val contentText = applicationContext.getString(
                        R.string.sync_notification_collection_update_games,
                        games.size,
                        subtype.getDescription(applicationContext),
                        gameDescription
                    )
                    setForeground(createForegroundInfo(contentText))
                    val result = performSync(subtype = subtype, gameIds = games.map { it.first }, errorMessage = contentText)
                    if (result is Result.Failure) return result
                }
            }
            Timber.i("Unupdated collection synced successfully")
            return Result.success()
        } catch (e: Exception) {
            return handleException(applicationContext.getString(R.string.sync_notification_collection_unupdated), e)
        }
    }

    private suspend fun performSync(
        timestamp: Long = System.currentTimeMillis(),
        subtype: BggService.ThingSubtype? = null,
        sinceTimestamp: Long? = null,
        status: String? = null,
        excludedStatuses: List<String>? = null,
        gameIds: List<Int>? = null,
        errorMessage: String = "",
    ): Result {
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

        val result = try {
            val count = collectionItemRepository.refresh(options, timestamp)
            Timber.i(
                "Saved $count collection ${subtype.getDescription(applicationContext).lowercase()}" +
                        if (status != null) " of status $status" else "" +
                                if (sinceTimestamp != null) " modified since ${sinceTimestamp.toDateTime()}" else "" +
                                        if (gameIds != null) " of game IDs of ${gameIds.formatList()}" else ""
            )
            Result.success()
        } catch (e: Exception) {
            handleException(errorMessage, e)
        }
        return result
    }

    private suspend fun deleteUnusedItems() {
        val timestamp = syncPrefs.getCurrentCollectionSyncTimestamp()
        val formattedDateTime = timestamp.formatDateTime(
            applicationContext,
            flags = DateUtils.FORMAT_ABBREV_ALL or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME
        )
        Timber.i("Deleting collection items not updated since $formattedDateTime")
        val count = collectionItemRepository.deleteUnupdatedItems(timestamp)
        Timber.i("Deleted $count old collection items")
        // TODO: delete thumbnail images associated with this list (both collection and game)
    }

    private suspend fun removeGames(): Result {
        Timber.i("Removing games not in the collection")
        setForeground(createForegroundInfo(applicationContext.getString(R.string.sync_notification_collection_missing)))

        val hoursAgo = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_GAMES_DELETE_VIEW_HOURS).hoursAgo()
        val date = DateUtils.formatDateTime(
            applicationContext,
            hoursAgo,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NUMERIC_DATE or DateUtils.FORMAT_SHOW_TIME
        )
        Timber.i("Fetching games that aren't in the collection and have not been viewed since $date")

        val games = gameRepository.loadDeletableGames(hoursAgo, prefs.isStatusSetToSync(COLLECTION_STATUS_PLAYED))
        if (games.isNotEmpty()) {
            Timber.i("Found ${games.size} games to delete: ${games.map { "[${it.first}] ${it.second}" }}")
            setForeground(
                createForegroundInfo(
                    applicationContext.resources.getQuantityString(
                        R.plurals.sync_notification_games_remove,
                        games.size,
                        games.size
                    )
                )
            )

            var count = 0
            // NOTE: We're deleting one at a time, because a batch doesn't perform the game/collection join
            for ((gameId, _) in games) {
                Timber.i("Deleting game ID=${gameId}")
                count += gameRepository.delete(gameId)
            }
            Timber.i("Deleted $count games")
        } else {
            Timber.i("No games need deleting")
        }
        return Result.success()
    }

    private suspend fun downloadGames(): Result {
        var updatedCount = 0
        val gamesPerFetch = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_GAMES_PER_FETCH)

        Timber.i("Refreshing $gamesPerFetch oldest games in the collection")
        setForeground(createForegroundInfo(applicationContext.getString(R.string.sync_notification_games_oldest)))
        val staleGames = gameRepository.loadOldestUpdatedGames(gamesPerFetch)
        staleGames.forEach { (gameId, gameName) ->
            Timber.i("Refreshing game $gameName [$gameId]")
            delay(RemoteConfig.getLong(RemoteConfig.KEY_SYNC_GAMES_FETCH_PAUSE_MILLIS))
            try {
                updatedCount += gameRepository.refreshGame(gameId)
                Timber.i("Refreshed game $gameName [$gameId]")
            } catch (e: Exception) {
                handleException(applicationContext.getString(R.string.sync_notification_games_oldest), e)
            }
        }

        Timber.i("Refreshing $gamesPerFetch games that are missing details in the collection")
        setForeground(createForegroundInfo(applicationContext.getString(R.string.sync_notification_games_unupdated)))
        val games = gameRepository.loadUnupdatedGames(gamesPerFetch)
        games.forEach { (gameId, gameName) ->
            Timber.i("Refreshing game $gameName [$gameId]")
            delay(RemoteConfig.getLong(RemoteConfig.KEY_SYNC_GAMES_FETCH_PAUSE_MILLIS))
            try {
                updatedCount += gameRepository.refreshGame(gameId)
                Timber.i("Refreshed game $gameName [$gameId]")
            } catch (e: Exception) {
                handleException(applicationContext.getString(R.string.sync_notification_games_unupdated), e)
            }
        }

        Timber.i("Refreshed $updatedCount games")
        return Result.success()
    }

    private fun handleException(contentText: String, e: Exception): Result {
        Timber.e(e)
        val bigText = if (e is HttpException) e.code().asHttpErrorMessage(applicationContext) else e.localizedMessage
        applicationContext.notifySyncError(contentText, bigText)
        return Result.failure(workDataOf(ERROR_MESSAGE to e.message))
    }

    private fun Long.toDateTime() = this.formatDateTime(
        applicationContext,
        flags = DateUtils.FORMAT_ABBREV_ALL or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME
    )

    private fun createForegroundInfo(contentText: String): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, NotificationChannels.SYNC_PROGRESS)
            .setContentTitle(applicationContext.getString(R.string.sync_notification_title_collection))
            .setTicker(applicationContext.getString(R.string.sync_notification_title_collection))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_stat_bgg)
            .setColor(ContextCompat.getColor(applicationContext, R.color.primary))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setProgress(1, 0, true)
            .addAction(
                R.drawable.ic_baseline_clear_24,
                applicationContext.getString(R.string.cancel),
                WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)
            )
            .build()

        return ForegroundInfo(43, notification) // 43 is different than 42
    }

    companion object {
        const val UNIQUE_WORK_NAME = "com.boardgamegeek.SYNC_COLLECTION"
        const val ERROR_MESSAGE = "ERROR_MESSAGE"
        const val STOPPED_REASON = "STOPPED_REASON"

        fun requestSync(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<SyncCollectionWorker>()
                .setConstraints(context.createWorkConstraints(true))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}