package com.boardgamegeek.service

import android.content.SyncResult
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.extensions.formatList
import com.boardgamegeek.io.BggService
import com.boardgamegeek.repository.CollectionItemRepository
import com.boardgamegeek.util.RemoteConfig
import kotlinx.coroutines.runBlocking
import retrofit2.HttpException
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

/**
 * Syncs collection items that have not yet been updated completely with stats and private info (in batches).
 */
class SyncCollectionUnupdated(
    application: BggApplication,
    syncResult: SyncResult,
    private val collectionItemRepository: CollectionItemRepository,
) : SyncTask(application, syncResult) {
    private var detail: String = ""

    override val syncType = SyncService.FLAG_SYNC_COLLECTION_DOWNLOAD

    override val notificationSummaryMessageId = R.string.sync_notification_collection_unupdated

    private val fetchPauseMillis = RemoteConfig.getLong(RemoteConfig.KEY_SYNC_COLLECTION_FETCH_PAUSE_MILLIS)

    private val gamesPerFetch = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_COLLECTION_GAMES_PER_FETCH)

    private val maxFetchCount = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_COLLECTION_FETCH_MAX)

    override fun execute() {
        Timber.i("Starting to sync unupdated collection items")
        try {
            var numberOfFetches = 0

            val options = mutableMapOf(
                BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE to "1",
                BggService.COLLECTION_QUERY_KEY_STATS to "1",
            )
            var previousGameList = mapOf<Int, String>()

            do {
                if (isCancelled) break

                if (numberOfFetches > 0) if (wasSleepInterrupted(fetchPauseMillis.milliseconds)) return

                numberOfFetches++
                val gameList = runBlocking { collectionItemRepository.loadUnupdatedItems(gamesPerFetch) }
                if (gameList.isEmpty()) {
                    Timber.i("No unupdated collection items; aborting")
                    break
                } else if (gameList == previousGameList) {
                    Timber.i("Didn't update any collection items in last attempt; aborting")
                    break
                }
                previousGameList = gameList.toMap()

                if (gameList.isNotEmpty()) {
                    val gameDescription = gameList.values.toList().formatList()
                    detail = context.getString(R.string.sync_notification_collection_update_games, gameList.size, gameDescription)
                    if (numberOfFetches > 1) {
                        detail = context.getString(R.string.sync_notification_page_suffix, detail, numberOfFetches)
                    }
                    updateProgressNotification(detail)
                    Timber.i("Found %,d collection items to update [%s]", gameList.size, gameDescription)

                    options[BggService.COLLECTION_QUERY_KEY_ID] = gameList.keys.joinToString(",")
                    options.remove(BggService.COLLECTION_QUERY_KEY_SUBTYPE)
                    val itemCount = requestAndPersist(options)

                    if (itemCount < 0) {
                        Timber.i("Unsuccessful attempt to update collection items; aborting")
                        cancel()
                        break
                    }

                    options[BggService.COLLECTION_QUERY_KEY_SUBTYPE] = BggService.ThingSubtype.BOARDGAME_ACCESSORY.code
                    val accessoryCount = requestAndPersist(options)

                    if (accessoryCount < 0) {
                        Timber.i("Unsuccessful attempt to update collection accessories; aborting")
                        cancel()
                        break
                    }

                    if (itemCount + accessoryCount == 0) {
                        Timber.i("Didn't find any collection items or accessories to update; aborting")
                        break
                    }
                } else {
                    Timber.i("No more unupdated collection items to sync")
                    break
                }
            } while (numberOfFetches < maxFetchCount)
            Timber.i("Syncing unupdated collection completed successfully")
        } catch (e: Exception) {
            Timber.i("Syncing unupdated collection ended with exception:\n$e")
        }
    }

    private fun requestAndPersist(options: Map<String, String>): Int {
        return try {
            Timber.i("Requesting collection items with options %s", options)
            val count = runBlocking { collectionItemRepository.refresh(options) }
            syncResult.stats.numUpdates += count.toLong()
            Timber.i("Saved %,d formerly unupdated collection items", count)
            count
        } catch (e: Exception) {
            if (e is HttpException) {
                showError(detail, e.code())
            } else {
                showError(detail, e)
            }
            syncResult.stats.numIoExceptions++
            -1
        }
    }
}
