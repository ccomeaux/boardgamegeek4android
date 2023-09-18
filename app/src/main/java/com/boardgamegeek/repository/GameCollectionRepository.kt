package com.boardgamegeek.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.work.*
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.db.GameDao
import com.boardgamegeek.entities.*
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.PhpApi
import com.boardgamegeek.mappers.*
import com.boardgamegeek.provider.BggContract.Collection
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
) {
    private val dao = CollectionDao(context)
    private val gameDao = GameDao(context)
    private val username: String? by lazy { context.preferences()[AccountPreferences.KEY_USERNAME, ""] }
    private val prefs: SharedPreferences by lazy { context.preferences() }

    suspend fun loadCollectionItem(internalId: Long) = dao.load(internalId)?.map {
        it.second.mapToEntity(it.first.mapToEntity())
    }?.firstOrNull()

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
                val response = api.collection(username, options)

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
        val urlMap = imageRepository.getImageUrls(item.thumbnailUrl.getImageId())
        val urls = urlMap[ImageRepository.ImageType.HERO]
        urls?.firstOrNull()?.let {
            dao.updateHeroImageUrl(item.internalId, it)
            item.copy(heroImageUrl = it)
        } ?: item
    }

    suspend fun loadCollectionItems(gameId: Int) = dao.loadByGame(gameId).map {
        it.second.mapToEntity(it.first.mapToEntity())
    }

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
            val response = api.collection(username, options)
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
                val playedResponse = api.collection(username, playedOptions)
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

    suspend fun loadItemsPendingDeletion() = dao.loadItemsPendingDeletion().map {
        it.second.mapToEntity(it.first.mapToEntity())
    }

    suspend fun loadItemsPendingInsert() = dao.loadItemsPendingInsert().map {
        it.second.mapToEntity(it.first.mapToEntity())
    }

    suspend fun loadItemsPendingUpdate() = dao.loadItemsPendingUpdate().map {
        it.second.mapToEntity(it.first.mapToEntity())
    }

    suspend fun uploadDeletedItem(item: CollectionItemEntity): Result<CollectionItemUploadResult> {
        val response = phpApi.collection(item.mapToFormBodyForDeletion())
        return if (response.hasAuthError()) {
            Authenticator.clearPassword(context)
            Result.failure(Exception(context.getString(R.string.msg_play_update_auth_error)))
        } else if (!response.error.isNullOrBlank()) {
            Result.failure(Exception(response.error))
        } else {
            context.contentResolver.delete(Collection.buildUri(item.internalId), null, null)
            Result.success(CollectionItemUploadResult.delete(item))
        }
    }

    suspend fun uploadNewItem(item: CollectionItemEntity): Result<CollectionItemUploadResult> {
        val response = phpApi.collection(item.mapToFormBodyForInsert())
        return if (response.hasAuthError()) {
            Authenticator.clearPassword(context)
            Result.failure(Exception(context.getString(R.string.msg_play_update_auth_error)))
        } else if (!response.error.isNullOrBlank()) {
            Result.failure(Exception(response.error))
        } else {
            val count = dao.clearDirtyTimestamp(item.internalId)
            if (count == 1)
                Result.success(CollectionItemUploadResult.insert(item))
            else
                Result.failure(Exception("Error inserting into database"))
        }
    }

    suspend fun uploadUpdatedItem(item: CollectionItemEntity): Result<CollectionItemUploadResult> {
        val statusResult =
            updateItemField(
                item.statusDirtyTimestamp,
                item.mapToFormBodyForStatusUpdate(),
                item,
            ) { dao.clearStatusTimestampColumn(it.internalId) }
        if (statusResult.isFailure) return statusResult

        val ratingResult =
            updateItemField(
                item.ratingDirtyTimestamp,
                item.mapToFormBodyForRatingUpdate(),
                item,
            ) { dao.clearRatingTimestampColumn(it.internalId) }
        if (ratingResult.isFailure) return ratingResult

        val commentResult =
            updateItemField(
                item.commentDirtyTimestamp,
                item.mapToFormBodyForCommentUpdate(),
                item,
            ) { dao.clearCommentTimestampColumn(it.internalId) }
        if (commentResult.isFailure) return commentResult

        val privateInfoResult = updateItemField(
            item.privateInfoDirtyTimestamp,
            item.mapToFormBodyForPrivateInfoUpdate(),
            item,
        ) { dao.clearPrivateInfoTimestampColumn(it.internalId) }
        if (privateInfoResult.isFailure) return privateInfoResult

        val wishlistCommentResult = updateItemField(
            item.wishListCommentDirtyTimestamp,
            item.mapToFormBodyForWishlistCommentUpdate(),
            item,
        ) { dao.clearWishListTimestampColumn(it.internalId) }
        if (wishlistCommentResult.isFailure) return wishlistCommentResult

        val tradeConditionResult = updateItemField(
            item.tradeConditionDirtyTimestamp,
            item.mapToFormBodyForTradeConditionUpdate(),
            item,
        ) { dao.clearTradeConditionTimestampColumn(it.internalId) }
        if (tradeConditionResult.isFailure) return tradeConditionResult

        val wantPartsResult =
            updateItemField(
                item.wantPartsDirtyTimestamp,
                item.mapToFormBodyForWantPartsUpdate(),
                item,
            ) { dao.clearWantPartsTimestampColumn(it.internalId) }
        if (wantPartsResult.isFailure) return wantPartsResult

        val hasPartsResult =
            updateItemField(
                item.hasPartsDirtyTimestamp,
                item.mapToFormBodyForHasPartsUpdate(),
                item,
            ) { dao.clearHasPartsTimestampColumn(it.internalId) }
        if (hasPartsResult.isFailure) return hasPartsResult

        return Result.success(CollectionItemUploadResult.update(item))
    }

    private suspend fun updateItemField(
        timestamp: Long,
        formBody: FormBody,
        item: CollectionItemEntity,
        clearTimestamp: suspend (CollectionItemEntity) -> Int
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
        if (gameId != INVALID_ID) {
            val game = gameDao.load(gameId)
            val internalId = dao.addNewCollectionItem(gameId, game, statuses, wishListPriority, timestamp)
            if (internalId == INVALID_ID.toLong()) {
                Timber.d("Collection item for game %s (%s) not added", game?.gameName.orEmpty(), gameId)
            } else {
                Timber.d("Collection item added for game %s (%s) (internal ID = %s)", game?.gameName.orEmpty(), gameId, internalId)
                enqueueUploadRequest(gameId)
            }
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
    ) = dao.updatePrivateInfo(
        internalId,
        priceCurrency,
        price,
        currentValueCurrency,
        currentValue,
        quantity,
        acquisitionDate,
        acquiredFrom,
        inventoryLocation
    )

    suspend fun updateStatuses(internalId: Long, statuses: List<String>, wishlistPriority: Int) =
        dao.updateStatuses(internalId, statuses, wishlistPriority)

    suspend fun updateRating(internalId: Long, rating: Double): Int = dao.updateRating(internalId, rating)

    suspend fun updateComment(internalId: Long, text: String): Int = dao.updateComment(internalId, text)

    suspend fun updatePrivateComment(internalId: Long, text: String): Int = dao.updatePrivateComment(internalId, text)

    suspend fun updateWishlistComment(internalId: Long, text: String): Int = dao.updateWishlistComment(internalId, text)

    suspend fun updateCondition(internalId: Long, text: String): Int = dao.updateCondition(internalId, text)

    suspend fun updateHasParts(internalId: Long, text: String): Int = dao.updateHasParts(internalId, text)

    suspend fun updateWantParts(internalId: Long, text: String): Int = dao.updateWantParts(internalId, text)

    suspend fun markAsDeleted(internalId: Long): Int = dao.markAsDeleted(internalId)

    suspend fun resetTimestamps(internalId: Long): Int = dao.resetTimestamps(internalId)

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
