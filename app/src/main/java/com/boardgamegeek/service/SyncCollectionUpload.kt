package com.boardgamegeek.service

import android.content.ContentValues
import android.content.Intent
import android.content.SyncResult
import android.database.Cursor
import android.provider.BaseColumns
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.core.database.getDoubleOrNull
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.entities.CollectionItemForUploadEntity
import com.boardgamegeek.extensions.NotificationTags
import com.boardgamegeek.extensions.getBoolean
import com.boardgamegeek.extensions.intentFor
import com.boardgamegeek.extensions.whereNullOrBlank
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import com.boardgamegeek.provider.BggContract.Games
import com.boardgamegeek.repository.GameCollectionRepository
import com.boardgamegeek.ui.CollectionActivity
import com.boardgamegeek.ui.GameActivity
import com.boardgamegeek.util.HttpUtils
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import timber.log.Timber
import java.util.concurrent.TimeUnit

class SyncCollectionUpload(application: BggApplication, syncResult: SyncResult) :
    SyncUploadTask(application, syncResult) {
    private val okHttpClient: OkHttpClient = HttpUtils.getHttpClientWithAuth(context)
    private val uploadTasks: List<CollectionUploadTask>
    private var currentGameId: Int = 0
    private var currentGameName: String = ""
    private var currentGameHeroImageUrl: String = ""
    private var currentGameThumbnailUrl: String = ""
    private val repository = GameCollectionRepository(application)

    override val syncType = SyncService.FLAG_SYNC_COLLECTION_UPLOAD

    override val notificationTitleResId = R.string.sync_notification_title_collection_upload

    override val summarySuffixResId = R.plurals.collection_items_suffix

    override val notificationSummaryIntent = context.intentFor<CollectionActivity>()

    override val notificationIntent: Intent?
        get() = if (currentGameId != INVALID_ID) {
            GameActivity.createIntent(
                context,
                currentGameId,
                currentGameName,
                currentGameThumbnailUrl,
                currentGameHeroImageUrl
            )
        } else super.notificationIntent

    override val notificationMessageTag = NotificationTags.UPLOAD_COLLECTION

    override val notificationErrorTag = NotificationTags.UPLOAD_COLLECTION_ERROR

    init {
        uploadTasks = createUploadTasks()
    }

    private fun createUploadTasks(): List<CollectionUploadTask> {
        val tasks = ArrayList<CollectionUploadTask>()
        tasks.add(CollectionStatusUploadTask(okHttpClient))
        tasks.add(CollectionRatingUploadTask(okHttpClient))
        tasks.add(CollectionCommentUploadTask(okHttpClient))
        tasks.add(CollectionPrivateInfoUploadTask(okHttpClient))
        tasks.add(CollectionWishlistCommentUploadTask(okHttpClient))
        tasks.add(CollectionTradeConditionUploadTask(okHttpClient))
        tasks.add(CollectionWantPartsUploadTask(okHttpClient))
        tasks.add(CollectionHasPartsUploadTask(okHttpClient))
        return tasks
    }

    override fun execute() {
        fetchList(fetchDeletedCollectionItems()).forEach {
            if (isCancelled) return@forEach
            if (wasSleepInterrupted(1, TimeUnit.SECONDS)) return@forEach
            processDeletedCollectionItem(it)
        }

        fetchList(fetchNewCollectionItems()).forEach {
            if (isCancelled) return@forEach
            if (wasSleepInterrupted(1, TimeUnit.SECONDS)) return@forEach
            processNewCollectionItem(it)
        }

        fetchList(fetchDirtyCollectionItems()).forEach {
            if (isCancelled) return@forEach
            if (wasSleepInterrupted(1, TimeUnit.SECONDS)) return@forEach
            processDirtyCollectionItem(it)
        }
    }

    private fun fetchList(cursor: Cursor?): MutableList<CollectionItemForUploadEntity> {
        val list = mutableListOf<CollectionItemForUploadEntity>()
        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    list.add(fromCursor(it))
                } while (it.moveToNext())
            }
        }
        return list
    }

    private fun fromCursor(cursor: Cursor): CollectionItemForUploadEntity {
        return CollectionItemForUploadEntity(
            internalId = cursor.getLong(0),
            collectionId = cursor.getIntOrNull(1) ?: INVALID_ID,
            gameId = cursor.getIntOrNull(2) ?: INVALID_ID,
            collectionName = cursor.getStringOrNull(3).orEmpty(),
            imageUrl = cursor.getStringOrNull(4).orEmpty().ifEmpty { cursor.getStringOrNull(7) }.orEmpty(),
            thumbnailUrl = cursor.getStringOrNull(5).orEmpty().ifEmpty { cursor.getStringOrNull(8) }.orEmpty(),
            heroImageUrl = cursor.getStringOrNull(6).orEmpty().ifEmpty { cursor.getStringOrNull(9) }.orEmpty(),
            rating = cursor.getDoubleOrNull(10) ?: 0.0,
            ratingTimestamp = cursor.getLongOrNull(11) ?: 0L,
            comment = cursor.getStringOrNull(12).orEmpty(),
            commentTimestamp = cursor.getLongOrNull(13) ?: 0L,
            acquiredFrom = cursor.getStringOrNull(14).orEmpty(),
            acquisitionDate = cursor.getStringOrNull(15).orEmpty(),
            privateComment = cursor.getStringOrNull(16).orEmpty(),
            currentValue = cursor.getDoubleOrNull(17) ?: 0.0,
            currentValueCurrency = cursor.getStringOrNull(18).orEmpty(),
            pricePaid = cursor.getDoubleOrNull(19) ?: 0.0,
            pricePaidCurrency = cursor.getStringOrNull(20).orEmpty(),
            quantity = cursor.getIntOrNull(21) ?: 1,
            inventoryLocation = cursor.getStringOrNull(22).orEmpty(),
            privateInfoTimestamp = cursor.getLongOrNull(23) ?: 0L,
            owned = cursor.getBoolean(24),
            previouslyOwned = cursor.getBoolean(25),
            forTrade = cursor.getBoolean(26),
            wantInTrade = cursor.getBoolean(27),
            wantToBuy = cursor.getBoolean(28),
            wantToPlay = cursor.getBoolean(29),
            preordered = cursor.getBoolean(30),
            wishlist = cursor.getBoolean(31),
            wishlistPriority = cursor.getIntOrNull(32) ?: 3, // Like to Have
            statusTimestamp = cursor.getLongOrNull(33) ?: 0L,
            wishlistComment = cursor.getString(34),
            wishlistCommentDirtyTimestamp = cursor.getLongOrNull(35) ?: 0L,
            tradeCondition = cursor.getStringOrNull(36),
            tradeConditionDirtyTimestamp = cursor.getLongOrNull(37) ?: 0L,
            wantParts = cursor.getStringOrNull(38),
            wantPartsDirtyTimestamp = cursor.getLongOrNull(39) ?: 0L,
            hasParts = cursor.getStringOrNull(40),
            hasPartsDirtyTimestamp = cursor.getLongOrNull(41) ?: 0L,
        )
    }

    private fun fetchDeletedCollectionItems(): Cursor? {
        return getCollectionItems(
            isGreaterThanZero(Collection.Columns.COLLECTION_DELETE_TIMESTAMP),
            R.plurals.sync_notification_collection_deleting
        )
    }

    private fun fetchNewCollectionItems(): Cursor? {
        val selection =
            "(${getDirtyColumnSelection(isGreaterThanZero(Collection.Columns.COLLECTION_DIRTY_TIMESTAMP))}) AND ${Collection.Columns.COLLECTION_ID.whereNullOrBlank()}"
        return getCollectionItems(selection, R.plurals.sync_notification_collection_adding)
    }

    private fun fetchDirtyCollectionItems(): Cursor? {
        val selection = getDirtyColumnSelection("")
        return getCollectionItems(selection, R.plurals.sync_notification_collection_uploading)
    }

    private fun getDirtyColumnSelection(existingSelection: String): String {
        val sb = StringBuilder(existingSelection)
        for (task in uploadTasks) {
            if (sb.isNotEmpty()) sb.append(" OR ")
            sb.append(isGreaterThanZero(task.timestampColumn))
        }
        return sb.toString()
    }

    private fun isGreaterThanZero(columnName: String): String {
        return "$columnName>0"
    }

    private fun getCollectionItems(selection: String, @PluralsRes messageResId: Int): Cursor? {
        val cursor = context.contentResolver.query(
            Collection.CONTENT_URI,
            PROJECTION,
            selection, null, null
        )
        val count = cursor?.count ?: 0
        val detail = context.resources.getQuantityString(messageResId, count, count)
        Timber.i(detail)
        if (count > 0) updateProgressNotification(detail)
        return cursor
    }

    private fun processDeletedCollectionItem(item: CollectionItemForUploadEntity) {
        val deleteTask = CollectionDeleteTask(okHttpClient, item)
        deleteTask.post()
        if (processResponseForError(deleteTask)) {
            return
        }
        context.contentResolver.delete(Collection.buildUri(item.internalId), null, null)
        notifySuccess(item, item.collectionId, R.string.sync_notification_collection_deleted)
    }

    private fun processNewCollectionItem(item: CollectionItemForUploadEntity) {
        val addTask = CollectionAddTask(okHttpClient, item)
        addTask.post()
        if (processResponseForError(addTask)) {
            return
        }
        val contentValues = ContentValues()
        addTask.appendContentValues(contentValues)
        context.contentResolver.update(Collection.buildUri(item.internalId), contentValues, null, null)
        runBlocking {
            repository.refreshCollectionItems(item.gameId)
        }
        notifySuccess(item, item.gameId * -1, R.string.sync_notification_collection_added)
    }

    private fun processDirtyCollectionItem(item: CollectionItemForUploadEntity) {
        if (item.collectionId != INVALID_ID) {
            val contentValues = ContentValues()
            for (task in uploadTasks) {
                if (processUploadTask(task, item, contentValues)) return
            }
            if (contentValues.size() > 0) {
                context.contentResolver.update(Collection.buildUri(item.internalId), contentValues, null, null)
                notifySuccess(item, item.collectionId, R.string.sync_notification_collection_updated)
            }
        } else {
            Timber.d("Invalid collectionItem ID for internal ID %1\$s; game ID %2\$s", item.internalId, item.gameId)
        }
    }

    private fun processUploadTask(
        task: CollectionUploadTask,
        collectionItem: CollectionItemForUploadEntity,
        contentValues: ContentValues
    ): Boolean {
        task.addCollectionItem(collectionItem)
        if (task.isDirty) {
            task.post()
            if (processResponseForError(task)) {
                return true
            }
            task.appendContentValues(contentValues)
        }
        return false
    }

    private fun notifySuccess(item: CollectionItemForUploadEntity, id: Int, @StringRes messageResId: Int) {
        syncResult.stats.numUpdates++
        currentGameId = item.gameId
        currentGameName = item.collectionName
        currentGameHeroImageUrl = item.heroImageUrl
        currentGameThumbnailUrl = item.thumbnailUrl
        notifyUser(
            item.collectionName,
            context.getString(messageResId),
            id,
            item.heroImageUrl,
            item.thumbnailUrl,
            item.imageUrl,
        )
    }

    private fun processResponseForError(response: CollectionTask): Boolean {
        return when {
            response.hasAuthError() -> {
                Timber.w("Auth error; clearing password")
                syncResult.stats.numAuthExceptions++
                Authenticator.clearPassword(context)
                true
            }
            !response.errorMessage.isNullOrBlank() -> {
                syncResult.stats.numIoExceptions++
                notifyUploadError(response.errorMessage)
                true
            }
            else -> false
        }
    }

    companion object {
        val PROJECTION = arrayOf(
            BaseColumns._ID,
            Collection.Columns.COLLECTION_ID,
            Games.Columns.GAME_ID,
            Collection.Columns.COLLECTION_NAME,
            Collection.Columns.COLLECTION_IMAGE_URL,
            Collection.Columns.COLLECTION_THUMBNAIL_URL, // 5
            Collection.Columns.COLLECTION_HERO_IMAGE_URL,
            Games.Columns.IMAGE_URL,
            Games.Columns.THUMBNAIL_URL,
            Games.Columns.HERO_IMAGE_URL,
            Collection.Columns.RATING, // 10
            Collection.Columns.RATING_DIRTY_TIMESTAMP,
            Collection.Columns.COMMENT,
            Collection.Columns.COMMENT_DIRTY_TIMESTAMP,
            Collection.Columns.PRIVATE_INFO_ACQUIRED_FROM,
            Collection.Columns.PRIVATE_INFO_ACQUISITION_DATE, // 15
            Collection.Columns.PRIVATE_INFO_COMMENT,
            Collection.Columns.PRIVATE_INFO_CURRENT_VALUE,
            Collection.Columns.PRIVATE_INFO_CURRENT_VALUE_CURRENCY,
            Collection.Columns.PRIVATE_INFO_PRICE_PAID,
            Collection.Columns.PRIVATE_INFO_PRICE_PAID_CURRENCY, // 20
            Collection.Columns.PRIVATE_INFO_QUANTITY,
            Collection.Columns.PRIVATE_INFO_INVENTORY_LOCATION,
            Collection.Columns.PRIVATE_INFO_DIRTY_TIMESTAMP,
            Collection.Columns.STATUS_OWN,
            Collection.Columns.STATUS_PREVIOUSLY_OWNED, // 25
            Collection.Columns.STATUS_FOR_TRADE,
            Collection.Columns.STATUS_WANT,
            Collection.Columns.STATUS_WANT_TO_BUY,
            Collection.Columns.STATUS_WANT_TO_PLAY,
            Collection.Columns.STATUS_PREORDERED, // 30
            Collection.Columns.STATUS_WISHLIST,
            Collection.Columns.STATUS_WISHLIST_PRIORITY,
            Collection.Columns.STATUS_DIRTY_TIMESTAMP,
            Collection.Columns.WISHLIST_COMMENT,
            Collection.Columns.WISHLIST_COMMENT_DIRTY_TIMESTAMP, // 35
            Collection.Columns.CONDITION,
            Collection.Columns.TRADE_CONDITION_DIRTY_TIMESTAMP,
            Collection.Columns.WANTPARTS_LIST,
            Collection.Columns.WANT_PARTS_DIRTY_TIMESTAMP,
            Collection.Columns.HASPARTS_LIST, // 40
            Collection.Columns.HAS_PARTS_DIRTY_TIMESTAMP,
        )
    }
}
