package com.boardgamegeek.service

import androidx.core.app.NotificationCompat
import androidx.work.ListenableWorker
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemForUploadEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.repository.GameCollectionRepository
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

class SyncCollectionUpload(
    application: BggApplication,
    private val gameCollectionRepository: GameCollectionRepository,
) : SyncTask(application) {
    override val syncType = SyncService.FLAG_SYNC_COLLECTION_UPLOAD

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

    private suspend fun doIt(items: List<CollectionItemForUploadEntity>, resId: Int, process: suspend (item: CollectionItemForUploadEntity) -> Unit) {
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
            context.notifyUploadCollectionItem(result.getOrNull()!!, R.string.sync_notification_collection_deleted)
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
            context.notifyUploadCollectionItem(result.getOrNull()!!, R.string.sync_notification_collection_added)
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
            context.notifyUploadCollectionItem(result.getOrNull()!!, R.string.sync_notification_collection_updated)
            gameCollectionRepository.refreshCollectionItems(item.gameId)
            ListenableWorker.Result.success()
        }
    }

    private fun notifyUploadError(errorMessage: CharSequence) {
        if (errorMessage.isBlank()) return
        if (prefs[KEY_SYNC_ERRORS, false] != true) return

        Timber.w(errorMessage.toString())
        val builder = context
            .createNotificationBuilder(R.string.sync_notification_title_collection_upload_error, NotificationChannels.ERROR)
            .setContentText(errorMessage)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setStyle(NotificationCompat.BigTextStyle().bigText(errorMessage))
        context.notify(builder, NotificationTags.UPLOAD_COLLECTION_ERROR)
    }
}
