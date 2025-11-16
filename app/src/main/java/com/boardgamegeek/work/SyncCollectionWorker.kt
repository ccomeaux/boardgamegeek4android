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
import com.boardgamegeek.mappers.mapToPreference
import com.boardgamegeek.model.CollectionStatus
import com.boardgamegeek.model.Game
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.pref.SyncPrefs.Companion.TIMESTAMP_COLLECTION_COMPLETE
import com.boardgamegeek.pref.SyncPrefs.Companion.TIMESTAMP_COLLECTION_COMPLETE_CURRENT
import com.boardgamegeek.pref.SyncPrefs.Companion.TIMESTAMP_COLLECTION_PARTIAL
import com.boardgamegeek.pref.getCompleteCollectionTimestampKey
import com.boardgamegeek.pref.getPartialCollectionTimestampKey
import com.boardgamegeek.repository.GameCollectionRepository
import com.boardgamegeek.repository.GameRepository
import com.boardgamegeek.util.RemoteConfig
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import retrofit2.HttpException
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

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
    private var requestedStatus: CollectionStatus = CollectionStatus.Unknown

    private val statusDescriptions = applicationContext.createStatusMap()

    override suspend fun doWork(): Result {
        quickSync = inputData.getBoolean(QUICK_SYNC, false)
        requestedStatus = inputData.getString(REQUESTED_STATUS).mapToEnum()

        val isSyncEnabled = RemoteConfig.getBoolean(RemoteConfig.KEY_SYNC_ENABLED)
        if (!isSyncEnabled)
            return Result.success(workDataOf(SUCCESS_MESSAGE to applicationContext.getString(R.string.msg_refresh_not_enabled)))

        if (!Authenticator.isSignedIn(applicationContext))
            return Result.success(workDataOf(SUCCESS_MESSAGE to applicationContext.getString(R.string.msg_refresh_collection_item_auth_error)))

        refreshCollection()
        failIfStopped("Canceled after refreshing collection")?.let { return it }
        syncUnupdatedCollection()?.let { return Result.failure(it) }
        failIfStopped("Canceled after syncing unupdated collection")?.let { return it }
        removeGames()
        failIfStopped("Canceled after removing old games")?.let { return it }
        refreshGames()?.run { return Result.failure(this) }
        return Result.success(workDataOf(SUCCESS_MESSAGE to applicationContext.getString(R.string.msg_refresh_success)))
    }

    private suspend fun refreshCollection(): Data? {
        Timber.i("Refreshing collection")
        if (!prefs.isCollectionSetToSync()) {
            Timber.i("Collection not set to sync")
            return null
        }

        if (prefs[KEY_SYNC_PROGRESS, false] ?: false)
            setForeground(createForegroundInfo(applicationContext.getString(R.string.sync_notification_title_collection)))

        return if (quickSync) {
            val lastPartialSync = syncPrefs[TIMESTAMP_COLLECTION_PARTIAL, 0L] ?: 0L
            if (lastPartialSync > 0L && lastPartialSync.isOlderThan(15.minutes)) {
                Timber.i("Quick sync requested; syncing recently modified collection")
                syncCollectionModifiedSince()
            } else {
                Timber.i("Quick sync requested; skipping collection sync entirely")
                null
            }
        } else if (requestedStatus != CollectionStatus.Unknown) {
            Timber.i("Syncing requested status of $requestedStatus")
            syncCompleteCollectionByStatus(requestedStatus)
        } else if (syncPrefs.getCurrentCollectionSyncTimestamp() == 0L) {
            val fetchIntervalInDays = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_COLLECTION_FETCH_INTERVAL_DAYS)
            val lastCompleteSync = syncPrefs[TIMESTAMP_COLLECTION_COMPLETE, 0L] ?: 0L
            val lastPartialSync = syncPrefs[TIMESTAMP_COLLECTION_PARTIAL, 0L] ?: 0L
            if (lastCompleteSync == 0L || lastCompleteSync.isOlderThan(fetchIntervalInDays.days)) {
                Timber.i("It's been more than $fetchIntervalInDays days since we synced completely [${lastCompleteSync.toDateTime()}]; syncing entire collection")
                syncPrefs[TIMESTAMP_COLLECTION_COMPLETE_CURRENT] = System.currentTimeMillis()
                syncCompleteCollection()
            } else if (lastPartialSync.isOlderThan(15.minutes)) {
                Timber.i("It's been less than $fetchIntervalInDays days since we synced completely [${lastCompleteSync.toDateTime()}]; syncing recently modified collection instead")
                syncCollectionModifiedSince()
            } else {
                Timber.i("It's been less than 15 minutes since we synced partially; skipping sync")
                null
            }
        } else {
            Timber.i("Continuing an in-progress sync of entire collection")
            syncCompleteCollection()
        }
    }

    private suspend fun syncCompleteCollection(): Data? {
        setProgress(PROGRESS_STEP_COLLECTION_COMPLETE)
        if (prefs[KEY_SYNC_PROGRESS, false] ?: false)
            setForeground(createForegroundInfo(applicationContext.getString(R.string.sync_notification_collection_full)))

        val statuses = prefs.getSyncStatuses().toMutableList().apply {
            // Played games should be synced first - they don't respect the "exclude" flag
            if (remove(CollectionStatus.Played)) {
                add(0, CollectionStatus.Played)
            }
        }

        for (i in statuses.indices) {
            val status = statuses[i]
            val excludedStatuses = (0 until i).map { statuses[it] }
            syncCompleteCollectionByStatus(null, status, excludedStatuses)?.let { return it }
            syncCompleteCollectionByStatus(Game.Subtype.BoardGameAccessory, status, excludedStatuses)?.let { return it }
        }

        checkIfStopped("Complete collection sync task canceled before item deletion, aborting")?.let { return it }

        deleteUnusedItems()

        syncPrefs[TIMESTAMP_COLLECTION_COMPLETE] = syncPrefs.getCurrentCollectionSyncTimestamp()
        syncPrefs[TIMESTAMP_COLLECTION_PARTIAL] = syncPrefs.getCurrentCollectionSyncTimestamp()
        syncPrefs[TIMESTAMP_COLLECTION_COMPLETE_CURRENT] = 0L

        Timber.i("Complete collection synced successfully")
        return null
    }

    private suspend fun syncCompleteCollectionByStatus(status: CollectionStatus): Data? {
        syncCompleteCollectionByStatus(null, status)?.let { return it }
        syncCompleteCollectionByStatus(Game.Subtype.BoardGameAccessory, status)?.let { return it }
        Timber.i("Complete collection sync for $status successfully")
        return null
    }

    private suspend fun syncCompleteCollectionByStatus(
        subtype: Game.Subtype? = null,
        status: CollectionStatus,
        excludedStatuses: List<CollectionStatus> = emptyList(),
    ): Data? {
        val statusDescription = statusDescriptions[status.mapToPreference()]
        val subtypeDescription = subtype.getDescription(applicationContext)

        val currentSyncTimestamp = syncPrefs.getCurrentCollectionSyncTimestamp()
        val lastStatusSyncTimestamp = syncPrefs[getCompleteCollectionTimestampKey(subtype, status), 0L] ?: 0L
        if (currentSyncTimestamp in 1..<lastStatusSyncTimestamp) {
            Timber.i("Skipping $statusDescription collection $subtypeDescription that have already been synced in the current sync request.")
            return null
        }

        checkIfStopped("Complete collection sync stopped before status=[$statusDescription], subtype=[$subtypeDescription]")?.let { return it }

        setProgress(
            PROGRESS_STEP_COLLECTION_COMPLETE,
            if (subtype == null) PROGRESS_SUBTYPE_ALL else PROGRESS_SUBTYPE_ACCESSORY,
            status,
        )
        Timber.i("Syncing $statusDescription collection $subtypeDescription while excluding statuses [${excludedStatuses.formatList()}]")

        val contentText = applicationContext.getString(R.string.sync_notification_collection_detail, statusDescription, subtypeDescription)
        if (prefs[KEY_SYNC_PROGRESS, false] ?: false)
            setForeground(createForegroundInfo(contentText))

        val updatedTimestamp = System.currentTimeMillis()
        val result = performSync(updatedTimestamp, subtype, null, status, excludedStatuses, errorMessage = contentText)
        if (result == null) syncPrefs[getCompleteCollectionTimestampKey(subtype, status)] = updatedTimestamp
        return result
    }

    private suspend fun syncCollectionModifiedSince(): Data? {
        Timber.i("Starting to sync recently modified collection")
        return try {
            syncCollectionModifiedSinceBySubtype(null)?.let { return it }

            checkIfStopped("Sync stopped before recently modified accessories, aborting")?.let { return it }
            delay(RemoteConfig.getLong(RemoteConfig.KEY_SYNC_COLLECTION_FETCH_PAUSE_MILLIS))

            syncCollectionModifiedSinceBySubtype(Game.Subtype.BoardGameAccessory)?.let { return it }

            syncPrefs[TIMESTAMP_COLLECTION_PARTIAL] = System.currentTimeMillis()
            Timber.i("Syncing recently modified collection completed successfully")
            null
        } catch (e: Exception) {
            handleException(applicationContext.getString(R.string.sync_notification_collection_partial), e)
        }
    }

    private suspend fun syncCollectionModifiedSinceBySubtype(subtype: Game.Subtype?): Data? {
        Timber.i("Starting to sync recently modified subtype [${subtype ?: "<none>"}]")
        val timestampKey = getPartialCollectionTimestampKey(subtype)
        val previousSyncTimestamp = syncPrefs[timestampKey, 0L] ?: 0L
        val lastPartialSync = syncPrefs[TIMESTAMP_COLLECTION_PARTIAL, 0L] ?: 0L
        val compareSync = if (lastPartialSync > 0L) lastPartialSync else syncPrefs[TIMESTAMP_COLLECTION_COMPLETE, 0L] ?: 0L
        if (previousSyncTimestamp > compareSync) {
            Timber.i("Subtype [${subtype ?: "<none>"}] has been synced in the current sync request; aborting")
            return null
        }

        setProgress(PROGRESS_STEP_COLLECTION_PARTIAL, if (subtype == null) PROGRESS_SUBTYPE_ALL else PROGRESS_SUBTYPE_ACCESSORY, modifiedSince = previousSyncTimestamp)

        val contentText = applicationContext.getString(
            R.string.sync_notification_collection_since,
            subtype.getDescription(applicationContext),
            previousSyncTimestamp.toDateTime()
        )
        if (prefs[KEY_SYNC_PROGRESS, false] ?: false)
            setForeground(createForegroundInfo(contentText))

        val updatedTimestamp = System.currentTimeMillis()
        val result = performSync(updatedTimestamp, subtype, previousSyncTimestamp, errorMessage = contentText)
        if (result == null) syncPrefs[timestampKey] = updatedTimestamp
        return result
    }

    private suspend fun syncUnupdatedCollection(): Data? {
        Timber.i("Starting to sync unupdated collection")
        setProgress(PROGRESS_STEP_COLLECTION_STALE)
        if (prefs[KEY_SYNC_PROGRESS, false] ?: false)
            setForeground(createForegroundInfo(applicationContext.getString(R.string.sync_notification_collection_unupdated)))

        return try {
            val gameList = gameCollectionRepository.loadUnupdatedItems()
            Timber.i("Found %,d unupdated collection items to update", gameList.size)

            val maxFetches = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_COLLECTION_FETCH_MAX).coerceIn(1, if (quickSync) 1 else 100)
            val chunkSize = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_COLLECTION_GAMES_PER_FETCH).coerceIn(1, if (quickSync) 8 else 32)
            val chunkedGames = gameList.chunked(chunkSize)
            chunkedGames.forEachIndexed { numberOfFetches, games ->
                if (numberOfFetches >= maxFetches) return null
                if (numberOfFetches > 0) delay(RemoteConfig.getLong(RemoteConfig.KEY_SYNC_COLLECTION_FETCH_PAUSE_MILLIS))
                checkIfStopped("Unupdated collection item sync stopped during index=$numberOfFetches")?.let { return it }

                val gameDescription = games.map { it.game.gameName }.formatList()
                listOf(null, Game.Subtype.BoardGameAccessory).forEach { subtype ->
                    val contentText = applicationContext.getString(
                        R.string.sync_notification_collection_update_games,
                        games.size,
                        subtype.getDescription(applicationContext),
                        gameDescription
                    )
                    if (prefs[KEY_SYNC_PROGRESS, false] ?: false)
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
        subtype: Game.Subtype? = null,
        sinceTimestamp: Long? = null,
        status: CollectionStatus = CollectionStatus.Unknown,
        excludedStatuses: List<CollectionStatus> = emptyList(),
        gameIds: List<Int>? = null,
        errorMessage: String = "",
    ): Data? {
        val options = BggService.createCollectionOptionsMap(
            subtype = subtype,
            sinceTimestamp = sinceTimestamp,
            status = status,
            excludedStatuses = excludedStatuses,
            gameIds = gameIds,
            includeStats = true,
            includePrivateInfo = true,
        )

        return try {
            val result = gameCollectionRepository.refresh(options, updatedTimestamp)
            if (result.isSuccess) {
                val subtypeDescription = subtype.getDescription(applicationContext).lowercase()
                val stat = if (status != CollectionStatus.Unknown) " of status $status" else ""
                val modified = if (sinceTimestamp != null) " modified since ${sinceTimestamp.toDateTime()}" else ""
                val games = if (gameIds != null) " of game IDs of ${gameIds.formatList()}" else ""
                Timber.i("Saved ${result.getOrNull() ?: 0} collection $subtypeDescription$stat$modified$games")
                null
            } else handleException(errorMessage, result.exceptionOrNull())
        } catch (e: Exception) {
            handleException(errorMessage, e)
        }
    }

    private suspend fun deleteUnusedItems() {
        setProgress(PROGRESS_STEP_COLLECTION_DELETE)
        if (prefs[KEY_SYNC_PROGRESS, false] ?: false)
            setForeground(createForegroundInfo(applicationContext.getString(R.string.sync_notification_collection_missing)))
        val timestamp = syncPrefs.getCurrentCollectionSyncTimestamp()
        Timber.i("Deleting collection items not updated since ${timestamp.toDateTime()}")
        val count = gameCollectionRepository.deleteUnupdatedItems(timestamp)
        Timber.i("Deleted $count old collection items")
        // TODO: delete thumbnail images associated with this list (both collection and game)
    }

    private suspend fun removeGames() {
        Timber.i("Removing games not in the collection")
        setProgress(PROGRESS_STEP_GAMES_REMOVE)

        val sinceTimestamp = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_GAMES_REMOVE_VIEW_HOURS).hoursAgo()
        Timber.i("Finding games to remove that aren't in the collection and have not been viewed since ${sinceTimestamp.toDateTime()}")

        val gamesToRemove = gameRepository.loadGamesByLastViewed(sinceTimestamp, prefs.isStatusSetToSync(CollectionStatus.Played))
        if (gamesToRemove.isNotEmpty()) {
            Timber.i("Found ${gamesToRemove.size} games to remove: ${gamesToRemove.map { "[${it.first}] ${it.second}" }}")
            if (prefs[KEY_SYNC_PROGRESS, false] ?: false)
                setForeground(createForegroundInfo(applicationContext.resources.getQuantityString(R.plurals.sync_notification_games_remove, gamesToRemove.size, gamesToRemove.size)))

            var count = 0
            // NOTE: We're deleting one at a time, because a batch doesn't perform the game/collection join
            for ((gameId, gameName) in gamesToRemove) {
                Timber.i("Removing game $gameName [$gameId]")
                count += gameRepository.delete(gameId)
            }
            Timber.i("Removed $count games")
        } else {
            Timber.i("No games need remove")
        }
    }

    /**
     * Refresh games that are missing details in the collection as well as the oldest few games in the collection.
     * Returns a data object containing the error message, null if successful.
     */
    private suspend fun refreshGames(): Data? {
        val startTimestamp = System.currentTimeMillis()

        val gamesFetchMaxUnupdated = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_GAMES_FETCH_MAX_UNUPDATED).coerceIn(1, if (quickSync) 8 else Int.MAX_VALUE)
        Timber.i("Refreshing up to $gamesFetchMaxUnupdated games that are missing details in the collection")
        setProgress(PROGRESS_STEP_GAMES_NEW)
        if (prefs[KEY_SYNC_PROGRESS, false] ?: false)
            setForeground(createForegroundInfo(applicationContext.getString(R.string.sync_notification_games_unupdated)))
        val games = gameRepository.loadUnupdatedGames(gamesFetchMaxUnupdated)
        refreshGames(games)?.let { return it }

        checkIfStopped("Canceled while refreshing games")?.let { return it }

        val gamesFetchMax = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_GAMES_FETCH_MAX).coerceIn(1, if (quickSync) 4 else Int.MAX_VALUE)
        Timber.i("Refreshing $gamesFetchMax oldest games in the collection")
        setProgress(PROGRESS_STEP_GAMES_STALE)
        if (prefs[KEY_SYNC_PROGRESS, false] ?: false)
            setForeground(createForegroundInfo(applicationContext.getString(R.string.sync_notification_games_oldest)))
        val staleGames = gameRepository.loadOldestUpdatedGames(gamesFetchMax, startTimestamp)
        refreshGames(staleGames)?.let { return it }

        return null
    }

    /**
     * Refresh all games in the list of game IDs and names, splitting into chunks sized by remote configuration.
     * If unsuccessful, it tries again with half of the chunk size. Returns a data object containing the error message, null if successful.
     */
    private suspend fun refreshGames(games: List<Pair<Int, String>>): Data? {
        var fetchSize = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_GAMES_PER_FETCH).coerceIn(1..MAX_THING_LIMIT)
        var data = refreshGameChunks(games.chunked(fetchSize))
        while (data != null && fetchSize > 1) {
            checkIfStopped("Canceled while refreshing games")?.let { return it }
            Timber.w("Failed fetching chunks of size $fetchSize; trying again with size ${fetchSize / 2}")
            fetchSize /= 2
            data = refreshGameChunks(games.chunked(fetchSize))
        }
        return data
    }

    /**
     * Refresh all game chunks of game IDs and names. Pauses a few seconds between chunks.
     * Returns a data object containing the error message, null if successful.
     */
    private suspend fun refreshGameChunks(gameChunks: List<List<Pair<Int, String>>>): Data? {
        var updatedCount = 0
        try {
            gameChunks.forEachIndexed { index, gameChunk ->
                Timber.i("Refreshing game chunk ${(index + 1)} of ${gameChunks.size} - $gameChunk")
                if (index > 0)
                    delay(RemoteConfig.getLong(RemoteConfig.KEY_SYNC_GAMES_FETCH_PAUSE_MILLIS))
                val result = gameRepository.refreshGame(*gameChunk.map { it.first }.toIntArray())
                if (result.isSuccess) {
                    updatedCount += result.getOrElse { gameChunk.size }
                    Timber.i("Refreshed game chunk ${(index + 1)} of ${gameChunks.size} - $gameChunk")
                } else {
                    result.exceptionOrNull()?.let {
                        return handleException(applicationContext.getString(R.string.sync_notification_games_oldest), it)
                    }
                }
                checkIfStopped("Canceled while refreshing games")?.let { return it }
            }
        } catch (e: Exception) {
            return handleException(applicationContext.getString(R.string.sync_notification_games_unupdated), e)
        }
        Timber.i("Refreshed $updatedCount games")
        return null
    }

    private fun failIfStopped(reason: String): Result? {
        return checkIfStopped(reason)?.let { return Result.failure(it) }
    }

    private fun checkIfStopped(reason: String): Data? {
        return if (isStopped) {
            Timber.i(reason)
            workDataOf(STOPPED_REASON to reason)
        } else {
            null
        }
    }

    /**
     * Log and notify of an exception. Returns a data object containing the error message.
     */
    private fun handleException(contentText: String, throwable: Throwable?): Data {
        if (throwable is CancellationException) {
            Timber.i("Canceling collection sync")
        } else {
            Timber.e(throwable)
            val bigText = if (throwable is HttpException)
                throwable.code().asHttpErrorMessage(applicationContext)
            else
                throwable?.localizedMessage ?: "Unknown exception while syncing collection"
            applicationContext.notifySyncError(contentText, bigText)
        }
        return workDataOf(ERROR_MESSAGE to (throwable?.message ?: "Unknown exception while syncing collection"))
    }

    private fun Long.toDateTime() = this.formatDateTime(
        applicationContext,
        flags = DateUtils.FORMAT_ABBREV_ALL or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME
    )

    private fun SharedPreferences.getCurrentCollectionSyncTimestamp(): Long {
        return this[TIMESTAMP_COLLECTION_COMPLETE_CURRENT, 0L] ?: 0L
    }

    private fun createForegroundInfo(contentText: String): ForegroundInfo {
        return applicationContext.createForegroundInfo(R.string.sync_notification_title_collection, NOTIFICATION_ID_COLLECTION, id, contentText)
    }

    private suspend fun setProgress(
        step: Int,
        subtype: Int = PROGRESS_SUBTYPE_NONE,
        status: CollectionStatus = CollectionStatus.Unknown,
        modifiedSince: Long? = null,
    ) {
        setProgress(
            workDataOf(
                PROGRESS_KEY_STEP to step,
                PROGRESS_KEY_SUBTYPE to subtype,
                PROGRESS_KEY_STATUS to status.mapToPreference(),
                PROGRESS_KEY_MODIFIED_SINCE to modifiedSince,
            )
        )
    }

    private fun Game.Subtype?.getDescription(context: Context): String {
        return context.getString(
            when (this) {
                Game.Subtype.BoardGame -> R.string.games
                Game.Subtype.BoardGameExpansion -> R.string.expansions
                Game.Subtype.BoardGameAccessory -> R.string.accessories
                else -> R.string.items
            }
        )
    }

    companion object {
        const val UNIQUE_WORK_NAME = "com.boardgamegeek.COLLECTION.DOWNLOAD"
        const val UNIQUE_WORK_NAME_AD_HOC = "$UNIQUE_WORK_NAME.adhoc"
        private const val SUCCESS_MESSAGE = "SUCCESS_MESSAGE"
        private const val ERROR_MESSAGE = "ERROR_MESSAGE"
        private const val STOPPED_REASON = "STOPPED_REASON"
        private const val QUICK_SYNC = "QUICK_SYNC"
        private const val REQUESTED_STATUS = "REQUESTED_STATUS"
        private const val MAX_THING_LIMIT = 20

        const val PROGRESS_KEY_STEP = "STEP"
        const val PROGRESS_KEY_SUBTYPE = "SUBTYPE"
        const val PROGRESS_KEY_STATUS = "STATUS"
        const val PROGRESS_KEY_MODIFIED_SINCE = "MODIFIED_SINCE"

        const val PROGRESS_STEP_UNKNOWN = 0
        const val PROGRESS_STEP_COLLECTION_COMPLETE = 1
        const val PROGRESS_STEP_COLLECTION_PARTIAL = 2
        const val PROGRESS_STEP_COLLECTION_DELETE = 3
        const val PROGRESS_STEP_COLLECTION_STALE = 4
        const val PROGRESS_STEP_GAMES_REMOVE = 5
        const val PROGRESS_STEP_GAMES_NEW = 6
        const val PROGRESS_STEP_GAMES_STALE = 7

        const val PROGRESS_SUBTYPE_NONE = 0
        const val PROGRESS_SUBTYPE_ALL = 1
        const val PROGRESS_SUBTYPE_ACCESSORY = 2

        fun requestSync(context: Context, status: CollectionStatus = CollectionStatus.Unknown) {
            val builder = OneTimeWorkRequestBuilder<SyncCollectionWorker>()
                .setConstraints(context.createWorkConstraints(true))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
            if (status != CollectionStatus.Unknown)
                builder.setInputData(workDataOf(REQUESTED_STATUS to status.mapToString()))
            WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_WORK_NAME_AD_HOC, ExistingWorkPolicy.KEEP, builder.build())
        }

        fun buildQuickRequest(context: Context) = OneTimeWorkRequestBuilder<SyncCollectionWorker>()
            .setInputData(workDataOf(QUICK_SYNC to true)) // limited to modified collection and smaller numbers of games
            .setConstraints(context.createWorkConstraints(true))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
            .build()

        private fun CollectionStatus.mapToString() = when (this) {
            CollectionStatus.Own -> "own"
            CollectionStatus.PreviouslyOwned -> "previously_owned"
            CollectionStatus.Preordered -> "preordered"
            CollectionStatus.Played -> "played"
            CollectionStatus.ForTrade -> "for_trade"
            CollectionStatus.WantInTrade -> "want_in_trade"
            CollectionStatus.WantToBuy -> "want_to_buy"
            CollectionStatus.WantToPlay -> "want_to_play"
            CollectionStatus.Wishlist -> "wishlist"
            CollectionStatus.Rated -> "rated"
            CollectionStatus.Commented -> "commented"
            CollectionStatus.HasParts -> "has_parts"
            CollectionStatus.WantParts -> "want_parts"
            CollectionStatus.Unknown -> ""
        }

        private fun String?.mapToEnum() = when (this) {
            "own" -> CollectionStatus.Own
            "previously_owned" -> CollectionStatus.PreviouslyOwned
            "preorderd" -> CollectionStatus.Preordered
            "played" -> CollectionStatus.Played
            "for_trade" -> CollectionStatus.ForTrade
            "want_in_trade" -> CollectionStatus.WantInTrade
            "want_to_buy" -> CollectionStatus.WantToBuy
            "want_to_play" -> CollectionStatus.WantToPlay
            "wishlist" -> CollectionStatus.Wishlist
            "rated" -> CollectionStatus.Rated
            "commented" -> CollectionStatus.Commented
            "has_parts" -> CollectionStatus.HasParts
            "want_parts" -> CollectionStatus.WantParts
            else -> CollectionStatus.Unknown
        }
    }
}
