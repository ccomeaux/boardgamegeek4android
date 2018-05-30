package com.boardgamegeek.repository

import android.arch.lifecycle.LiveData
import android.support.v4.util.ArrayMap
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.auth.AccountUtils
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.model.CollectionResponse
import com.boardgamegeek.livedata.RefreshableResourceLoader
import com.boardgamegeek.mappers.CollectionItemMapper
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.model.GameCollectionItem
import com.boardgamegeek.ui.model.RefreshableResource
import com.boardgamegeek.util.DateTimeUtils
import retrofit2.Call
import timber.log.Timber
import java.util.concurrent.TimeUnit

class GameCollectionRepository(val application: BggApplication) {
    private var timestamp = 0L

    private val username: String? by lazy {
        AccountUtils.getUsername(application)
    }

    /**
     * Get a game from the database and potentially refresh it from BGG.
     */
    fun getCollectionItems(gameId: Int): LiveData<RefreshableResource<List<GameCollectionItem>>> {
        return GameCollectionLoader(application, gameId).asLiveData()
    }

    inner class GameCollectionLoader(application: BggApplication, private val gameId: Int) : RefreshableResourceLoader<List<GameCollectionItem>, CollectionResponse>(application) {
        override val typeDescriptionResId = R.string.title_collection

        override fun loadFromDatabase() = CollectionDao(application).load(gameId)

        override fun shouldRefresh(data: List<GameCollectionItem>?): Boolean {
            if (gameId == BggContract.INVALID_ID || username == null) return false
            val syncTimestamp = data?.minBy { it.syncTimestamp }?.syncTimestamp ?: 0L
            return data == null || DateTimeUtils.isOlderThan(syncTimestamp, 10, TimeUnit.MINUTES)
        }

        override fun createCall(): Call<CollectionResponse> {
            timestamp = System.currentTimeMillis()
            val options = ArrayMap<String, String>()
            options[BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE] = "1"
            options[BggService.COLLECTION_QUERY_KEY_STATS] = "1"
            options[BggService.COLLECTION_QUERY_KEY_ID] = gameId.toString()
            return Adapter.createForXmlWithAuth(application).collection(username, options)
        }

        override fun saveCallResult(result: CollectionResponse) {
            val dao = CollectionDao(application)
            val mapper = CollectionItemMapper()
            val collectionIds = arrayListOf<Int>()

            result.items?.forEach { item ->
                val collectionId = dao.saveItem(mapper.map(item), timestamp)
                collectionIds.add(collectionId)
            }
            Timber.i("Synced %,d collection item(s) for game '%s'", result.items?.size ?: 0, gameId)

            val deleteCount = dao.delete(gameId, collectionIds)
            Timber.i("Removed %,d collection item(s) for game '%s'", deleteCount, gameId)
        }
    }
}
