package com.boardgamegeek.tasks.sync

import androidx.annotation.StringRes
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.auth.AccountUtils
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.model.CollectionResponse
import com.boardgamegeek.mappers.CollectionItemMapper
import com.boardgamegeek.provider.BggContract
import retrofit2.Call
import timber.log.Timber

class SyncCollectionByGameTask(application: BggApplication, private val gameId: Int) : SyncTask<CollectionResponse>(application.applicationContext) {
    private val username = AccountUtils.getUsername(context)
    private val dao = CollectionDao(application)
    private val results = mutableListOf<Int>()
    private val timestamp = System.currentTimeMillis()

    @get:StringRes
    override val typeDescriptionResId: Int
        get() = R.string.title_collection

    override fun createService(): BggService = Adapter.createForXmlWithAuth(context)

    override fun createCall(): Call<CollectionResponse>? {
        val options = mapOf(
                BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE to "1",
                BggService.COLLECTION_QUERY_KEY_STATS to "1",
                BggService.COLLECTION_QUERY_KEY_ID to gameId.toString(),
        )
        return bggService?.collection(username, options)
    }

    override val isRequestParamsValid: Boolean
        get() = gameId != BggContract.INVALID_ID && !username.isNullOrBlank()

    override fun persistResponse(body: CollectionResponse?) {
        results.clear()
        if (body?.items != null) {
            val mapper = CollectionItemMapper()
            for (item in body.items.filterNotNull()) {
                val (first, second) = mapper.map(item)
                val collectionId = dao.saveItem(first, second, timestamp, includeStats = true, includePrivateInfo = true, isBrief = false)
                results.add(collectionId)
            }
            Timber.i("Synced %,d collection item(s) for game '%s'", body.items.size, gameId)
        } else {
            Timber.i("No collection items for game '%s'", gameId)
        }
    }

    override fun finishSync() {
        val deleteCount = dao.delete(gameId, results)
        Timber.i("Removed %,d collection item(s) for game '%s'", deleteCount, gameId)
    }
}