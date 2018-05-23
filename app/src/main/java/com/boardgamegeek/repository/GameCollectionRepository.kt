package com.boardgamegeek.repository

import android.arch.lifecycle.LiveData
import android.content.Context
import android.support.v4.util.ArrayMap
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.auth.AccountUtils
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.model.CollectionResponse
import com.boardgamegeek.livedata.GameCollectionLiveData
import com.boardgamegeek.livedata.RefreshableResourceLoader
import com.boardgamegeek.mappers.CollectionItemMapper
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.model.GameCollectionItem
import com.boardgamegeek.ui.model.RefreshableResource
import com.boardgamegeek.util.DateTimeUtils
import retrofit2.Call
import timber.log.Timber

private const val AGE_IN_DAYS_TO_REFRESH = 3

class GameCollectionRepository(val application: BggApplication) {
    private val loader: GameCollectionLoader = GameCollectionLoader(application)
    private var gameId: Int = BggContract.INVALID_ID
    private var timestamp = 0L

    private val username: String? by lazy {
        AccountUtils.getUsername(application)
    }

    /**
     * Get a game from the database and potentially refresh it from BGG.
     */
    fun getCollectionItems(gameId: Int): LiveData<RefreshableResource<List<GameCollectionItem>>> {
        this.gameId = gameId
        return loader.load()
    }

    /**
     * Refresh the currently loaded game from BGG.
     */
    fun refresh() {
        loader.refresh()
    }

    inner class GameCollectionLoader(context: Context) : RefreshableResourceLoader<List<GameCollectionItem>, CollectionResponse>(context) {
        override val typeDescriptionResId = R.string.title_collection

        override fun isRequestParamsValid(): Boolean {
            return gameId != BggContract.INVALID_ID && !username.isNullOrBlank()
        }

        override fun loadFromDatabase() = GameCollectionLiveData(context, gameId)

        override fun shouldRefresh(data: List<GameCollectionItem>?): Boolean {
            val syncTimestamp = data?.minBy { it.syncTimestamp }?.syncTimestamp ?: 0L
            return data == null || DateTimeUtils.howManyDaysOld(syncTimestamp) > AGE_IN_DAYS_TO_REFRESH
        }

        override fun createCall(): Call<CollectionResponse> {
            timestamp = System.currentTimeMillis()
            val options = ArrayMap<String, String>()
            options[BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE] = "1"
            options[BggService.COLLECTION_QUERY_KEY_STATS] = "1"
            options[BggService.COLLECTION_QUERY_KEY_ID] = gameId.toString()
            return Adapter.createForXmlWithAuth(context).collection(username, options)
        }

        override fun saveCallResult(result: CollectionResponse) {
            val dao = CollectionDao(application)
            val mapper = CollectionItemMapper()
            val collectionIds = arrayListOf<Int>()

            for (item in result.items) {
                val collectionId = dao.saveItem(mapper.map(item), timestamp)
                collectionIds.add(collectionId)
            }
            Timber.i("Synced %,d collection item(s) for game '%s'", if (result.items == null) 0 else result.items.size, gameId)

            val deleteCount = dao.delete(gameId, collectionIds)
            Timber.i("Removed %,d collection item(s) for game '%s'", deleteCount, gameId)
        }
    }
}
