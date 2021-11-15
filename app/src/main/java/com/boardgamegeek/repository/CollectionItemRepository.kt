package com.boardgamegeek.repository

import android.content.SharedPreferences
import com.boardgamegeek.BggApplication
import com.boardgamegeek.auth.AccountUtils
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.BggService
import com.boardgamegeek.mappers.mapToEntities
import com.boardgamegeek.pref.*
import com.boardgamegeek.provider.BggContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.*

class CollectionItemRepository(val application: BggApplication) {
    private val dao = CollectionDao(application)
    private val prefs: SharedPreferences by lazy { application.preferences() }
    private val username: String? by lazy { prefs[AccountUtils.KEY_USERNAME, ""] }
    private val syncPrefs: SharedPreferences by lazy { SyncPrefs.getPrefs(application) }
    private val statusesToSync = syncPrefs.getSyncStatusesOrDefault()

    suspend fun load(): List<CollectionItemEntity> = withContext(Dispatchers.IO) {
        dao.load()
    }

    suspend fun refresh(): List<CollectionItemEntity> = withContext(Dispatchers.IO) { // TODO change dispatcher?
        if (!prefs.isCollectionSetToSync()) {
            Timber.i("Collection not set to sync any statuses")
            return@withContext dao.load()
        }

        if (username.isNullOrBlank()) {
            Timber.i("User name not set")
            return@withContext dao.load()
        }

        if (syncPrefs.getCurrentCollectionSyncTimestamp() > 0) {
            Timber.i("Collection sync is already under way")
            return@withContext dao.load()
        }

        listOf("", BggService.THING_SUBTYPE_BOARDGAME_ACCESSORY).forEach { subtype ->
            refreshSubtype(subtype)
        }
        dao.load()
    }

    private suspend fun refreshSubtype(subtype: String, timestamp: Long = System.currentTimeMillis()) {
        val lastPartialSync = syncPrefs.getPartialCollectionSyncLastCompletedAt()
        val lastStatusSync = syncPrefs.getPartialCollectionSyncLastCompletedAt(subtype)
        if (lastStatusSync <= lastPartialSync) {
            val modifiedSince = BggService.COLLECTION_QUERY_DATE_TIME_FORMAT.format(Date(lastStatusSync))
            val options = mutableMapOf(
                BggService.COLLECTION_QUERY_KEY_STATS to "1",
                BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE to "1",
                BggService.COLLECTION_QUERY_KEY_MODIFIED_SINCE to modifiedSince,
            )
            if (subtype.isNotEmpty()) options[BggService.COLLECTION_QUERY_KEY_SUBTYPE] = subtype
            val response = Adapter.createForXml().collectionC(username, options)

            var count = 0
            response.items?.forEach {
                val (item, game) = it.mapToEntities()
                if (isItemStatusSetToSync(item)) {
                    val (collectionId, _) = dao.saveItem(item, game, timestamp)
                    if (collectionId != BggContract.INVALID_ID) count++
                } else {
                    Timber.i("Skipped collection item '${item.gameName}' [ID=${item.gameId}, collection ID=${item.collectionId}] - collection status not synced")
                }
            }
            syncPrefs.setPartialCollectionSyncLastCompletedAt(subtype, timestamp)
            Timber.i("...saved %,d %s collection items", count, subtype)
        } else {
            Timber.i("Collection subtype $subtype recently synced")
        }
    }

    private fun isItemStatusSetToSync(item: CollectionItemEntity): Boolean {
        if (item.own && COLLECTION_STATUS_OWN in statusesToSync) return true
        if (item.previouslyOwned && COLLECTION_STATUS_PREVIOUSLY_OWNED in statusesToSync) return true
        if (item.forTrade && COLLECTION_STATUS_FOR_TRADE in statusesToSync) return true
        if (item.wantInTrade && COLLECTION_STATUS_WANT_IN_TRADE in statusesToSync) return true
        if (item.wantToPlay && COLLECTION_STATUS_WANT_TO_PLAY in statusesToSync) return true
        if (item.wantToBuy && COLLECTION_STATUS_WANT_TO_BUY in statusesToSync) return true
        if (item.wishList && COLLECTION_STATUS_WISHLIST in statusesToSync) return true
        if (item.preOrdered && COLLECTION_STATUS_PREORDERED in statusesToSync) return true
        if (item.rating > 0.0 && COLLECTION_STATUS_RATED in statusesToSync) return true
        if (item.comment.isNotEmpty() && COLLECTION_STATUS_COMMENTED in statusesToSync) return true
        if (item.hasPartsList.isNotEmpty() && COLLECTION_STATUS_HAS_PARTS in statusesToSync) return true
        if (item.wantPartsList.isNotEmpty() && COLLECTION_STATUS_WANT_PARTS in statusesToSync) return true
        return item.numberOfPlays > 0 && COLLECTION_STATUS_PLAYED in statusesToSync
    }
}
