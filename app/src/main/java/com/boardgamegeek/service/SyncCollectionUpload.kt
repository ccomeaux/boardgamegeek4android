package com.boardgamegeek.service

import android.content.Intent
import android.content.SyncResult
import androidx.annotation.StringRes
import androidx.work.ListenableWorker
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemForUploadEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import com.boardgamegeek.repository.GameCollectionRepository
import com.boardgamegeek.ui.CollectionActivity
import com.boardgamegeek.ui.GameActivity
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

class SyncCollectionUpload(
    application: BggApplication,
    syncResult: SyncResult,
    private val gameCollectionRepository: GameCollectionRepository,
) : SyncUploadTask(application, syncResult) {
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

    override val notificationMessageTag = NotificationTags.UPLOAD_COLLECTION

    override val notificationErrorTag = NotificationTags.UPLOAD_COLLECTION_ERROR

    override fun execute() {
        runBlocking {
            doIt(gameCollectionRepository.loadItemsPendingDeletion(), R.plurals.sync_notification_collection_deleting) {
                processDeletedCollectionItem(it)
            }
            doIt(gameCollectionRepository.loadItemsPendingInsert(), R.plurals.sync_notification_collection_adding) {
                processNewCollectionItem(it)
            }
            doIt(gameCollectionRepository.loadItemsPendingUpdate(), R.plurals.sync_notification_collection_uploading) {
                processDirtyCollectionItem(it)
            }
        }
    }

    private suspend fun doIt(items: List<CollectionItemForUploadEntity>, resId: Int,  process: suspend (item: CollectionItemForUploadEntity) -> Unit) {
        val count = items.size
        val detail = context.resources.getQuantityString(resId, count, count)
        Timber.i(detail)
        if (count > 0) updateProgressNotification(detail)
        items.forEach {
            if (isCancelled) return@forEach
            if (wasSleepInterrupted(1.seconds)) return@forEach
            process(it)
        }
    }

    private suspend fun processDeletedCollectionItem(item: CollectionItemForUploadEntity): ListenableWorker.Result {
        val result = gameCollectionRepository.uploadDeletedItem(item)
        return if (result.isFailure) {
            notifyUploadError(result.exceptionOrNull()?.message.orEmpty())
            ListenableWorker.Result.failure()
        } else {
            notifySuccess(item, item.collectionId, R.string.sync_notification_collection_deleted)
            gameCollectionRepository.refreshCollectionItems(item.gameId)
            ListenableWorker.Result.success()
        }
    }

    private suspend fun processNewCollectionItem(item: CollectionItemForUploadEntity): ListenableWorker.Result {
        val result = gameCollectionRepository.uploadNewItem(item)
        return if (result.isFailure) {
            notifyUploadError(result.exceptionOrNull()?.message.orEmpty())
            ListenableWorker.Result.failure()
        } else {
            notifySuccess(item, item.gameId * -1, R.string.sync_notification_collection_added)
            gameCollectionRepository.refreshCollectionItems(item.gameId)
            ListenableWorker.Result.success()
        }
    }

    private suspend fun processDirtyCollectionItem(item: CollectionItemForUploadEntity): ListenableWorker.Result {
        val result = gameCollectionRepository.uploadUpdatedItem(item)
        return if (result.isFailure) {
            notifyUploadError(result.exceptionOrNull()?.message.orEmpty())
            ListenableWorker.Result.failure()
        } else {
            notifySuccess(item, item.collectionId, R.string.sync_notification_collection_updated)
            gameCollectionRepository.refreshCollectionItems(item.gameId)
            ListenableWorker.Result.success()
        }
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
}
