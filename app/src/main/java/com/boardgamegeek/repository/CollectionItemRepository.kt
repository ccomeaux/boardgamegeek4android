package com.boardgamegeek.repository

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.auth.AccountUtils
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.BggService.THING_SUBTYPE_BOARDGAME_ACCESSORY
import com.boardgamegeek.io.model.CollectionResponse
import com.boardgamegeek.livedata.RefreshableResourceLoader
import com.boardgamegeek.mappers.CollectionItemMapper
import com.boardgamegeek.pref.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.util.RateLimiter
import retrofit2.Call
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

class CollectionItemRepository(val application: BggApplication) {
    private val dao = CollectionDao(application)
    private val mapper = CollectionItemMapper()
    private val username: String? by lazy { AccountUtils.getUsername(application) }
    private val prefs: SharedPreferences by lazy { application.preferences() }
    private val syncPrefs: SharedPreferences by lazy { SyncPrefs.getPrefs(application) }
    private val statusesToSync = syncPrefs.getSyncStatusesOrDefault()

    fun load(): LiveData<List<CollectionItemEntity>> {
        return dao.loadAsLiveData()
    }

    fun loadCollection(): LiveData<RefreshableResource<List<CollectionItemEntity>>> {
        return object : RefreshableResourceLoader<List<CollectionItemEntity>, CollectionResponse>(application) {
            private val timestamp = System.currentTimeMillis()
            private val rateLimiter = RateLimiter<Int>(10, TimeUnit.MINUTES)
            private val subtypes = listOf("", THING_SUBTYPE_BOARDGAME_ACCESSORY)
            private var subtype = ""

            override fun loadFromDatabase(): LiveData<List<CollectionItemEntity>> {
                return dao.loadAsLiveData()
            }

            override fun shouldRefresh(data: List<CollectionItemEntity>?): Boolean {
                val lastStatusSync = syncPrefs.getPartialCollectionSyncLastCompletedAt(subtype)
                val lastPartialSync = syncPrefs.getPartialCollectionSyncLastCompletedAt()
                val isSyncUnderway = syncPrefs.getCurrentCollectionSyncTimestamp() > 0
                return prefs.isCollectionSetToSync() &&
                        !username.isNullOrBlank() &&
                        rateLimiter.shouldProcess(0) &&
                        (lastStatusSync <= lastPartialSync) &&
                        !isSyncUnderway
            }

            override val typeDescriptionResId = R.string.title_collection

            override fun createCall(page: Int): Call<CollectionResponse> {
                subtype = subtypes.getOrNull(page - 1) ?: ""
                val lastStatusSync = syncPrefs.getPartialCollectionSyncLastCompletedAt(subtype)
                val modifiedSince = BggService.COLLECTION_QUERY_DATE_TIME_FORMAT.format(Date(lastStatusSync))
                val options = mutableMapOf(
                        BggService.COLLECTION_QUERY_KEY_STATS to "1",
                        BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE to "1",
                        BggService.COLLECTION_QUERY_KEY_MODIFIED_SINCE to modifiedSince,
                )
                if (subtype.isNotEmpty()) options[BggService.COLLECTION_QUERY_KEY_SUBTYPE] = subtype
                return Adapter.createForXml().collection(username, options)
            }

            override fun saveCallResult(result: CollectionResponse) {
                var count = 0
                result.items?.forEach {
                    val (item, game) = mapper.map(it)
                    if (isItemStatusSetToSync(item)) {
                        val collectionId = dao.saveItem(item, game, timestamp)
                        if (collectionId != BggContract.INVALID_ID) count++
                    } else {
                        Timber.i("Skipped collection item '${item.gameName}' [ID=${item.gameId}, collection ID=${item.collectionId}] - collection status not synced")
                    }
                }
                syncPrefs.setPartialCollectionSyncLastCompletedAt(subtype, timestamp)
                Timber.i("...saved %,d %s collection items", count, subtype)
            }

            override fun hasMorePages(result: CollectionResponse, currentPage: Int): Boolean {
                return currentPage < subtypes.size
            }

            override fun onRefreshSucceeded() {
                syncPrefs.setPartialCollectionSyncLastCompletedAt()
            }

            override fun onRefreshFailed() {
                rateLimiter.reset(0)
            }

            override fun onRefreshCancelled() {
                rateLimiter.reset(0)
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
        }.asLiveData()
    }
}