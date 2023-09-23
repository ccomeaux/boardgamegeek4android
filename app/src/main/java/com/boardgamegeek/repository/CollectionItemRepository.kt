package com.boardgamegeek.repository

import android.content.Context
import android.content.SharedPreferences
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.BggService
import com.boardgamegeek.mappers.mapToCollectionItemEntity
import com.boardgamegeek.mappers.mapToCollectionItemGameEntity
import com.boardgamegeek.mappers.mapToEntity
import com.boardgamegeek.pref.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.work.SyncCollectionWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class CollectionItemRepository(
    val context: Context,
    private val api: BggService,
) {
    private val dao = CollectionDao(context)
    private val prefs: SharedPreferences by lazy { context.preferences() }
    private val syncPrefs: SharedPreferences by lazy { SyncPrefs.getPrefs(context) }

    suspend fun loadAll(): List<CollectionItemEntity> = dao.loadAll()
        .filter { (it.second.collectionDeleteTimestamp ?: 0L) == 0L }
        .map {
            it.second.mapToEntity(it.first.mapToEntity())
        }

    suspend fun resetCollectionItems() = withContext(Dispatchers.IO) {
        syncPrefs.clearCollection()
        SyncCollectionWorker.requestSync(context)
    }

    suspend fun refresh(options: Map<String, String>, updatedTimestamp: Long = System.currentTimeMillis()): Int = withContext(Dispatchers.IO) {
        var count = 0
        val username = prefs[AccountPreferences.KEY_USERNAME, ""]
        if (!username.isNullOrBlank()) {
            val response = api.collection(username, options)
            response.items?.forEach {
                val item = it.mapToCollectionItemEntity()
                if (isItemStatusSetToSync(item)) {
                    val game = it.mapToCollectionItemGameEntity(updatedTimestamp)
                    val (collectionId, _) = dao.saveItem(item.mapToEntity(updatedTimestamp), game)
                    if (collectionId != BggContract.INVALID_ID) count++
                } else {
                    Timber.i("Skipped collection item '${item.gameName}' [ID=${item.gameId}, collection ID=${item.collectionId}] - collection status not synced")
                }
            }
        }
        count
    }

    suspend fun loadUnupdatedItems() = dao.loadUnupdatedItems()

    suspend fun deleteUnupdatedItems(timestamp: Long) = dao.deleteUnupdatedItems(timestamp)

    private fun isItemStatusSetToSync(item: CollectionItemEntity): Boolean {
        val statusesToSync = prefs.getSyncStatusesOrDefault()
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
