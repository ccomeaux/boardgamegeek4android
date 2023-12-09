package com.boardgamegeek.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.work.*
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.db.CollectionDaoNew
import com.boardgamegeek.db.GameDaoNew
import com.boardgamegeek.db.model.CollectionPrivateInfoEntity
import com.boardgamegeek.db.model.CollectionStatusEntity
import com.boardgamegeek.model.*
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.PhpApi
import com.boardgamegeek.mappers.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import com.boardgamegeek.work.CollectionUploadWorker
import com.boardgamegeek.work.SyncCollectionWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import timber.log.Timber

class GameCollectionRepository(
    val context: Context,
    private val api: BggService,
    private val imageRepository: ImageRepository,
    private val phpApi: PhpApi,
    private val gameDao: GameDaoNew,
    private val collectionDaoNew: CollectionDaoNew,
) {
    private val collectionDao = CollectionDao(context)
    private val username: String? by lazy { context.preferences()[AccountPreferences.KEY_USERNAME, ""] }
    private val prefs: SharedPreferences by lazy { context.preferences() }

    suspend fun loadCollectionItem(internalId: Long): CollectionItem? {
        return if (internalId == INVALID_ID.toLong())
            null
        else
            collectionDaoNew.load(internalId)?.mapToModel()
    }

    suspend fun loadCollectionItemsForGame(gameId: Int): List<CollectionItem> {
        return if (gameId == INVALID_ID)
            emptyList()
        else
            collectionDaoNew.loadForGame(gameId).map { it.mapToModel() }
    }

    suspend fun refreshCollectionItem(gameId: Int, collectionId: Int, subtype: Game.Subtype?): CollectionItem? =
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
                val response = api.collection(username, options)

                val collectionIds = mutableListOf<Int>()
                var entity: CollectionItem? = null
                response.items?.forEach { collectionItem ->
                    val item = collectionItem.mapToCollectionItem()
                    val game = collectionItem.mapToCollectionItemGame(timestamp)
                    val (id, internalId) = collectionDao.saveItem(item.mapToEntity(timestamp), game)
                    collectionIds += id
                    if (item.collectionId == collectionId) {
                        entity = item.copy(internalId = internalId, syncTimestamp = timestamp)
                    }
                }
                Timber.i("Synced %,d collection item(s) for game '%s'", response.items?.size ?: 0, gameId)
                entity
            } else null
        }

    suspend fun refreshHeroImage(item: CollectionItem): CollectionItem = withContext(Dispatchers.IO) {
        val urlMap = imageRepository.getImageUrls(item.thumbnailUrl.getImageId())
        val urls = urlMap[ImageRepository.ImageType.HERO]
        urls?.firstOrNull()?.let {
            collectionDaoNew.updateHeroImageUrl(item.internalId, it)
            item.copy(heroImageUrl = it)
        } ?: item
    }

    suspend fun refreshCollectionItems(gameId: Int, subtype: Game.Subtype? = null): List<CollectionItem>? = withContext(Dispatchers.IO) {
        if (gameId != INVALID_ID && !username.isNullOrBlank()) {
            val timestamp = System.currentTimeMillis()
            val list = mutableListOf<CollectionItem>()
            val collectionIds = arrayListOf<Int>()

            val options = mutableMapOf(
                BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE to "1",
                BggService.COLLECTION_QUERY_KEY_STATS to "1",
                BggService.COLLECTION_QUERY_KEY_ID to gameId.toString(),
            )
            options.addSubtype(subtype)
            val response = api.collection(username, options)
            response.items?.forEach { collectionItem ->
                val item = collectionItem.mapToCollectionItem()
                val game = collectionItem.mapToCollectionItemGame(timestamp)
                val (collectionId, internalId) = collectionDao.saveItem(item.mapToEntity(timestamp), game)
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
                val playedResponse = api.collection(username, playedOptions)
                playedResponse.items?.forEach { collectionItem ->
                    val item = collectionItem.mapToCollectionItem()
                    val game = collectionItem.mapToCollectionItemGame(timestamp)
                    val (collectionId, internalId) = collectionDao.saveItem(item.mapToEntity(timestamp), game)
                    list += item.copy(internalId = internalId, syncTimestamp = timestamp)
                    collectionIds += collectionId
                }
            }

            Timber.i("Synced %,d collection item(s) for game '%s'", list.size, gameId)

            val deleteCount = collectionDao.delete(gameId, collectionIds)
            Timber.i("Removed %,d collection item(s) for game '%s'", deleteCount, gameId)

            list
        } else null
    }

    private fun MutableMap<String, String>.addSubtype(subtype: Game.Subtype?) {
        subtype?.let {
            this += BggService.COLLECTION_QUERY_KEY_SUBTYPE to when (it) {
                Game.Subtype.BOARDGAME -> BggService.ThingSubtype.BOARDGAME
                Game.Subtype.BOARDGAME_EXPANSION -> BggService.ThingSubtype.BOARDGAME_EXPANSION
                Game.Subtype.BOARDGAME_ACCESSORY -> BggService.ThingSubtype.BOARDGAME_ACCESSORY
            }.code
        }
    }

    suspend fun loadAcquiredFrom() = collectionDaoNew.loadAcquiredFrom().filterNot { it.isBlank() }

    suspend fun loadInventoryLocation() = collectionDaoNew.loadInventoryLocation().filterNot { it.isBlank() }

    suspend fun loadItemsPendingDeletion() = collectionDaoNew.loadItemsPendingDeletion().map { it.mapToModel() }

    suspend fun loadItemsPendingInsert() = collectionDaoNew.loadItemsPendingInsert().map { it.mapToModel() }

    suspend fun loadItemsPendingUpdate() = collectionDaoNew.loadItemsPendingUpdate().map { it.mapToModel() }

    suspend fun uploadDeletedItem(item: CollectionItem): Result<CollectionItemUploadResult> {
        val response = phpApi.collection(item.mapToFormBodyForDeletion())
        return if (response.hasAuthError()) {
            Authenticator.clearPassword(context)
            Result.failure(Exception(context.getString(R.string.msg_play_update_auth_error)))
        } else if (!response.error.isNullOrBlank()) {
            Result.failure(Exception(response.error))
        } else {
            collectionDao.delete(item.internalId)
            Result.success(CollectionItemUploadResult.delete(item))
        }
    }

    suspend fun uploadNewItem(item: CollectionItem): Result<CollectionItemUploadResult> {
        val response = phpApi.collection(item.mapToFormBodyForInsert())
        return if (response.hasAuthError()) {
            Authenticator.clearPassword(context)
            Result.failure(Exception(context.getString(R.string.msg_play_update_auth_error)))
        } else if (!response.error.isNullOrBlank()) {
            Result.failure(Exception(response.error))
        } else {
            val count = collectionDaoNew.clearItemDirtyTimestamp(item.internalId)
            if (count == 1)
                Result.success(CollectionItemUploadResult.insert(item))
            else
                Result.failure(Exception("Error inserting into database"))
        }
    }

    suspend fun uploadUpdatedItem(item: CollectionItem): Result<CollectionItemUploadResult> {
        val statusResult =
            updateItemField(
                item.statusDirtyTimestamp,
                item.mapToFormBodyForStatusUpdate(),
                item,
            ) { collectionDaoNew.clearStatusDirtyTimestamp(it.internalId) }
        if (statusResult.isFailure) return statusResult

        val ratingResult =
            updateItemField(
                item.ratingDirtyTimestamp,
                item.mapToFormBodyForRatingUpdate(),
                item,
            ) { collectionDaoNew.clearRatingDirtyTimestamp(it.internalId) }
        if (ratingResult.isFailure) return ratingResult

        val commentResult =
            updateItemField(
                item.commentDirtyTimestamp,
                item.mapToFormBodyForCommentUpdate(),
                item,
            ) { collectionDaoNew.clearCommentDirtyTimestamp(it.internalId) }
        if (commentResult.isFailure) return commentResult

        val privateInfoResult = updateItemField(
            item.privateInfoDirtyTimestamp,
            item.mapToFormBodyForPrivateInfoUpdate(),
            item,
        ) { collectionDaoNew.clearPrivateInfoDirtyTimestamp(it.internalId) }
        if (privateInfoResult.isFailure) return privateInfoResult

        val wishlistCommentResult = updateItemField(
            item.wishListCommentDirtyTimestamp,
            item.mapToFormBodyForWishlistCommentUpdate(),
            item,
        ) { collectionDaoNew.clearWishlistCommentDirtyTimestamp(it.internalId) }
        if (wishlistCommentResult.isFailure) return wishlistCommentResult

        val tradeConditionResult = updateItemField(
            item.tradeConditionDirtyTimestamp,
            item.mapToFormBodyForTradeConditionUpdate(),
            item,
        ) { collectionDaoNew.clearTradeConditionDirtyTimestamp(it.internalId) }
        if (tradeConditionResult.isFailure) return tradeConditionResult

        val wantPartsResult =
            updateItemField(
                item.wantPartsDirtyTimestamp,
                item.mapToFormBodyForWantPartsUpdate(),
                item,
            ) { collectionDaoNew.clearWantPartsDirtyTimestamp(it.internalId) }
        if (wantPartsResult.isFailure) return wantPartsResult

        val hasPartsResult =
            updateItemField(
                item.hasPartsDirtyTimestamp,
                item.mapToFormBodyForHasPartsUpdate(),
                item,
            ) { collectionDaoNew.clearHasPartsDirtyTimestamp(it.internalId) }
        if (hasPartsResult.isFailure) return hasPartsResult

        return Result.success(CollectionItemUploadResult.update(item))
    }

    private suspend fun updateItemField(
        timestamp: Long,
        formBody: FormBody,
        item: CollectionItem,
        clearTimestamp: suspend (CollectionItem) -> Int
    ): Result<CollectionItemUploadResult> {
        return if (timestamp > 0L) {
            val response = phpApi.collection(formBody)
            if (response.hasAuthError()) {
                Authenticator.clearPassword(context)
                Result.failure(Exception(context.getString(R.string.msg_play_update_auth_error)))
            } else if (!response.error.isNullOrBlank()) {
                Result.failure(Exception(response.error))
            } else {
                val count = clearTimestamp(item)
                if (count != 1)
                    Result.failure(Exception("Error inserting into database"))
                else
                    Result.success(CollectionItemUploadResult.update(item))
            }
        } else Result.success(CollectionItemUploadResult.update(item))
    }

    suspend fun addCollectionItem(
        gameId: Int,
        statuses: List<String>,
        wishListPriority: Int,
        timestamp: Long = System.currentTimeMillis()
    ) {
        gameDao.loadGame(gameId)?.let { entity ->
            entity.game?.let {
                val internalId = collectionDao.addNewCollectionItem(gameId, it, statuses, wishListPriority, timestamp)
                if (internalId == INVALID_ID.toLong()) {
                    Timber.d("Collection item for game %s (%s) not added", it.gameName, gameId)
                } else {
                    Timber.d("Collection item added for game %s (%s) (internal ID = %s)", it.gameName, gameId, internalId)
                    enqueueUploadRequest(gameId)
                }
            }
        }
    }

    suspend fun updatePrivateInfo(
        internalId: Long,
        priceCurrency: String?,
        price: Double?,
        currentValueCurrency: String?,
        currentValue: Double?,
        quantity: Int?,
        acquisitionDate: Long?,
        acquiredFrom: String?,
        inventoryLocation: String?,
    ): Int {
        val entity = CollectionPrivateInfoEntity(
            internalId,
            priceCurrency,
            price,
            currentValueCurrency,
            currentValue,
            quantity,
            acquisitionDate.asDateForApi(),
            acquiredFrom,
            inventoryLocation,
            System.currentTimeMillis(),
        )
        return collectionDaoNew.updatePrivateInfo(entity)
    }

    suspend fun updateStatuses(internalId: Long, statuses: List<String>, wishlistPriority: Int): Int {
        val priority = if (statuses.contains(BggContract.Collection.Columns.STATUS_WISHLIST))
            wishlistPriority.coerceIn(1..5)
        else null
        val entity = CollectionStatusEntity( // TODO don't use column names!
            internalId = internalId,
            statusOwn = statuses.contains(BggContract.Collection.Columns.STATUS_OWN),
            statusPreviouslyOwned = statuses.contains(BggContract.Collection.Columns.STATUS_PREVIOUSLY_OWNED),
            statusForTrade = statuses.contains(BggContract.Collection.Columns.STATUS_FOR_TRADE),
            statusWant = statuses.contains(BggContract.Collection.Columns.STATUS_WANT),
            statusWantToPlay = statuses.contains(BggContract.Collection.Columns.STATUS_WANT_TO_PLAY),
            statusWantToBuy = statuses.contains(BggContract.Collection.Columns.STATUS_WANT_TO_BUY),
            statusWishlist = statuses.contains(BggContract.Collection.Columns.STATUS_WISHLIST),
            statusWishlistPriority = priority,
            statusPreordered = statuses.contains(BggContract.Collection.Columns.STATUS_PREORDERED),
            statusDirtyTimestamp = System.currentTimeMillis(),
        )
        return collectionDaoNew.updateStatuses(entity)
    }

    suspend fun updateRating(internalId: Long, rating: Double): Int = collectionDaoNew.updateRating(internalId, rating, System.currentTimeMillis())

    suspend fun updateComment(internalId: Long, text: String): Int = collectionDaoNew.updateComment(internalId, text, System.currentTimeMillis())

    suspend fun updatePrivateComment(internalId: Long, text: String): Int = collectionDaoNew.updatePrivateComment(internalId, text, System.currentTimeMillis())

    suspend fun updateWishlistComment(internalId: Long, text: String): Int = collectionDaoNew.updateWishlistComment(internalId, text, System.currentTimeMillis())

    suspend fun updateCondition(internalId: Long, text: String): Int = collectionDaoNew.updateTradeCondition(internalId, text, System.currentTimeMillis())

    suspend fun updateHasParts(internalId: Long, text: String): Int = collectionDaoNew.updateHasParts(internalId, text, System.currentTimeMillis())

    suspend fun updateWantParts(internalId: Long, text: String): Int = collectionDaoNew.updateWantParts(internalId, text, System.currentTimeMillis())

    suspend fun markAsDeleted(internalId: Long): Int = collectionDaoNew.updateDeletedTimestamp(internalId, System.currentTimeMillis())

    suspend fun resetTimestamps(internalId: Long): Int = collectionDaoNew.clearDirtyTimestamps(internalId)

    fun enqueueUploadRequest(gameId: Int) {
        WorkManager.getInstance(context).enqueue(CollectionUploadWorker.buildRequest(context, gameId))
    }

    fun enqueueRefreshRequest(workName: String) {
        WorkManager.getInstance(context)
            .beginUniqueWork(workName, ExistingWorkPolicy.KEEP, CollectionUploadWorker.buildRequest(context))
            .then(SyncCollectionWorker.buildQuickRequest(context))
            .enqueue()
    }
}
