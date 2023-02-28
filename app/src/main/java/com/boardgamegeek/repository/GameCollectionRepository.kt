package com.boardgamegeek.repository

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.contentValuesOf
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.db.GameDao
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.entities.GameEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.BggService
import com.boardgamegeek.mappers.mapToEntities
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import com.boardgamegeek.service.SyncService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class GameCollectionRepository(val context: Context) {
    private val dao = CollectionDao(context)
    private val gameDao = GameDao(context)
    private val username: String? by lazy { context.preferences()[AccountPreferences.KEY_USERNAME, ""] }
    private val prefs: SharedPreferences by lazy { context.preferences() }

    suspend fun loadCollectionItem(internalId: Long) = dao.load(internalId)

    suspend fun refreshCollectionItem(gameId: Int, collectionId: Int, subtype: GameEntity.Subtype?): CollectionItemEntity? =
        withContext(Dispatchers.IO) {
            if ((gameId != INVALID_ID || collectionId != INVALID_ID) && !username.isNullOrBlank()) {
                val timestamp = System.currentTimeMillis()
                val options = mutableMapOf(
                    BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE to "1",
                    BggService.COLLECTION_QUERY_KEY_STATS to "1",
                )
                options += if (collectionId != INVALID_ID)
                    BggService.COLLECTION_QUERY_KEY_COLLECTION_ID to collectionId.toString()
                else
                    BggService.COLLECTION_QUERY_KEY_ID to gameId.toString()
                options.addSubtype(subtype)
                val response = Adapter.createForXmlWithAuth(context).collection(username, options)

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
                Timber.i("Synced %,d collection item(s) for game '%s'", response.items?.size ?: 0, gameId)
                entity
            } else null
        }

    suspend fun refreshHeroImage(item: CollectionItemEntity): CollectionItemEntity = withContext(Dispatchers.IO) {
        val response = Adapter.createGeekdoApi().image(item.thumbnailUrl.getImageId())
        val url = response.images.medium.url
        dao.update(item.internalId, contentValuesOf(Collection.Columns.COLLECTION_HERO_IMAGE_URL to url))
        item.copy(heroImageUrl = url)
    }

    suspend fun loadCollectionItems(gameId: Int) = dao.loadByGame(gameId)

    suspend fun refreshCollectionItems(gameId: Int, subtype: GameEntity.Subtype? = null): List<CollectionItemEntity>? = withContext(Dispatchers.IO) {
        if (gameId != INVALID_ID && !username.isNullOrBlank()) {
            val timestamp = System.currentTimeMillis()
            val list = mutableListOf<CollectionItemEntity>()
            val collectionIds = arrayListOf<Int>()

            val options = mutableMapOf(
                BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE to "1",
                BggService.COLLECTION_QUERY_KEY_STATS to "1",
                BggService.COLLECTION_QUERY_KEY_ID to gameId.toString(),
            )
            options.addSubtype(subtype)
            val response = Adapter.createForXmlWithAuth(context).collection(username, options)
            response.items?.forEach { collectionItem ->
                val (item, game) = collectionItem.mapToEntities()
                val (collectionId, internalId) = dao.saveItem(item, game, timestamp)
                list += item.copy(internalId = internalId, syncTimestamp = timestamp)
                collectionIds += collectionId
            }

            val statuses = prefs.getSyncStatusesOrDefault()
            if ((response.items == null || response.items.isNotEmpty()) && statuses.contains(BggService.COLLECTION_QUERY_STATUS_PLAYED)) {
                val playedOptions = mutableMapOf(
                    BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE to "1",
                    BggService.COLLECTION_QUERY_KEY_STATS to "1",
                    BggService.COLLECTION_QUERY_KEY_ID to gameId.toString(),
                    BggService.COLLECTION_QUERY_STATUS_PLAYED to "1",
                )
                playedOptions.addSubtype(subtype)
                val playedResponse = Adapter.createForXmlWithAuth(context).collection(username, playedOptions)
                playedResponse.items?.forEach { collectionItem ->
                    val (item, game) = collectionItem.mapToEntities()
                    val (collectionId, internalId) = dao.saveItem(item, game, timestamp)
                    list += item.copy(internalId = internalId, syncTimestamp = timestamp)
                    collectionIds += collectionId
                }
            }

            Timber.i("Synced %,d collection item(s) for game '%s'", list.size, gameId)

            val deleteCount = dao.delete(gameId, collectionIds)
            Timber.i("Removed %,d collection item(s) for game '%s'", deleteCount, gameId)

            list
        } else null
    }

    private fun MutableMap<String, String>.addSubtype(subtype: GameEntity.Subtype?) {
        subtype?.let {
            this += BggService.COLLECTION_QUERY_KEY_SUBTYPE to when (it) {
                GameEntity.Subtype.BOARDGAME -> BggService.ThingSubtype.BOARDGAME
                GameEntity.Subtype.BOARDGAME_EXPANSION -> BggService.ThingSubtype.BOARDGAME_EXPANSION
                GameEntity.Subtype.BOARDGAME_ACCESSORY -> BggService.ThingSubtype.BOARDGAME_ACCESSORY
            }.code
        }
    }

    suspend fun loadAcquiredFrom() = dao.loadAcquiredFrom()

    suspend fun loadInventoryLocation() = dao.loadInventoryLocation()

    suspend fun addCollectionItem(
        gameId: Int,
        statuses: List<String>,
        wishListPriority: Int?,
        timestamp: Long = System.currentTimeMillis()
    ) {
        if (gameId != INVALID_ID) {
            val values = contentValuesOf(
                Collection.Columns.GAME_ID to gameId,
                Collection.Columns.STATUS_DIRTY_TIMESTAMP to timestamp
            )
            putValue(statuses, values, Collection.Columns.STATUS_OWN)
            putValue(statuses, values, Collection.Columns.STATUS_PREORDERED)
            putValue(statuses, values, Collection.Columns.STATUS_FOR_TRADE)
            putValue(statuses, values, Collection.Columns.STATUS_WANT)
            putValue(statuses, values, Collection.Columns.STATUS_WANT_TO_PLAY)
            putValue(statuses, values, Collection.Columns.STATUS_WANT_TO_BUY)
            putValue(statuses, values, Collection.Columns.STATUS_WISHLIST)
            putValue(statuses, values, Collection.Columns.STATUS_PREVIOUSLY_OWNED)
            putWishList(statuses, wishListPriority, values)

            val gameName = gameDao.load(gameId)?.let { game ->
                values.put(Collection.Columns.COLLECTION_NAME, game.name)
                values.put(Collection.Columns.COLLECTION_SORT_NAME, game.sortName)
                values.put(Collection.Columns.COLLECTION_YEAR_PUBLISHED, game.yearPublished)
                values.put(Collection.Columns.COLLECTION_IMAGE_URL, game.imageUrl)
                values.put(Collection.Columns.COLLECTION_THUMBNAIL_URL, game.thumbnailUrl)
                values.put(Collection.Columns.COLLECTION_HERO_IMAGE_URL, game.heroImageUrl)
                values.put(Collection.Columns.COLLECTION_DIRTY_TIMESTAMP, System.currentTimeMillis())
                game.name
            }

            val internalId = dao.upsertItem(values)
            if (internalId == INVALID_ID.toLong()) {
                Timber.d("Collection item for game %s (%s) not added", gameName, gameId)
            } else {
                Timber.d("Collection item added for game %s (%s) (internal ID = %s)", gameName, gameId, internalId)
                SyncService.sync(context, SyncService.FLAG_SYNC_COLLECTION_UPLOAD)
            }
        }
    }

    private fun putValue(statuses: List<String>, values: ContentValues, statusColumn: String) {
        values.put(statusColumn, if (statuses.contains(statusColumn)) 1 else 0)
    }

    private fun putWishList(statuses: List<String>, wishListPriority: Int?, values: ContentValues) {
        if (statuses.contains(Collection.Columns.STATUS_WISHLIST)) {
            values.put(Collection.Columns.STATUS_WISHLIST, 1)
            values.put(
                Collection.Columns.STATUS_WISHLIST_PRIORITY, wishListPriority ?: 3 // like to have
            )
        } else {
            values.put(Collection.Columns.STATUS_WISHLIST, 0)
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
                Collection.Columns.PRIVATE_INFO_DIRTY_TIMESTAMP to System.currentTimeMillis(),
                Collection.Columns.PRIVATE_INFO_PRICE_PAID_CURRENCY to priceCurrency,
                Collection.Columns.PRIVATE_INFO_PRICE_PAID to price,
                Collection.Columns.PRIVATE_INFO_CURRENT_VALUE_CURRENCY to currentValueCurrency,
                Collection.Columns.PRIVATE_INFO_CURRENT_VALUE to currentValue,
                Collection.Columns.PRIVATE_INFO_QUANTITY to quantity,
                Collection.Columns.PRIVATE_INFO_ACQUISITION_DATE to acquisitionDate.asDateForApi(),
                Collection.Columns.PRIVATE_INFO_ACQUIRED_FROM to acquiredFrom,
                Collection.Columns.PRIVATE_INFO_INVENTORY_LOCATION to inventoryLocation
            )
            dao.update(internalId, values)
        } else 0
    }

    suspend fun updateStatuses(internalId: Long, statuses: List<String>, wishlistPriority: Int): Int =
        withContext(Dispatchers.IO) {
            if (internalId != INVALID_ID.toLong()) {
                val values = contentValuesOf(
                    Collection.Columns.STATUS_DIRTY_TIMESTAMP to System.currentTimeMillis(),
                    Collection.Columns.STATUS_OWN to statuses.contains(Collection.Columns.STATUS_OWN),
                    Collection.Columns.STATUS_PREVIOUSLY_OWNED to statuses.contains(Collection.Columns.STATUS_PREVIOUSLY_OWNED),
                    Collection.Columns.STATUS_PREORDERED to statuses.contains(Collection.Columns.STATUS_PREORDERED),
                    Collection.Columns.STATUS_FOR_TRADE to statuses.contains(Collection.Columns.STATUS_FOR_TRADE),
                    Collection.Columns.STATUS_WANT to statuses.contains(Collection.Columns.STATUS_WANT),
                    Collection.Columns.STATUS_WANT_TO_BUY to statuses.contains(Collection.Columns.STATUS_WANT_TO_BUY),
                    Collection.Columns.STATUS_WANT_TO_PLAY to statuses.contains(Collection.Columns.STATUS_WANT_TO_PLAY),
                    Collection.Columns.STATUS_WISHLIST to statuses.contains(Collection.Columns.STATUS_WISHLIST),
                )
                if (statuses.contains(Collection.Columns.STATUS_WISHLIST)) {
                    values.put(Collection.Columns.STATUS_WISHLIST_PRIORITY, wishlistPriority.coerceIn(1..5))
                }
                dao.update(internalId, values)
            } else 0
        }

    suspend fun updateRating(internalId: Long, rating: Double): Int = withContext(Dispatchers.IO) {
        if (internalId != INVALID_ID.toLong()) {
            val values = contentValuesOf(
                Collection.Columns.RATING to rating,
                Collection.Columns.RATING_DIRTY_TIMESTAMP to System.currentTimeMillis()
            )
            dao.update(internalId, values)
        } else 0
    }

    suspend fun updateComment(internalId: Long, text: String): Int =
        updateText(internalId, text, Collection.Columns.COMMENT, Collection.Columns.COMMENT_DIRTY_TIMESTAMP)

    suspend fun updatePrivateComment(internalId: Long, text: String): Int =
        updateText(internalId, text, Collection.Columns.PRIVATE_INFO_COMMENT, Collection.Columns.PRIVATE_INFO_DIRTY_TIMESTAMP)

    suspend fun updateWishlistComment(internalId: Long, text: String): Int =
        updateText(internalId, text, Collection.Columns.WISHLIST_COMMENT, Collection.Columns.WISHLIST_COMMENT_DIRTY_TIMESTAMP)

    suspend fun updateCondition(internalId: Long, text: String): Int =
        updateText(internalId, text, Collection.Columns.CONDITION, Collection.Columns.TRADE_CONDITION_DIRTY_TIMESTAMP)

    suspend fun updateHasParts(internalId: Long, text: String): Int =
        updateText(internalId, text, Collection.Columns.HASPARTS_LIST, Collection.Columns.HAS_PARTS_DIRTY_TIMESTAMP)

    suspend fun updateWantParts(internalId: Long, text: String): Int =
        updateText(internalId, text, Collection.Columns.WANTPARTS_LIST, Collection.Columns.WANT_PARTS_DIRTY_TIMESTAMP)

    private suspend fun updateText(internalId: Long, text: String, textColumn: String, timestampColumn: String): Int =
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
            val values = contentValuesOf(Collection.Columns.COLLECTION_DELETE_TIMESTAMP to System.currentTimeMillis())
            dao.update(internalId, values)
        } else 0
    }

    suspend fun resetTimestamps(internalId: Long): Int =
        withContext(Dispatchers.IO) {
            if (internalId != INVALID_ID.toLong()) {
                val values = contentValuesOf(
                    Collection.Columns.COLLECTION_DIRTY_TIMESTAMP to 0,
                    Collection.Columns.STATUS_DIRTY_TIMESTAMP to 0,
                    Collection.Columns.COMMENT_DIRTY_TIMESTAMP to 0,
                    Collection.Columns.RATING_DIRTY_TIMESTAMP to 0,
                    Collection.Columns.PRIVATE_INFO_DIRTY_TIMESTAMP to 0,
                    Collection.Columns.WISHLIST_COMMENT_DIRTY_TIMESTAMP to 0,
                    Collection.Columns.TRADE_CONDITION_DIRTY_TIMESTAMP to 0,
                    Collection.Columns.WANT_PARTS_DIRTY_TIMESTAMP to 0,
                    Collection.Columns.HAS_PARTS_DIRTY_TIMESTAMP to 0,
                )
                dao.update(internalId, values)
            } else 0
        }
}
