package com.boardgamegeek.repository

import android.content.ContentValues
import androidx.core.content.contentValuesOf
import com.boardgamegeek.BggApplication
import com.boardgamegeek.auth.AccountUtils
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.extensions.asDateForApi
import com.boardgamegeek.extensions.load
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.BggService
import com.boardgamegeek.mappers.mapToEntities
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.service.SyncService
import com.boardgamegeek.util.ImageUtils.getImageId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class GameCollectionRepository(val application: BggApplication) {
    private val dao = CollectionDao(application)

    private val username: String? by lazy {
        AccountUtils.getUsername(application)
    }

    suspend fun loadCollectionItem(collectionId: Int) = dao.load(collectionId)

    suspend fun refreshCollectionItem(gameId: Int, collectionId: Int): CollectionItemEntity? =
        withContext(Dispatchers.IO) {
            if (gameId != INVALID_ID && !username.isNullOrBlank()) {
                val timestamp = System.currentTimeMillis()
                val response = Adapter.createForXmlWithAuth(application).collectionC(
                    username, mapOf(
                        BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE to "1",
                        BggService.COLLECTION_QUERY_KEY_STATS to "1",
                        BggService.COLLECTION_QUERY_KEY_ID to gameId.toString(),
                        //BggService.COLLECTION_QUERY_KEY_SUBTYPE to subType TODO determine if needed
                    )
                )

                val collectionIds = mutableListOf<Int>()
                var entity: CollectionItemEntity? = null
                response.items?.forEach { collectionItem ->
                    val (item, game) = collectionItem.mapToEntities()
                    val (id, internalId) = dao.saveItem(item, game, timestamp)
                    collectionIds.add(id)
                    if (item.collectionId == collectionId) {
                        entity = item.copy(internalId = internalId, syncTimestamp = timestamp)
                    }
                }
                Timber.i(
                    "Synced %,d collection item(s) for game '%s'", response.items?.size ?: 0, gameId
                )

                val deleteCount = dao.delete(gameId, collectionIds)
                Timber.i("Removed %,d collection item(s) for game '%s'", deleteCount, gameId)

                entity
            } else null
        }

    suspend fun refreshHeroImage(item: CollectionItemEntity): CollectionItemEntity = withContext(Dispatchers.IO) {
        val response = Adapter.createGeekdoApi().image2(item.thumbnailUrl.getImageId())
        val url = response.images.medium.url
        dao.update(item.internalId, contentValuesOf(Collection.COLLECTION_HERO_IMAGE_URL to url))
        item.copy(heroImageUrl = url)
    }

    suspend fun loadCollectionItems(gameId: Int) = dao.loadByGame(gameId)

    suspend fun refreshCollectionItems(
        gameId: Int,
        subType: String = BggService.THING_SUBTYPE_BOARDGAME
    ): List<CollectionItemEntity>? = withContext(Dispatchers.IO) {
        if (gameId != INVALID_ID && !username.isNullOrBlank()) {
            val timestamp = System.currentTimeMillis()
            val list = mutableListOf<CollectionItemEntity>()
            // TODO This doesn't sync only-played games (the played flag needs to be set explicitly)
            val response = Adapter.createForXmlWithAuth(application).collectionC(
                username, mapOf(
                    BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE to "1",
                    BggService.COLLECTION_QUERY_KEY_STATS to "1",
                    BggService.COLLECTION_QUERY_KEY_ID to gameId.toString(),
                    BggService.COLLECTION_QUERY_KEY_SUBTYPE to subType
                )
            )
            val collectionIds = arrayListOf<Int>()
            response.items?.forEach { collectionItem ->
                val (item, game) = collectionItem.mapToEntities()
                val (collectionId, internalId) = dao.saveItem(item, game, timestamp)
                list += item.copy(internalId = internalId, syncTimestamp = timestamp)
                collectionIds += collectionId
            }
            Timber.i("Synced %,d collection item(s) for game '%s'", response.items?.size ?: 0, gameId)

            val deleteCount = dao.delete(gameId, collectionIds)
            Timber.i("Removed %,d collection item(s) for game '%s'", deleteCount, gameId)

            list
        } else null
    }

    fun addCollectionItem(
        gameId: Int,
        statuses: List<String>,
        wishListPriority: Int?,
        timestamp: Long = System.currentTimeMillis()
    ) {
        if (gameId != INVALID_ID) {
            val values = contentValuesOf(
                Collection.GAME_ID to gameId,
                Collection.STATUS_DIRTY_TIMESTAMP to timestamp
            )
            putValue(statuses, values, Collection.STATUS_OWN)
            putValue(statuses, values, Collection.STATUS_PREORDERED)
            putValue(statuses, values, Collection.STATUS_FOR_TRADE)
            putValue(statuses, values, Collection.STATUS_WANT)
            putValue(statuses, values, Collection.STATUS_WANT_TO_PLAY)
            putValue(statuses, values, Collection.STATUS_WANT_TO_BUY)
            putValue(statuses, values, Collection.STATUS_WISHLIST)
            putValue(statuses, values, Collection.STATUS_PREVIOUSLY_OWNED)
            putWishList(statuses, wishListPriority, values)

            // TODO move into DAO
            application.contentResolver.load(
                Games.buildGameUri(gameId),
                arrayOf(
                    Games.GAME_NAME,
                    Games.GAME_SORT_NAME,
                    Games.YEAR_PUBLISHED,
                    Games.IMAGE_URL,
                    Games.THUMBNAIL_URL,
                    Games.HERO_IMAGE_URL
                )
            )?.use {
                if (it.moveToFirst()) {
                    values.put(Collection.COLLECTION_NAME, it.getString(0))
                    values.put(Collection.COLLECTION_SORT_NAME, it.getString(1))
                    values.put(Collection.COLLECTION_YEAR_PUBLISHED, it.getInt(2))
                    values.put(Collection.COLLECTION_IMAGE_URL, it.getString(3))
                    values.put(Collection.COLLECTION_THUMBNAIL_URL, it.getString(4))
                    values.put(Collection.COLLECTION_HERO_IMAGE_URL, it.getString(5))
                    values.put(Collection.COLLECTION_DIRTY_TIMESTAMP, System.currentTimeMillis())
                }
            }

            val gameName = values.getAsString(Collection.COLLECTION_NAME)
            val response = application.contentResolver.insert(Collection.CONTENT_URI, values)
            val internalId = response?.lastPathSegment?.toLongOrNull() ?: INVALID_ID.toLong()
            if (internalId == INVALID_ID.toLong()) {
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
        if (statuses.contains(Collection.STATUS_WISHLIST)) {
            values.put(Collection.STATUS_WISHLIST, 1)
            values.put(
                Collection.STATUS_WISHLIST_PRIORITY, wishListPriority ?: 3 // like to have
            )
        } else {
            values.put(Collection.STATUS_WISHLIST, 0)
        }
    }

    suspend fun updatePrivateInfo(
        internalId: Long, priceCurrency: String?,
        price: Double?,
        currentValueCurrency: String?,
        currentValue: Double?,
        quantity: Int?,
        acquisitionDate: Long?,
        acquiredFrom: String?,
        inventoryLocation: String?,
    ): Int = withContext(Dispatchers.IO) {
        if (internalId != INVALID_ID.toLong()) {
            val values = contentValuesOf(
                Collection.PRIVATE_INFO_DIRTY_TIMESTAMP to System.currentTimeMillis(),
                Collection.PRIVATE_INFO_PRICE_PAID_CURRENCY to priceCurrency,
                Collection.PRIVATE_INFO_PRICE_PAID to price,
                Collection.PRIVATE_INFO_CURRENT_VALUE_CURRENCY to currentValueCurrency,
                Collection.PRIVATE_INFO_CURRENT_VALUE to currentValue,
                Collection.PRIVATE_INFO_QUANTITY to quantity,
                Collection.PRIVATE_INFO_ACQUISITION_DATE to acquisitionDate.asDateForApi(),
                Collection.PRIVATE_INFO_ACQUIRED_FROM to acquiredFrom,
                Collection.PRIVATE_INFO_INVENTORY_LOCATION to inventoryLocation
            )
            dao.update(internalId, values)
        } else 0
    }

    suspend fun updateStatuses(internalId: Long, statuses: List<String>, wishlistPriority: Int): Int =
        withContext(Dispatchers.IO) {
            if (internalId != INVALID_ID.toLong()) {
                val values = contentValuesOf(
                    Collection.STATUS_DIRTY_TIMESTAMP to System.currentTimeMillis(),
                    Collection.STATUS_OWN to statuses.contains(Collection.STATUS_OWN),
                    Collection.STATUS_PREVIOUSLY_OWNED to statuses.contains(Collection.STATUS_PREVIOUSLY_OWNED),
                    Collection.STATUS_PREORDERED to statuses.contains(Collection.STATUS_PREORDERED),
                    Collection.STATUS_FOR_TRADE to statuses.contains(Collection.STATUS_FOR_TRADE),
                    Collection.STATUS_WANT to statuses.contains(Collection.STATUS_WANT),
                    Collection.STATUS_WANT_TO_BUY to statuses.contains(Collection.STATUS_WANT_TO_BUY),
                    Collection.STATUS_WANT_TO_PLAY to statuses.contains(Collection.STATUS_WANT_TO_PLAY),
                    Collection.STATUS_WISHLIST to statuses.contains(Collection.STATUS_WISHLIST)
                )
                if (statuses.contains(Collection.STATUS_WISHLIST)) {
                    values.put(Collection.STATUS_WISHLIST_PRIORITY, wishlistPriority.coerceIn(1..5))
                }
                dao.update(internalId, values)
            } else 0
        }

    suspend fun updateRating(internalId: Long, rating: Double): Int = withContext(Dispatchers.IO) {
        if (internalId != INVALID_ID.toLong()) {
            val values = contentValuesOf(
                Collection.RATING to rating,
                Collection.RATING_DIRTY_TIMESTAMP to System.currentTimeMillis()
            )
            dao.update(internalId, values)
        } else 0
    }

    suspend fun updateText(internalId: Long, text: String, textColumn: String, timestampColumn: String): Int =
        withContext(Dispatchers.IO) {
            if (internalId != INVALID_ID.toLong()) {
                val values = contentValuesOf(
                    textColumn to text,
                    timestampColumn to System.currentTimeMillis()
                )
                dao.update(internalId, values)
            } else 0
        }

    suspend fun markAsDeleted(internalId: Long): Int = withContext(Dispatchers.IO) {
        if (internalId != INVALID_ID.toLong()) {
            val values = contentValuesOf(Collection.COLLECTION_DELETE_TIMESTAMP to System.currentTimeMillis())
            dao.update(internalId, values)
        } else 0
    }

    suspend fun resetTimestamps(internalId: Long): Int =
        withContext(Dispatchers.IO) {
            if (internalId != INVALID_ID.toLong()) {
                val values = contentValuesOf(
                    Collection.COLLECTION_DIRTY_TIMESTAMP to 0,
                    Collection.STATUS_DIRTY_TIMESTAMP to 0,
                    Collection.COMMENT_DIRTY_TIMESTAMP to 0,
                    Collection.RATING_DIRTY_TIMESTAMP to 0,
                    Collection.PRIVATE_INFO_DIRTY_TIMESTAMP to 0,
                    Collection.WISHLIST_COMMENT_DIRTY_TIMESTAMP to 0,
                    Collection.TRADE_CONDITION_DIRTY_TIMESTAMP to 0,
                    Collection.WANT_PARTS_DIRTY_TIMESTAMP to 0,
                    Collection.HAS_PARTS_DIRTY_TIMESTAMP to 0,
                )
                dao.update(internalId, values)
            } else 0
        }
}
