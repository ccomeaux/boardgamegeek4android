package com.boardgamegeek.work

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.boardgamegeek.R
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.CollectionItemUploadResult
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.GameCollectionRepository
import com.boardgamegeek.ui.CollectionActivity
import com.boardgamegeek.ui.GameActivity
import com.boardgamegeek.util.LargeIconLoader
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.concurrent.TimeUnit

@HiltWorker
class CollectionUploadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val gameCollectionRepository: GameCollectionRepository,
) : CoroutineWorker(appContext, workerParams) {
    private val prefs: SharedPreferences by lazy { appContext.preferences() }
    private var requestedGameId = BggContract.INVALID_ID

    override suspend fun doWork(): Result {
        requestedGameId = inputData.getInt(GAME_ID, BggContract.INVALID_ID)

        processList(gameCollectionRepository.loadItemsPendingDeletion(), R.plurals.sync_notification_collection_deleting) {
            gameCollectionRepository.uploadDeletedItem(it)
        }
        processList(gameCollectionRepository.loadItemsPendingInsert(), R.plurals.sync_notification_collection_adding) {
            gameCollectionRepository.uploadNewItem(it)
        }
        processList(gameCollectionRepository.loadItemsPendingUpdate(), R.plurals.sync_notification_collection_uploading) {
            gameCollectionRepository.uploadUpdatedItem(it)
        }

        return Result.success()
    }

    private suspend fun processList(items: List<CollectionItem>, resId: Int, process: suspend (item: CollectionItem) -> kotlin.Result<CollectionItemUploadResult>) {
        val list = if (requestedGameId == BggContract.INVALID_ID) items else items.filter { it.gameId == requestedGameId }
        val count = list.size
        val detail = applicationContext.resources.getQuantityString(resId, count, count)
        Timber.i(detail)
        if (count > 0) setForeground(createForegroundInfo(detail))
        list.forEach {
            if (isStopped) return@forEach
            val result = process(it)
            if (result.isFailure) {
                notifyUploadError(result.exceptionOrNull()?.message.orEmpty())
                Result.failure()
            } else {
                applicationContext.notifyUploadCollectionItem(result.getOrNull()!!)
                gameCollectionRepository.refreshCollectionItems(it.gameId)
                Result.success()
            }
        }
    }

    private fun createForegroundInfo(contentText: String): ForegroundInfo {
        return applicationContext.createForegroundInfo(R.string.sync_notification_title_collection_upload, NOTIFICATION_ID_COLLECTION_UPLOAD, id, contentText)
    }

    fun Context.notifyUploadCollectionItem(result: CollectionItemUploadResult) {
        if (this.preferences()[KEY_SYNC_UPLOADS, true] != true) return

        val imageUrls = listOf(result.item.thumbnailUrl, result.item.heroImageUrl, result.item.imageUrl)
        val messageResId = when (result.status) {
            CollectionItemUploadResult.Status.NEW -> R.string.sync_notification_collection_added
            CollectionItemUploadResult.Status.UPDATE -> R.string.sync_notification_collection_updated
            CollectionItemUploadResult.Status.DELETE -> R.string.sync_notification_collection_deleted

        }
        val message = getString(messageResId)

        val loader = LargeIconLoader(this, *imageUrls.toTypedArray(), callback = object : LargeIconLoader.Callback {
            override fun onSuccessfulIconLoad(bitmap: Bitmap) {
                buildAndNotify(this@notifyUploadCollectionItem, result.item.collectionName, message, bitmap)
            }

            override fun onFailedIconLoad() {
                buildAndNotify(this@notifyUploadCollectionItem, result.item.collectionName, message)
            }

            fun buildAndNotify(context: Context, title: CharSequence, message: CharSequence, largeIcon: Bitmap? = null) {
                val intent = GameActivity.createIntent(
                    context,
                    result.item.gameId,
                    result.item.gameName,
                    result.item.heroImageUrl,
                )

                val builder = context.createNotificationBuilder(title, NotificationChannels.SYNC_UPLOAD, intent)
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setLargeIcon(largeIcon)
                    .setOnlyAlertOnce(true)
                    .setGroup(NotificationTags.UPLOAD_COLLECTION)
                    .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                context.notify(builder, NotificationTags.UPLOAD_COLLECTION, result.item.internalId.toInt())

                val summaryBuilder = context.createNotificationBuilder(
                    message,
                    NotificationChannels.SYNC_UPLOAD,
                    context.intentFor<CollectionActivity>()
                )
                    .setGroup(NotificationTags.UPLOAD_COLLECTION)
                    .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                    .setGroupSummary(true)
                context.notify(summaryBuilder, NotificationTags.UPLOAD_COLLECTION, 0)
            }
        })
        loader.executeInBackground()
    }

    private fun notifyUploadError(errorMessage: CharSequence) {
        if (errorMessage.isBlank()) return
        Timber.w(errorMessage.toString())
        if (prefs[KEY_SYNC_ERRORS, false] != true) return
        val builder = applicationContext
            .createNotificationBuilder(R.string.sync_notification_title_collection_upload_error, NotificationChannels.ERROR)
            .setContentText(errorMessage)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setStyle(NotificationCompat.BigTextStyle().bigText(errorMessage))
        applicationContext.notify(builder, NotificationTags.UPLOAD_COLLECTION_ERROR)
    }

    companion object {
        const val GAME_ID = "GAME_ID"

        fun buildRequest(context: Context, gameId: Int = BggContract.INVALID_ID) =
            OneTimeWorkRequestBuilder<CollectionUploadWorker>()
                .setInputData(workDataOf(GAME_ID to gameId))
                .setConstraints(context.createWorkConstraints())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 3, TimeUnit.MINUTES)
                .build()
    }
}