package com.boardgamegeek.service

import android.content.ContentValues
import android.content.Intent
import android.content.SyncResult
import android.database.Cursor
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.extensions.executeAsyncTask
import com.boardgamegeek.entities.CollectionItemForUploadEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.BggService
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.provider.BggContract.INVALID_ID
import com.boardgamegeek.tasks.sync.SyncCollectionByGameTask
import com.boardgamegeek.ui.CollectionActivity
import com.boardgamegeek.ui.GameActivity
import com.boardgamegeek.util.HttpUtils
import com.boardgamegeek.util.NotificationUtils
import okhttp3.OkHttpClient
import org.jetbrains.anko.intentFor
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

class SyncCollectionUpload(application: BggApplication, service: BggService, syncResult: SyncResult) :
    SyncUploadTask(application, service, syncResult) {
    private val okHttpClient: OkHttpClient = HttpUtils.getHttpClientWithAuth(context)
    private val uploadTasks: List<CollectionUploadTask>
    private var currentGameId: Int = 0
    private var currentGameName: String = ""
    private var currentGameHeroImageUrl: String = ""
    private var currentGameThumbnailUrl: String = ""

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

    override val notificationMessageTag = NotificationUtils.TAG_UPLOAD_COLLECTION

    override val notificationErrorTag = NotificationUtils.TAG_UPLOAD_COLLECTION_ERROR

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
            cursor.getLong(0),
            cursor.getIntOrNull(1) ?: INVALID_ID,
            cursor.getIntOrNull(2) ?: INVALID_ID,
            cursor.getString(3),
            cursor.getStringOrNull(4).orEmpty().ifEmpty { cursor.getStringOrNull(7) }.orEmpty(),
            cursor.getStringOrNull(5).orEmpty().ifEmpty { cursor.getStringOrNull(8) }.orEmpty(),
            cursor.getStringOrNull(6).orEmpty().ifEmpty { cursor.getStringOrNull(9) }.orEmpty(),
            cursor.getDouble(10),
            cursor.getLong(11),
            cursor.getString(12),
            cursor.getLong(13),
            cursor.getString(14),
            cursor.getString(15),
            cursor.getString(16),
            cursor.getDouble(17),
            cursor.getString(18),
            cursor.getDouble(19),
            cursor.getString(20),
            cursor.getInt(21),
            cursor.getString(22),
            cursor.getLong(23),
            cursor.getBoolean(24),
            cursor.getBoolean(25),
            cursor.getBoolean(26),
            cursor.getBoolean(27),
            cursor.getBoolean(28),
            cursor.getBoolean(29),
            cursor.getBoolean(30),
            cursor.getBoolean(31),
            cursor.getInt(32),
            cursor.getLong(33),
            cursor.getString(34),
            cursor.getLong(35),
            cursor.getString(36),
            cursor.getLong(37),
            cursor.getString(38),
            cursor.getLong(39),
            cursor.getString(40),
            cursor.getLong(41),
        )
    }

    private fun fetchDeletedCollectionItems(): Cursor? {
        return getCollectionItems(
            isGreaterThanZero(Collection.COLLECTION_DELETE_TIMESTAMP),
            R.plurals.sync_notification_collection_deleting
        )
    }

    private fun fetchNewCollectionItems(): Cursor? {
        val selection =
            "(${getDirtyColumnSelection(isGreaterThanZero(Collection.COLLECTION_DIRTY_TIMESTAMP))}) AND ${Collection.COLLECTION_ID.whereNullOrBlank()}"
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
        SyncCollectionByGameTask(application, item.gameId).executeAsyncTask()
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
            item.imageUrl,
            item.thumbnailUrl,
            item.heroImageUrl
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
            Collection._ID,
            Collection.COLLECTION_ID,
            Collection.GAME_ID,
            Collection.COLLECTION_NAME,
            Collection.COLLECTION_IMAGE_URL,
            Collection.COLLECTION_THUMBNAIL_URL, // 5
            Collection.COLLECTION_HERO_IMAGE_URL,
            Collection.IMAGE_URL,
            Collection.THUMBNAIL_URL,
            Collection.HERO_IMAGE_URL,
            Collection.RATING, // 10
            Collection.RATING_DIRTY_TIMESTAMP,
            Collection.COMMENT,
            Collection.COMMENT_DIRTY_TIMESTAMP,
            Collection.PRIVATE_INFO_ACQUIRED_FROM,
            Collection.PRIVATE_INFO_ACQUISITION_DATE, // 15
            Collection.PRIVATE_INFO_COMMENT,
            Collection.PRIVATE_INFO_CURRENT_VALUE,
            Collection.PRIVATE_INFO_CURRENT_VALUE_CURRENCY,
            Collection.PRIVATE_INFO_PRICE_PAID,
            Collection.PRIVATE_INFO_PRICE_PAID_CURRENCY, // 20
            Collection.PRIVATE_INFO_QUANTITY,
            Collection.PRIVATE_INFO_INVENTORY_LOCATION,
            Collection.PRIVATE_INFO_DIRTY_TIMESTAMP,
            Collection.STATUS_OWN,
            Collection.STATUS_PREVIOUSLY_OWNED, // 25
            Collection.STATUS_FOR_TRADE,
            Collection.STATUS_WANT,
            Collection.STATUS_WANT_TO_BUY,
            Collection.STATUS_WANT_TO_PLAY,
            Collection.STATUS_PREORDERED, // 30
            Collection.STATUS_WISHLIST,
            Collection.STATUS_WISHLIST_PRIORITY,
            Collection.STATUS_DIRTY_TIMESTAMP,
            Collection.WISHLIST_COMMENT,
            Collection.WISHLIST_COMMENT_DIRTY_TIMESTAMP, // 35
            Collection.CONDITION,
            Collection.TRADE_CONDITION_DIRTY_TIMESTAMP,
            Collection.WANTPARTS_LIST,
            Collection.WANT_PARTS_DIRTY_TIMESTAMP,
            Collection.HASPARTS_LIST, // 40
            Collection.HAS_PARTS_DIRTY_TIMESTAMP,
        )
    }
}
