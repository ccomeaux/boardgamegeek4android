package com.boardgamegeek.repository

import android.content.ContentValues
import androidx.core.content.contentValuesOf
import androidx.lifecycle.MutableLiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.auth.AccountUtils
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.extensions.executeAsyncTask
import com.boardgamegeek.extensions.load
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.BggService
import com.boardgamegeek.mappers.CollectionItemMapper
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.service.SyncService
import com.boardgamegeek.tasks.sync.SyncCollectionByGameTask
import com.boardgamegeek.util.ImageUtils.getImageId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class GameCollectionRepository(val application: BggApplication) {
    private val dao = CollectionDao(application)
    private val mapper = CollectionItemMapper()

    private val username: String? by lazy {
        AccountUtils.getUsername(application)
    }

    suspend fun loadCollectionItem(collectionId: Int) = dao.load(collectionId)

    suspend fun refreshCollectionItem(gameId: Int, collectionId: Int): CollectionItemEntity? =
        withContext(Dispatchers.IO) {
            val collectionIds = mutableListOf<Int>()

            val options = mutableMapOf(
                BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE to "1",
                BggService.COLLECTION_QUERY_KEY_STATS to "1",
                BggService.COLLECTION_QUERY_KEY_ID to gameId.toString(),
                //BggService.COLLECTION_QUERY_KEY_SUBTYPE to subType TODO determine if needed
            )
            val timestamp = System.currentTimeMillis()
            val response = Adapter.createForXmlWithAuth(application).collectionC(username, options)

            var entity: CollectionItemEntity? = null
            response.items?.forEach { item ->
                val pair = mapper.map(item)
                val id = dao.saveItem(pair.first, pair.second, timestamp)
                collectionIds.add(id.first)
                if (pair.first.collectionId == collectionId) {
                    entity = pair.first.copy(internalId = id.second, syncTimestamp = timestamp)
                }
            }
            Timber.i(
                "Synced %,d collection item(s) for game '%s'", response.items?.size ?: 0, gameId
            )

            val deleteCount = dao.delete(gameId, collectionIds)
            Timber.i("Removed %,d collection item(s) for game '%s'", deleteCount, gameId)

            entity
        }

    suspend fun refreshHeroImage(item: CollectionItemEntity): CollectionItemEntity = withContext(Dispatchers.IO) {
        val response = Adapter.createGeekdoApi().image2(item.thumbnailUrl.getImageId())
        val url = response.images.medium.url
        dao.update(item.internalId, contentValuesOf(BggContract.Collection.COLLECTION_HERO_IMAGE_URL to url))
        item.copy(heroImageUrl = url)
    }

    suspend fun loadCollectionItems(gameId: Int) = dao.loadByGame(gameId)

    suspend fun refreshCollectionItems(
        gameId: Int,
        subType: String = BggService.THING_SUBTYPE_BOARDGAME
    ): List<CollectionItemEntity> = withContext(Dispatchers.IO) {
        val list = mutableListOf<CollectionItemEntity>()
        val mapper = CollectionItemMapper()
        val collectionIds = arrayListOf<Int>()
        val timestamp = System.currentTimeMillis()
        val options = mutableMapOf(
            BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE to "1",
            BggService.COLLECTION_QUERY_KEY_STATS to "1",
            BggService.COLLECTION_QUERY_KEY_ID to gameId.toString(),
            BggService.COLLECTION_QUERY_KEY_SUBTYPE to subType
        )
        // TODO This doesn't sync only-played games (the played flag needs to be set explicitly)
        val response = Adapter.createForXmlWithAuth(application).collectionC(username, options)
        response.items?.forEach { item ->
            val pair = mapper.map(item)
            val collectionId = dao.saveItem(pair.first, pair.second, timestamp)
            list += pair.first.copy(internalId = collectionId.second, syncTimestamp = timestamp)
            collectionIds += collectionId.first
        }
        Timber.i("Synced %,d collection item(s) for game '%s'", response.items?.size ?: 0, gameId)

        val deleteCount = dao.delete(gameId, collectionIds)
        Timber.i("Removed %,d collection item(s) for game '%s'", deleteCount, gameId)

        list
    }

    fun addCollectionItem(gameId: Int, statuses: List<String>, wishListPriority: Int?) {
        if (gameId == BggContract.INVALID_ID) return
        application.appExecutors.diskIO.execute {
            val values = contentValuesOf()
            values.put(BggContract.Collection.GAME_ID, gameId)
            putValue(statuses, values, BggContract.Collection.STATUS_OWN)
            putValue(statuses, values, BggContract.Collection.STATUS_PREORDERED)
            putValue(statuses, values, BggContract.Collection.STATUS_FOR_TRADE)
            putValue(statuses, values, BggContract.Collection.STATUS_WANT)
            putValue(statuses, values, BggContract.Collection.STATUS_WANT_TO_PLAY)
            putValue(statuses, values, BggContract.Collection.STATUS_WANT_TO_BUY)
            putValue(statuses, values, BggContract.Collection.STATUS_WISHLIST)
            putValue(statuses, values, BggContract.Collection.STATUS_PREVIOUSLY_OWNED)
            putWishList(statuses, wishListPriority, values)
            values.put(BggContract.Collection.STATUS_DIRTY_TIMESTAMP, System.currentTimeMillis())

            application.contentResolver.load(
                BggContract.Games.buildGameUri(gameId),
                arrayOf(
                    BggContract.Games.GAME_NAME,
                    BggContract.Games.GAME_SORT_NAME,
                    BggContract.Games.YEAR_PUBLISHED,
                    BggContract.Games.IMAGE_URL,
                    BggContract.Games.THUMBNAIL_URL,
                    BggContract.Games.HERO_IMAGE_URL
                )
            )?.use {
                if (it.moveToFirst()) {
                    values.put(BggContract.Collection.COLLECTION_NAME, it.getString(0))
                    values.put(BggContract.Collection.COLLECTION_SORT_NAME, it.getString(1))
                    values.put(BggContract.Collection.COLLECTION_YEAR_PUBLISHED, it.getInt(2))
                    values.put(BggContract.Collection.COLLECTION_IMAGE_URL, it.getString(3))
                    values.put(BggContract.Collection.COLLECTION_THUMBNAIL_URL, it.getString(4))
                    values.put(BggContract.Collection.COLLECTION_HERO_IMAGE_URL, it.getString(5))
                    values.put(BggContract.Collection.COLLECTION_DIRTY_TIMESTAMP, System.currentTimeMillis())
                }
            }

            val gameName = values.getAsString(BggContract.Collection.COLLECTION_NAME)
            val response = application.contentResolver.insert(BggContract.Collection.CONTENT_URI, values)
            val internalId = response?.lastPathSegment?.toLongOrNull()
                ?: BggContract.INVALID_ID.toLong()
            if (internalId == BggContract.INVALID_ID.toLong()) {
                Timber.d("Collection item for game %s (%s) not added", gameName, gameId)
            } else {
                Timber.d("Collection item added for game %s (%s) (internal ID = %s)", gameName, gameId, internalId)
                SyncService.sync(application, SyncService.FLAG_SYNC_COLLECTION_UPLOAD)
            }
        }
    }

    private fun putValue(statuses: List<String>, values: ContentValues, statusColumn: String) {
        values.put(statusColumn, if (statuses.contains(statusColumn)) 1 else 0)
    }

    private fun putWishList(statuses: List<String>, wishListPriority: Int?, values: ContentValues) {
        if (statuses.contains(BggContract.Collection.STATUS_WISHLIST)) {
            values.put(BggContract.Collection.STATUS_WISHLIST, 1)
            values.put(
                BggContract.Collection.STATUS_WISHLIST_PRIORITY, wishListPriority ?: 3 // like to have
            )
            return
        } else {
            values.put(BggContract.Collection.STATUS_WISHLIST, 0)
        }
    }

    fun update(internalId: Long, values: ContentValues) {
        if (internalId == BggContract.INVALID_ID.toLong()) return
        application.appExecutors.diskIO.execute {
            dao.update(internalId, values)
        }
    }

    fun resetTimestamps(internalId: Long, gameId: Int, errorMessage: MutableLiveData<String>) {
        if (internalId == BggContract.INVALID_ID.toLong()) return
        application.appExecutors.diskIO.execute {
            val values = contentValuesOf(
                BggContract.Collection.COLLECTION_DIRTY_TIMESTAMP to 0,
                BggContract.Collection.STATUS_DIRTY_TIMESTAMP to 0,
                BggContract.Collection.COMMENT_DIRTY_TIMESTAMP to 0,
                BggContract.Collection.RATING_DIRTY_TIMESTAMP to 0,
                BggContract.Collection.PRIVATE_INFO_DIRTY_TIMESTAMP to 0,
                BggContract.Collection.WISHLIST_COMMENT_DIRTY_TIMESTAMP to 0,
                BggContract.Collection.TRADE_CONDITION_DIRTY_TIMESTAMP to 0,
                BggContract.Collection.WANT_PARTS_DIRTY_TIMESTAMP to 0,
                BggContract.Collection.HAS_PARTS_DIRTY_TIMESTAMP to 0
            )

            val success = dao.update(internalId, values) > 0
            if (success && gameId != BggContract.INVALID_ID) {
                SyncCollectionByGameTask(application, gameId, errorMessage).executeAsyncTask()
            }
        }
    }
}
