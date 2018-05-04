package com.boardgamegeek.service

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SyncResult
import android.database.Cursor
import android.support.annotation.PluralsRes
import android.support.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.io.BggService
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.service.model.CollectionItem
import com.boardgamegeek.tasks.sync.SyncCollectionByGameTask
import com.boardgamegeek.ui.CollectionActivity
import com.boardgamegeek.ui.GameActivity
import com.boardgamegeek.use
import com.boardgamegeek.util.HttpUtils
import com.boardgamegeek.util.NotificationUtils
import com.boardgamegeek.util.SelectionBuilder
import com.boardgamegeek.util.TaskUtils
import hugo.weaving.DebugLog
import okhttp3.OkHttpClient
import org.jetbrains.anko.intentFor
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

class SyncCollectionUpload(context: Context, service: BggService, syncResult: SyncResult) : SyncUploadTask(context, service, syncResult) {
    private val okHttpClient: OkHttpClient = HttpUtils.getHttpClientWithAuth(context)
    private val deleteTask: CollectionDeleteTask
    private val addTask: CollectionAddTask
    private val uploadTasks: List<CollectionUploadTask>
    private var currentGameId: Int = 0
    private var currentGameName: String? = null
    private var currentGameImageUrl: String = ""

    override val syncType = SyncService.FLAG_SYNC_COLLECTION_UPLOAD

    override val notificationTitleResId = R.string.sync_notification_title_collection_upload

    override val notificationSummaryIntent = context.intentFor<CollectionActivity>()

    override val notificationIntent: Intent?
        get() = if (currentGameId != BggContract.INVALID_ID) {
            GameActivity.createIntent(context, currentGameId, currentGameName, currentGameImageUrl, "", "")
        } else super.notificationIntent

    override val notificationMessageTag = NotificationUtils.TAG_UPLOAD_COLLECTION

    override val notificationErrorTag = NotificationUtils.TAG_UPLOAD_COLLECTION_ERROR

    init {
        deleteTask = CollectionDeleteTask(okHttpClient)
        addTask = CollectionAddTask(okHttpClient)
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

    @DebugLog
    override fun execute() {
        val deletedItemsCursor = fetchDeletedCollectionItems()
        deletedItemsCursor?.use {
            while (it.moveToNext()) {
                if (isCancelled) break
                if (wasSleepInterrupted(1000)) break
                processDeletedCollectionItem(it)
            }
        }

        val newItemsCursor = fetchNewCollectionItems()
        newItemsCursor?.use {
            while (it.moveToNext()) {
                if (isCancelled) break
                if (wasSleepInterrupted(1, TimeUnit.SECONDS)) break
                processNewCollectionItem(it)
            }
        }

        val dirtyItemsCursor = fetchDirtyCollectionItems()
        dirtyItemsCursor?.use {
            while (it.moveToNext()) {
                if (isCancelled) break
                if (wasSleepInterrupted(1, TimeUnit.SECONDS)) break
                processDirtyCollectionItem(it)
            }
        }
    }

    private fun fetchDeletedCollectionItems(): Cursor? {
        return getCollectionItems(isGreaterThanZero(Collection.COLLECTION_DELETE_TIMESTAMP), R.plurals.sync_notification_collection_deleting)
    }

    private fun fetchNewCollectionItems(): Cursor? {
        val selection = "(" + getDirtyColumnSelection(isGreaterThanZero(Collection.COLLECTION_DIRTY_TIMESTAMP)) + ") AND " +
                SelectionBuilder.whereNullOrEmpty(Collection.COLLECTION_ID)
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
        val cursor = context.contentResolver.query(Collection.CONTENT_URI,
                CollectionItem.PROJECTION,
                selection, null, null)
        val count = cursor?.count ?: 0
        val detail = context.resources.getQuantityString(messageResId, count, count)
        Timber.i(detail)
        if (count > 0) updateProgressNotification(detail)
        return cursor
    }

    private fun processDeletedCollectionItem(cursor: Cursor) {
        val item = CollectionItem.fromCursor(cursor)
        deleteTask.addCollectionItem(item)
        deleteTask.post()
        if (processResponseForError(deleteTask)) {
            return
        }
        context.contentResolver.delete(Collection.buildUri(item.internalId), null, null)
        notifySuccess(item, item.collectionId, R.string.sync_notification_collection_deleted)
    }

    private fun processNewCollectionItem(cursor: Cursor) {
        val item = CollectionItem.fromCursor(cursor)
        addTask.addCollectionItem(item)
        addTask.post()
        if (processResponseForError(addTask)) {
            return
        }
        val contentValues = ContentValues()
        addTask.appendContentValues(contentValues)
        context.contentResolver.update(Collection.buildUri(item.internalId), contentValues, null, null)
        TaskUtils.executeAsyncTask(SyncCollectionByGameTask(context, item.gameId))
        notifySuccess(item, item.gameId * -1, R.string.sync_notification_collection_added)
    }

    private fun processDirtyCollectionItem(cursor: Cursor) {
        val item = CollectionItem.fromCursor(cursor)
        if (item.collectionId != BggContract.INVALID_ID) {
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

    private fun processUploadTask(task: CollectionUploadTask, collectionItem: CollectionItem, contentValues: ContentValues): Boolean {
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

    private fun notifySuccess(item: CollectionItem, id: Int, @StringRes messageResId: Int) {
        syncResult.stats.numUpdates++
        currentGameId = item.gameId
        currentGameName = item.collectionName
        currentGameImageUrl = item.imageUrl ?: ""
        notifyUser(item.collectionName, context.getString(messageResId), id, item.imageUrl, item.thumbnailUrl, item.heroImageUrl)
    }

    private fun processResponseForError(response: CollectionTask): Boolean {
        return when {
            response.hasAuthError() -> {
                Timber.w("Auth error; clearing password")
                syncResult.stats.numAuthExceptions++
                Authenticator.clearPassword(context)
                true
            }
            response.hasError() -> {
                syncResult.stats.numIoExceptions++
                notifyUploadError(response.errorMessage)
                true
            }
            else -> false
        }
    }
}
