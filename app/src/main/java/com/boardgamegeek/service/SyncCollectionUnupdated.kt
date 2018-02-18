package com.boardgamegeek.service

import android.accounts.Account
import android.content.Context
import android.content.SyncResult
import android.support.v4.util.ArrayMap
import com.boardgamegeek.R
import com.boardgamegeek.io.BggService
import com.boardgamegeek.model.persister.CollectionPersister
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.service.model.GameList
import com.boardgamegeek.util.SelectionBuilder
import timber.log.Timber
import java.io.IOException

/**
 * Syncs collection items that have not yet been updated completely with stats and private info (in batches of 25).
 */
class SyncCollectionUnupdated(context: Context, service: BggService, syncResult: SyncResult, private val account: Account) : SyncTask(context, service, syncResult) {
    private var detail: String = ""

    override val syncType: Int
        get() = SyncService.FLAG_SYNC_COLLECTION_DOWNLOAD

    override val notificationSummaryMessageId: Int
        get() = R.string.sync_notification_collection_unupdated

    override fun execute() {
        Timber.i("Syncing unupdated collection list...")
        try {
            var numberOfFetches = 0
            val persister = CollectionPersister.Builder(context)
                    .includePrivateInfo()
                    .includeStats()
                    .build()
            val options = ArrayMap<String, String>()
            options[BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE] = "1"
            options[BggService.COLLECTION_QUERY_KEY_STATS] = "1"
            var previousGameList = GameList(0)

            do {
                if (isCancelled) break

                if (numberOfFetches > 0) if (wasSleepInterrupted(5000)) return

                numberOfFetches++
                val gameIds = queryGames()
                if (areGamesListsEqual(gameIds, previousGameList)) {
                    Timber.i("...didn't update any games; breaking out of fetch loop")
                    break
                }
                previousGameList = gameIds

                if (gameIds.size > 0) {
                    detail = context.getString(R.string.sync_notification_collection_update_games, gameIds.size, gameIds.description)
                    if (numberOfFetches > 1) {
                        detail = context.getString(R.string.sync_notification_page_suffix, detail, numberOfFetches)
                    }
                    updateProgressNotification(detail)
                    Timber.i("...found %,d games to update [%s]", gameIds.size, gameIds.description)

                    options[BggService.COLLECTION_QUERY_KEY_ID] = gameIds.ids
                    options.remove(BggService.COLLECTION_QUERY_KEY_SUBTYPE)
                    val itemCount = requestAndPersist(account.name, persister, options)

                    if (itemCount < 0) {
                        Timber.i("...unsuccessful sync; breaking out of fetch loop")
                        cancel()
                        break
                    }

                    options[BggService.COLLECTION_QUERY_KEY_SUBTYPE] = BggService.THING_SUBTYPE_BOARDGAME_ACCESSORY
                    val accessoryCount = requestAndPersist(account.name, persister, options)

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
            } while (numberOfFetches < 100)
        } finally {
            Timber.i("...complete!")
        }
    }

    private fun queryGames(): GameList {
        val list = GameList(GAME_PER_FETCH)
        val cursor = context.contentResolver.query(Collection.CONTENT_URI,
                arrayOf(Collection.GAME_ID, Collection.GAME_NAME),
                SelectionBuilder.whereZeroOrNull("collection.${Collection.UPDATED}"),
                null,
                "collection.${Collection.UPDATED_LIST} DESC LIMIT $GAME_PER_FETCH")
        cursor?.use { c ->
            while (c.moveToNext()) {
                list.addGame(c.getInt(0), c.getString(1))
            }
        }
        return list
    }


    private fun requestAndPersist(username: String, persister: CollectionPersister, options: ArrayMap<String, String>): Int {
        Timber.i("..requesting collection items with options %s", options)

        val call = service.collection(username, options)
        try {
            val response = call.execute()
            if (response.isSuccessful) {
                val body = response.body()
                return if (body != null && body.itemCount > 0) {
                    val count = persister.save(body.items).recordCount
                    syncResult.stats.numUpdates += body.itemCount.toLong()
                    Timber.i("...saved %,d records for %,d collection items", count, body.itemCount)
                    body.itemCount
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

    private fun areGamesListsEqual(gameList1: GameList, gameList2: GameList): Boolean {
        for (i in gameList1.idList) {
            if (!gameList2.idList.contains(i)) return false
        }
        for (i in gameList2.idList) {
            if (!gameList1.idList.contains(i)) return false
        }
        return true
    }

    companion object {
        private const val GAME_PER_FETCH = 25
    }
}
