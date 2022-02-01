package com.boardgamegeek.service

import android.accounts.Account
import android.content.SyncResult
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.extensions.formatList
import com.boardgamegeek.extensions.use
import com.boardgamegeek.extensions.whereZeroOrNull
import com.boardgamegeek.io.BggService
import com.boardgamegeek.mappers.CollectionItemMapper
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.util.RemoteConfig
import timber.log.Timber
import java.io.IOException

/**
 * Syncs collection items that have not yet been updated completely with stats and private info (in batches).
 */
class SyncCollectionUnupdated(application: BggApplication, service: BggService, syncResult: SyncResult, private val account: Account) : SyncTask(application, service, syncResult) {
    private var detail: String = ""

    override val syncType = SyncService.FLAG_SYNC_COLLECTION_DOWNLOAD

    override val notificationSummaryMessageId = R.string.sync_notification_collection_unupdated

    private val fetchPauseMillis = RemoteConfig.getLong(RemoteConfig.KEY_SYNC_COLLECTION_FETCH_PAUSE_MILLIS)

    private val gamesPerFetch = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_COLLECTION_GAMES_PER_FETCH)

    private val maxFetchCount = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_COLLECTION_FETCH_MAX)

    override fun execute() {
        Timber.i("Syncing unupdated collection list...")
        try {
            var numberOfFetches = 0
            val dao = CollectionDao(application)
            val options = mutableMapOf(
                    BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE to "1",
                    BggService.COLLECTION_QUERY_KEY_STATS to "1",
            )
            var previousGameList = mapOf<Int, String>()

            do {
                if (isCancelled) break

                if (numberOfFetches > 0) if (wasSleepInterrupted(fetchPauseMillis)) return

                numberOfFetches++
                val gameList = queryGames()
                if (gameList == previousGameList) {
                    Timber.i("...didn't update any games; breaking out of fetch loop")
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
                    Timber.i("...found %,d games to update [%s]", gameList.size, gameDescription)

                    options[BggService.COLLECTION_QUERY_KEY_ID] = gameList.keys.joinToString(",")
                    options.remove(BggService.COLLECTION_QUERY_KEY_SUBTYPE)
                    val itemCount = requestAndPersist(account.name, dao, options)

                    if (itemCount < 0) {
                        Timber.i("...unsuccessful sync; breaking out of fetch loop")
                        cancel()
                        break
                    }

                    options[BggService.COLLECTION_QUERY_KEY_SUBTYPE] = BggService.THING_SUBTYPE_BOARDGAME_ACCESSORY
                    val accessoryCount = requestAndPersist(account.name, dao, options)

                    if (accessoryCount < 0) {
                        Timber.i("...unsuccessful sync; breaking out of fetch loop")
                        cancel()
                        break
                    }

                    if (itemCount + accessoryCount == 0) {
                        Timber.i("...unsuccessful sync; breaking out of fetch loop")
                        break
                    }
                } else {
                    Timber.i("...no more unupdated collection items")
                    break
                }
            } while (numberOfFetches < maxFetchCount)
        } finally {
            Timber.i("...complete!")
        }
    }

    private fun queryGames(): Map<Int, String> {
        val games = mutableMapOf<Int, String>()
        val cursor = context.contentResolver.query(Collection.CONTENT_URI,
                arrayOf(Collection.GAME_ID, Collection.GAME_NAME),
                "collection.${Collection.UPDATED}".whereZeroOrNull(),
                null,
                "collection.${Collection.UPDATED_LIST} DESC LIMIT $gamesPerFetch")
        cursor?.use {
            while (it.moveToNext()) {
                games[it.getInt(0)] = it.getString(1)
            }
        }
        return games
    }


    private fun requestAndPersist(username: String, dao: CollectionDao, options: Map<String, String>): Int {
        Timber.i("..requesting collection items with options %s", options)

        val call = service.collection(username, options)
        try {
            val timestamp = System.currentTimeMillis()
            val response = call.execute()
            if (response.isSuccessful) {
                val items = response.body()?.items
                return if (items != null && items.size > 0) {
                    val mapper = CollectionItemMapper()
                    for (item in items) {
                        val pair = mapper.map(item)
                        dao.saveItem(pair.first, pair.second, timestamp)
                    }
                    syncResult.stats.numUpdates += items.size.toLong()
                    Timber.i("...saved %,d collection items", items.size)
                    items.size
                } else {
                    Timber.i("...no collection items found for these games")
                    0
                }
            } else {
                showError(detail, response.code())
                syncResult.stats.numIoExceptions++
                return -1
            }
        } catch (e: IOException) {
            showError(detail, e)
            syncResult.stats.numIoExceptions++
            return -1
        }
    }
}
