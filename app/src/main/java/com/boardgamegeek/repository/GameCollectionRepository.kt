package com.boardgamegeek.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.work.*
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.db.GameDao
import com.boardgamegeek.db.model.*
import com.boardgamegeek.model.*
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.PhpApi
import com.boardgamegeek.io.model.CollectionItemRemote
import com.boardgamegeek.io.safeApiCall
import com.boardgamegeek.mappers.*
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.pref.clearCollection
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import com.boardgamegeek.util.FileUtils
import com.boardgamegeek.work.CollectionUploadWorker
import com.boardgamegeek.work.SyncCollectionWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import timber.log.Timber

class GameCollectionRepository(
    val context: Context,
    private val api: BggService,
    private val imageRepository: ImageRepository,
    private val phpApi: PhpApi,
    private val gameDao: GameDao,
    private val collectionDao: CollectionDao,
) {
    private val prefs: SharedPreferences by lazy { context.preferences() }

    fun resetCollectionItems() {
        SyncPrefs.getPrefs(context).clearCollection()
        SyncCollectionWorker.requestSync(context)
    }

    fun loadAllAsFlow(): Flow<List<CollectionItem>> = collectionDao.loadAllAsFlow()
        .map { list -> list.map { it.mapToModel() } }
        .flowOn(Dispatchers.Default)
        .map { it.filter { item -> item.deleteTimestamp == 0L } }
        .flowOn(Dispatchers.Default)

    fun loadCollectionItemFlow(internalId: Long): Flow<CollectionItem?> {
        return collectionDao.loadFlow(internalId)
            .map { it?.mapToModel() }
            .flowOn(Dispatchers.Default)
    }

    fun loadCollectionItemsForGameFlow(gameId: Int): Flow<List<CollectionItem>> {
        return collectionDao.loadForGameFlow(gameId)
            .map {
                list -> list.map { it.mapToModel() }.filter { item -> item.deleteTimestamp == 0L }
            }
            .flowOn(Dispatchers.Default)
    }

    suspend fun loadUnupdatedItems() = withContext(Dispatchers.IO) { collectionDao.loadItemsNotUpdated() }

    suspend fun refresh(options: Map<String, String>, updatedTimestamp: Long = System.currentTimeMillis()): Int = withContext(Dispatchers.IO) {
        var count = 0
        val username = prefs[AccountPreferences.KEY_USERNAME, ""]
        if (!username.isNullOrBlank()) {
            val response = api.collection(username, options)
            response.items?.forEach {
                val item = it.mapToCollectionItem()
                if (isItemStatusSetToSync(item)) {
                    val (collectionId, _) = upsert(it, updatedTimestamp)
                    if (collectionId != INVALID_ID) count++
                } else {
                    Timber.i("Skipped collection item '${item.gameName}' [ID=${item.gameId}, collection ID=${item.collectionId}] - collection status not synced")
                }
            }
        }
        count
    }

    private fun isItemStatusSetToSync(item: CollectionItem): Boolean {
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

    suspend fun refreshCollectionItem(gameId: Int, collectionId: Int, subtype: Game.Subtype?): String? =
        withContext(Dispatchers.IO) {
            val username = prefs[AccountPreferences.KEY_USERNAME, ""]
            if (gameId == INVALID_ID && collectionId == INVALID_ID)
                context.getString(R.string.msg_refresh_collection_item_invalid_id)
            else if (username.isNullOrBlank())
                context.getString(R.string.msg_refresh_collection_item_auth_error)
            else {
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

                val result = safeApiCall(context) { api.collection(username, options) }
                if (result.isSuccess) {
                    val collectionIds = mutableListOf<Int>()
                    val items = result.getOrNull()?.items
                    items?.forEach { collectionItem ->
                        val (id, _) = upsert(collectionItem, timestamp)
                        if (collectionId != INVALID_ID) collectionIds += id
                    }
                    Timber.i("Synced ${collectionIds.size} collection item(s) $collectionIds for game ID=[$gameId]")
                    null
                } else result.exceptionOrNull()?.localizedMessage
            }
        }

    suspend fun refreshHeroImage(item: CollectionItem) = withContext(Dispatchers.IO) {
        val urlMap = imageRepository.getImageUrls(item.thumbnailUrl.getImageId())
        val urls = urlMap[ImageRepository.ImageType.HERO]
        urls?.firstOrNull()?.let {
            collectionDao.updateHeroImageUrl(item.internalId, it)
        }
    }

    suspend fun refreshCollectionItems(gameId: Int, subtype: Game.Subtype? = null): String? = withContext(Dispatchers.IO) {
        val username = prefs[AccountPreferences.KEY_USERNAME, ""]
        if (gameId == INVALID_ID)
            context.getString(R.string.msg_refresh_collection_item_invalid_id)
        else if (username.isNullOrBlank())
            context.getString(R.string.msg_refresh_collection_item_auth_error)
        else {
            val timestamp = System.currentTimeMillis()
            val collectionIds = arrayListOf<Int>()

            val options = mutableMapOf(
                BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE to "1",
                BggService.COLLECTION_QUERY_KEY_STATS to "1",
                BggService.COLLECTION_QUERY_KEY_ID to gameId.toString(),
            )
            options.addSubtype(subtype)
            val result = safeApiCall(context) { api.collection(username, options) }
            if (result.isFailure)
                return@withContext result.exceptionMessage()

            result.getOrNull()?.items?.forEach { collectionItem ->
                val (collectionId, _) = upsert(collectionItem, timestamp)
                collectionIds += collectionId
            }
            val statuses = prefs.getSyncStatusesOrDefault()
            if (result.getOrNull()?.items.isNullOrEmpty() && statuses.contains(BggService.COLLECTION_QUERY_STATUS_PLAYED)) {
                val playedOptions = mutableMapOf(
                    BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE to "1",
                    BggService.COLLECTION_QUERY_KEY_STATS to "1",
                    BggService.COLLECTION_QUERY_KEY_ID to gameId.toString(),
                    BggService.COLLECTION_QUERY_STATUS_PLAYED to "1",
                )
                playedOptions.addSubtype(subtype)
                val playedResponse = safeApiCall(context) { api.collection(username, playedOptions) }
                if (playedResponse.isFailure)
                    return@withContext result.exceptionMessage()

                playedResponse.getOrNull()?.items?.forEach { collectionItem ->
                    val (collectionId, _) = upsert(collectionItem, timestamp)
                    collectionIds += collectionId
                }
            }

            Timber.i("Synced %,d collection item(s) for game '%s'", collectionIds.size, gameId)
            delete(gameId, collectionIds)
            null
        }
    }

    private fun Result<*>.exceptionMessage() = exceptionOrNull()?.localizedMessage ?: "Unknown error"

    private fun Result<*>.exception() = exceptionOrNull() ?: Exception("Unknown error")

    private suspend fun upsert(collectionItem: CollectionItemRemote, timestamp: Long): Pair<Int, Long> = withContext(Dispatchers.IO) {
        val itemForInsert = withContext(Dispatchers.Default) { collectionItem.mapForInsert(timestamp) }

        // upsert info on the Game entity
        val loadedGame = gameDao.loadGame(itemForInsert.gameId)
        if (loadedGame?.game == null || loadedGame.game.gameId == INVALID_ID) {
            val game = withContext(Dispatchers.Default) { collectionItem.mapToCollectionGame(timestamp) }
            val internalId = gameDao.insertGame(game)
            Timber.i("Inserted game '${game.gameName}' (${game.gameId}) [$internalId]")
        } else {
            val game = withContext(Dispatchers.Default) { collectionItem.mapToCollectionGame(timestamp, loadedGame.game.internalId) }
            gameDao.updateGame(game)
            Timber.i("Updated game '${game.gameName}' (${game.gameId}) [${game.internalId}]")
        }

        // find a candidate collection item in the database that should be replaced with this one
        var candidate: CollectionItemWithGameEntity? = null
        if (itemForInsert.collectionId != INVALID_ID) {
            candidate = collectionDao.load(itemForInsert.collectionId)
        }
        if (candidate == null) {
            candidate = collectionDao.loadForGame(itemForInsert.gameId).find { it.item.collectionId == INVALID_ID }
        }

        // upsert the collection item
        if (candidate == null) {
            val internalId = collectionDao.insert(itemForInsert)
            Timber.i("Inserted collection item '${itemForInsert.collectionName}' (${itemForInsert.collectionId}) [$internalId].")
            itemForInsert.collectionId to internalId
        } else {
            val internalId = candidate.item.internalId
            if ((candidate.item.collectionDirtyTimestamp ?: 0L) > 0L) {
                Timber.i("Local copy of collection item '${itemForInsert.collectionName}' (${itemForInsert.collectionId}) [$internalId] is dirty, skipping sync.")
                candidate.item.collectionId to INVALID_ID.toLong()
            } else {
                Timber.i("Updating collection item '${itemForInsert.collectionName}' (${itemForInsert.collectionId}) [$internalId]")
                val count = collectionDao.update(collectionItem.mapForUpdate(internalId, timestamp))
                if (count == 1) {
                    if ((candidate.item.statusDirtyTimestamp ?: 0L) == 0L) {
                        val statuses = CollectionStatusEntity(
                            internalId,
                            statusOwn = itemForInsert.statusOwn,
                            statusPreviouslyOwned = itemForInsert.statusPreviouslyOwned,
                            statusForTrade = itemForInsert.statusForTrade,
                            statusWant = itemForInsert.statusWant,
                            statusWantToPlay = itemForInsert.statusWantToPlay,
                            statusWantToBuy = itemForInsert.statusWantToBuy,
                            statusWishlist = itemForInsert.statusWishlist,
                            statusWishlistPriority = itemForInsert.statusWishlistPriority,
                            statusPreordered = itemForInsert.statusPreordered,
                            statusDirtyTimestamp = 0L,
                        )
                        collectionDao.updateStatuses(statuses)
                    } else Timber.i("Skipping dirty collection statuses")

                    if ((candidate.item.ratingDirtyTimestamp ?: 0L) == 0L) collectionDao.updateRating(internalId, itemForInsert.rating ?: CollectionItem.UNRATED, 0L)
                    else Timber.i("Skipping dirty collection rating")

                    if ((candidate.item.commentDirtyTimestamp ?: 0L) == 0L) collectionDao.updateComment(internalId, itemForInsert.comment.orEmpty(), 0L)
                    else Timber.i("Skipping dirty collection comment")

                    if ((candidate.item.privateInfoDirtyTimestamp ?: 0L) == 0L) {
                        val entity = CollectionPrivateInfoEntity(
                            internalId,
                            itemForInsert.privateInfoPricePaidCurrency,
                            itemForInsert.privateInfoPricePaid,
                            itemForInsert.privateInfoCurrentValueCurrency,
                            itemForInsert.privateInfoCurrentValue,
                            itemForInsert.privateInfoQuantity,
                            itemForInsert.privateInfoAcquisitionDate,
                            itemForInsert.privateInfoAcquiredFrom,
                            itemForInsert.privateInfoInventoryLocation,
                            0L,
                        )
                        collectionDao.updatePrivateInfo(entity)
                    } else Timber.i("Skipping dirty collection private info")

                    if ((candidate.item.wishlistCommentDirtyTimestamp ?: 0L) == 0L) collectionDao.updateWishlistComment(internalId, itemForInsert.wishlistComment.orEmpty(), 0L)
                    else Timber.i("Skipping dirty collection wishlist comment")

                    if ((candidate.item.tradeConditionDirtyTimestamp ?: 0L) == 0L) collectionDao.updateTradeCondition(internalId, itemForInsert.condition.orEmpty(), 0L)
                    else Timber.i("Skipping dirty collection trade condition")

                    if ((candidate.item.wantPartsDirtyTimestamp ?: 0L) == 0L) collectionDao.updateWantParts(internalId, itemForInsert.wantpartsList.orEmpty(), 0L)
                    else Timber.i("Skipping dirty collection want parts list")

                    if ((candidate.item.hasPartsDirtyTimestamp ?: 0L) == 0L) collectionDao.updateHasParts(internalId, itemForInsert.haspartsList.orEmpty(), 0L)
                    else Timber.i("Skipping dirty collection has parts list")

                    if (candidate.item.collectionThumbnailUrl != itemForInsert.collectionThumbnailUrl) {
                        collectionDao.updateHeroImageUrl(internalId, "")
                        val thumbnailFileName = FileUtils.getFileNameFromUrl(candidate.item.collectionThumbnailUrl)
                        val imageDeletedCount = imageRepository.deleteThumbnail(thumbnailFileName)
                        Timber.i("Deleted $imageDeletedCount thumbnails no longer needed for collection ID=[${itemForInsert.collectionId}]")
                    }
                    Timber.i("Updated collection item '${itemForInsert.collectionName}' (${itemForInsert.collectionId}) [$internalId]")
                    candidate.item.collectionId to internalId
                } else {
                    Timber.i("Failed to update collection item '${itemForInsert.collectionName}' (${itemForInsert.collectionId}) [$internalId]")
                    candidate.item.collectionId to INVALID_ID.toLong()
                }
            }
        }
    }

    private suspend fun delete(gameId: Int, protectedCollectionIds: List<Int>) = withContext(Dispatchers.IO) {
        var deleteCount = 0
        val items = collectionDao.loadForGame(gameId)
        items.forEach { item ->
            if (!protectedCollectionIds.contains(item.item.collectionId)) {
                deleteCount += collectionDao.delete(item.item.internalId)
                Timber.i("Deleted collection item ${item.item.collectionName} [${item.item.collectionId}]")
            }
        }
        Timber.i("Removed %,d collection item(s) for game '%s'", deleteCount, gameId)
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

    suspend fun loadAcquiredFrom() = withContext(Dispatchers.IO) { collectionDao.loadAcquiredFrom().filterNot { it.isBlank() } }

    suspend fun loadInventoryLocation() = withContext(Dispatchers.IO) { collectionDao.loadInventoryLocation().filterNot { it.isBlank() } }

    suspend fun loadItemsPendingDeletion() = withContext(Dispatchers.IO) { collectionDao.loadItemsPendingDeletion().map { it.mapToModel() } }

    suspend fun loadItemsPendingInsert() = withContext(Dispatchers.IO) { collectionDao.loadItemsPendingInsert().map { it.mapToModel() } }

    suspend fun loadItemsPendingUpdate() = withContext(Dispatchers.IO) { collectionDao.loadItemsPendingUpdate().map { it.mapToModel() } }

    suspend fun uploadDeletedItem(item: CollectionItem): Result<CollectionItemUploadResult> {
        val result = safeApiCall(context) { phpApi.collection(item.mapToFormBodyForDeletion()) }
        return if (result.isSuccess) {
            val response = result.getOrNull()
            if (response == null) {
                Result.failure(Exception("Unknown error"))
            } else if (response.hasAuthError()) {
                Authenticator.clearPassword(context)
                Result.failure(Exception(context.getString(R.string.msg_play_update_auth_error)))
            } else if (!response.error.isNullOrBlank()) {
                Result.failure(Exception(response.error))
            } else {
                withContext(Dispatchers.IO) {
                    collectionDao.delete(item.internalId).also {
                        Timber.i("Deleted collection item ${item.collectionName} [${item.collectionId}]")
                    }
                }
                Result.success(CollectionItemUploadResult.delete(item))
            }
        } else {
             Result.failure(result.exception())
        }
    }

    suspend fun uploadNewItem(item: CollectionItem): Result<CollectionItemUploadResult> {
        val result = safeApiCall(context) { phpApi.collection(item.mapToFormBodyForInsert()) }
        return if (result.isSuccess) {
            val response = result.getOrNull()
            if (response == null) {
                Result.failure(Exception("Unknown error"))
            } else if (response.hasAuthError()) {
                Authenticator.clearPassword(context)
                Result.failure(Exception(context.getString(R.string.msg_play_update_auth_error)))
            } else if (!response.error.isNullOrBlank()) {
                Result.failure(Exception(response.error))
            } else {
                val count = withContext(Dispatchers.IO) { collectionDao.clearItemDirtyTimestamp(item.internalId) }
                if (count == 1)
                    Result.success(CollectionItemUploadResult.insert(item))
                else
                    Result.failure(Exception("Error inserting into database"))
            }
        } else {
            Result.failure(result.exception())
        }
    }

    suspend fun uploadUpdatedItem(item: CollectionItem): Result<CollectionItemUploadResult> {
        val statusResult =
            updateItemField(
                item.statusDirtyTimestamp,
                item.mapToFormBodyForStatusUpdate(),
                item,
            ) { collectionDao.clearStatusDirtyTimestamp(it.internalId) }
        if (statusResult.isFailure) return statusResult

        val ratingResult =
            updateItemField(
                item.ratingDirtyTimestamp,
                item.mapToFormBodyForRatingUpdate(),
                item,
            ) { collectionDao.clearRatingDirtyTimestamp(it.internalId) }
        if (ratingResult.isFailure) return ratingResult

        val commentResult =
            updateItemField(
                item.commentDirtyTimestamp,
                item.mapToFormBodyForCommentUpdate(),
                item,
            ) { collectionDao.clearCommentDirtyTimestamp(it.internalId) }
        if (commentResult.isFailure) return commentResult

        val privateInfoResult = updateItemField(
            item.privateInfoDirtyTimestamp,
            item.mapToFormBodyForPrivateInfoUpdate(),
            item,
        ) { collectionDao.clearPrivateInfoDirtyTimestamp(it.internalId) }
        if (privateInfoResult.isFailure) return privateInfoResult

        val wishlistCommentResult = updateItemField(
            item.wishListCommentDirtyTimestamp,
            item.mapToFormBodyForWishlistCommentUpdate(),
            item,
        ) { collectionDao.clearWishlistCommentDirtyTimestamp(it.internalId) }
        if (wishlistCommentResult.isFailure) return wishlistCommentResult

        val tradeConditionResult = updateItemField(
            item.tradeConditionDirtyTimestamp,
            item.mapToFormBodyForTradeConditionUpdate(),
            item,
        ) { collectionDao.clearTradeConditionDirtyTimestamp(it.internalId) }
        if (tradeConditionResult.isFailure) return tradeConditionResult

        val wantPartsResult =
            updateItemField(
                item.wantPartsDirtyTimestamp,
                item.mapToFormBodyForWantPartsUpdate(),
                item,
            ) { collectionDao.clearWantPartsDirtyTimestamp(it.internalId) }
        if (wantPartsResult.isFailure) return wantPartsResult

        val hasPartsResult =
            updateItemField(
                item.hasPartsDirtyTimestamp,
                item.mapToFormBodyForHasPartsUpdate(),
                item,
            ) { collectionDao.clearHasPartsDirtyTimestamp(it.internalId) }
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
    ) = withContext(Dispatchers.IO) {
        gameDao.loadGame(gameId)?.let { entity ->
            entity.game?.let {
                val collectionItemForInsert = withContext(Dispatchers.Default) { it.mapForInsert(statuses, wishListPriority, timestamp) }
                val internalId = collectionDao.insert(collectionItemForInsert)
                if (internalId == INVALID_ID.toLong()) {
                    Timber.d("Collection item for game '${it.gameName}` ($gameId) not added")
                } else {
                    Timber.d("Collection item added for game '${it.gameName}` ($gameId) [$internalId]")
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
    ): Int = withContext(Dispatchers.IO) {
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
        collectionDao.updatePrivateInfo(entity)
    }

    suspend fun updateStatuses(internalId: Long, statuses: List<String>, wishlistPriority: Int): Int = withContext(Dispatchers.IO) {
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
        collectionDao.updateStatuses(entity)
    }

    suspend fun updateRating(internalId: Long, rating: Double): Int = withContext(Dispatchers.IO) { collectionDao.updateRating(internalId, rating, System.currentTimeMillis()) }

    suspend fun updateComment(internalId: Long, text: String): Int = withContext(Dispatchers.IO) { collectionDao.updateComment(internalId, text, System.currentTimeMillis()) }

    suspend fun updatePrivateComment(internalId: Long, text: String): Int = withContext(Dispatchers.IO) { collectionDao.updatePrivateComment(internalId, text, System.currentTimeMillis()) }

    suspend fun updateWishlistComment(internalId: Long, text: String): Int = withContext(Dispatchers.IO) { collectionDao.updateWishlistComment(internalId, text, System.currentTimeMillis()) }

    suspend fun updateCondition(internalId: Long, text: String): Int = withContext(Dispatchers.IO) { collectionDao.updateTradeCondition(internalId, text, System.currentTimeMillis()) }

    suspend fun updateHasParts(internalId: Long, text: String): Int = withContext(Dispatchers.IO) { collectionDao.updateHasParts(internalId, text, System.currentTimeMillis()) }

    suspend fun updateWantParts(internalId: Long, text: String): Int = withContext(Dispatchers.IO) { collectionDao.updateWantParts(internalId, text, System.currentTimeMillis()) }

    suspend fun markAsDeleted(internalId: Long): Int = withContext(Dispatchers.IO) { collectionDao.updateDeletedTimestamp(internalId, System.currentTimeMillis()) }

    suspend fun resetTimestamps(internalId: Long): Int = withContext(Dispatchers.IO) { collectionDao.clearDirtyTimestamps(internalId) }

    suspend fun deleteUnupdatedItems(timestamp: Long): Int = withContext(Dispatchers.IO) {
        collectionDao.deleteUnupdatedItems(timestamp).also {
            Timber.i("Deleted $it collection items not updated since ${timestamp.formatTimestamp(context)}")
        }
    }

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
