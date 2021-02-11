package com.boardgamegeek.service

import android.app.PendingIntent
import android.content.Intent
import android.content.SyncResult
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat.Action
import androidx.core.content.contentValuesOf
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.db.PlayDao
import com.boardgamegeek.entities.PlayEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.BggService
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.tasks.CalculatePlayStatsTask
import com.boardgamegeek.ui.GamePlaysActivity
import com.boardgamegeek.ui.LogPlayActivity
import com.boardgamegeek.ui.PlayActivity
import com.boardgamegeek.ui.PlaysActivity
import com.boardgamegeek.util.HttpUtils
import com.boardgamegeek.util.NotificationUtils
import com.boardgamegeek.util.SelectionBuilder
import okhttp3.FormBody
import okhttp3.Request.Builder
import org.jetbrains.anko.intentFor
import java.util.concurrent.TimeUnit

class SyncPlaysUpload(application: BggApplication, service: BggService, syncResult: SyncResult) : SyncUploadTask(application, service, syncResult) {
    private val httpClient = HttpUtils.getHttpClientWithAuth(context)
    private val dao = PlayDao(application)
    private var currentPlay = PlayForNotification()

    inner class PlayForNotification(
            var internalId: Long = INVALID_ID.toLong(),
            val gameId: Int = INVALID_ID,
            val gameName: String = "") {
        var imageUrl: String = ""
        var thumbnailUrl: String = ""
        var heroImageUrl: String = ""
        var customPlayerSort: Boolean = false
    }

    override val syncType = SyncService.FLAG_SYNC_PLAYS_UPLOAD

    override val notificationTitleResId = R.string.sync_notification_title_play_upload

    override val summarySuffixResId = R.plurals.plays_suffix

    override val notificationSummaryIntent = context.intentFor<PlaysActivity>()

    override val notificationIntent: Intent
        get() = if (currentPlay.internalId == INVALID_ID.toLong())
            GamePlaysActivity.createIntent(context,
                    currentPlay.gameId,
                    currentPlay.gameName,
                    currentPlay.imageUrl,
                    currentPlay.thumbnailUrl,
                    currentPlay.heroImageUrl)
        else
            PlayActivity.createIntent(context, currentPlay.internalId)

    override val notificationMessageTag = NotificationUtils.TAG_UPLOAD_PLAY

    override val notificationErrorTag = NotificationUtils.TAG_UPLOAD_PLAY_ERROR

    override val notificationSummaryMessageId = R.string.sync_notification_plays_upload

    override fun execute() {
        deletePendingPlays()
        updatePendingPlays()
        CalculatePlayStatsTask(application).executeAsyncTask()
    }

    private fun updatePendingPlays() {
        val cursor = context.contentResolver.query(
                Plays.CONTENT_SIMPLE_URI,
                arrayOf(Plays._ID),
                Plays.UPDATE_TIMESTAMP + ">0",
                null,
                Plays.UPDATE_TIMESTAMP)
        cursor?.use {
            var currentNumberOfPlays = 0
            val totalNumberOfPlays = it.count
            updateProgressNotificationAsPlural(R.plurals.sync_notification_plays_update, totalNumberOfPlays, totalNumberOfPlays)

            while (it.moveToNext()) {
                if (isCancelled) break
                if (wasSleepInterrupted(1, TimeUnit.SECONDS, false)) break

                updateProgressNotificationAsPlural(R.plurals.sync_notification_plays_update_increment, totalNumberOfPlays, ++currentNumberOfPlays, totalNumberOfPlays)

                try {
                    val internalId = it.getLongOrNull(Plays._ID) ?: INVALID_ID.toLong()
                    val play = dao.loadPlay(internalId) ?: break
                    val response = postPlayUpdate(play)
                    if (response.hasAuthError()) {
                        syncResult.stats.numAuthExceptions++
                        Authenticator.clearPassword(context)
                        break
                    } else if (response.hasInvalidIdError()) {
                        syncResult.stats.numConflictDetectedExceptions++
                        notifyUploadError(context.getText(R.string.msg_play_update_bad_id, play.playId))
                    } else if (response.hasError()) {
                        syncResult.stats.numIoExceptions++
                        notifyUploadError(response.errorMessage)
                    } else if (response.playCount < 0) {
                        syncResult.stats.numIoExceptions++
                        notifyUploadError(context.getString(R.string.msg_play_update_null_response))
                    } else {
                        syncResult.stats.numUpdates++
                        val message = when {
                            play.playId > 0 -> context.getText(R.string.msg_play_updated)
                            play.quantity > 0 -> context.getText(R.string.msg_play_added_quantity, getPlayCountDescription(response.playCount, play.quantity))
                            else -> context.getText(R.string.msg_play_added)
                        }

                        currentPlay = PlayForNotification(internalId, play.gameId, play.gameName)
                        notifyUser(play, message)

                        dao.setAsSynced(internalId, response.playId)
                        updateGamePlayCount(play.gameId)
                    }
                } catch (e: Exception) {
                    syncResult.stats.numParseExceptions++
                    notifyUploadError(e.localizedMessage.orEmpty())
                }
            }
        }
    }

    private fun deletePendingPlays() {
        val cursor = context.contentResolver.query(Plays.CONTENT_SIMPLE_URI,
                arrayOf(Plays._ID),
                Plays.DELETE_TIMESTAMP + ">0",
                null,
                Plays.DELETE_TIMESTAMP)
        cursor?.use {
            var currentNumberOfPlays = 0
            val totalNumberOfPlays = it.count
            updateProgressNotificationAsPlural(R.plurals.sync_notification_plays_delete, totalNumberOfPlays, totalNumberOfPlays)

            while (it.moveToNext()) {
                if (isCancelled) break
                if (wasSleepInterrupted(1, TimeUnit.SECONDS, false)) break

                updateProgressNotificationAsPlural(R.plurals.sync_notification_plays_delete_increment, totalNumberOfPlays, ++currentNumberOfPlays, totalNumberOfPlays)

                try {
                    val internalId = it.getLongOrNull(Plays._ID) ?: INVALID_ID.toLong()
                    val play = dao.loadPlay(internalId) ?: break
                    currentPlay = PlayForNotification(internalId, play.gameId, play.gameName)
                    if (play.playId > 0) {
                        val response = postPlayDelete(play.playId)
                        if (response.isSuccessful) {
                            syncResult.stats.numDeletes++
                            dao.delete(internalId)
                            updateGamePlayCount(play.gameId)
                            notifyUserOfDelete(R.string.msg_play_deleted, play)
                        } else if (response.hasInvalidIdError()) {
                            syncResult.stats.numConflictDetectedExceptions++
                            dao.delete(internalId)
                            notifyUserOfDelete(R.string.msg_play_deleted, play)
                        } else if (response.hasAuthError()) {
                            syncResult.stats.numAuthExceptions++
                            Authenticator.clearPassword(context)
                            break
                        } else {
                            syncResult.stats.numIoExceptions++
                            notifyUploadError(response.errorMessage)
                        }
                    } else {
                        syncResult.stats.numDeletes++
                        dao.delete(internalId)
                        notifyUserOfDelete(R.string.msg_play_deleted_draft, play)
                    }
                } catch (e: Exception) {
                    syncResult.stats.numParseExceptions++
                    notifyUploadError(e.localizedMessage.orEmpty())
                }
            }
        }
    }

    private fun updateGamePlayCount(gameId: Int) {
        val resolver = context.contentResolver
        val cursor = resolver.query(Plays.CONTENT_SIMPLE_URI,
                arrayOf(Plays.SUM_QUANTITY),
                "${Plays.OBJECT_ID}=? AND ${SelectionBuilder.whereZeroOrNull(Plays.DELETE_TIMESTAMP)}",
                arrayOf(gameId.toString()),
                null)
        cursor?.use {
            if (it.moveToFirst()) {
                val values = contentValuesOf(Collection.NUM_PLAYS to it.getInt(0))
                resolver.update(Games.buildGameUri(gameId), values, null, null)
            }
        }
    }

    @Suppress("SpellCheckingInspection")
    private fun postPlayUpdate(play: PlayEntity): PlaySaveResponse {
        val bodyBuilder = FormBody.Builder()
                .add("ajax", "1")
                .add("action", "save")
                .add("version", "2")
                .add("objecttype", "thing")
        if (play.playId > 0) {
            bodyBuilder.add("playid", play.playId.toString())
        }
        bodyBuilder.add("objectid", play.gameId.toString())
                .add("playdate", play.dateInMillis.asDateForApi())
                .add("dateinput", play.dateInMillis.asDateForApi())
                .add("length", play.length.toString())
                .add("location", play.location)
                .add("quantity", play.quantity.toString())
                .add("incomplete", if (play.incomplete) "1" else "0")
                .add("nowinstats", if (play.noWinStats) "1" else "0")
                .add("comments", play.comments)
        val players = play.players
        for (i in players.indices) {
            val player = players[i]
            bodyBuilder
                    .add(getMapKey(i, "playerid"), "player_$i")
                    .add(getMapKey(i, "name"), player.name)
                    .add(getMapKey(i, "username"), player.username)
                    .add(getMapKey(i, "color"), player.color.orEmpty())
                    .add(getMapKey(i, "position"), player.startingPosition.orEmpty())
                    .add(getMapKey(i, "score"), player.score.orEmpty())
                    .add(getMapKey(i, "rating"), player.rating.toString())
                    .add(getMapKey(i, "new"), if (player.isNew) "1" else "0")
                    .add(getMapKey(i, "win"), if (player.isWin) "1" else "0")
        }

        val request = Builder()
                .url(GEEK_PLAY_URL)
                .post(bodyBuilder.build())
                .build()
        return PlaySaveResponse(httpClient, request)
    }

    private fun postPlayDelete(playId: Int): PlayDeleteResponse {
        @Suppress("SpellCheckingInspection")
        val bodyBuilder = FormBody.Builder()
                .add("ajax", "1")
                .add("action", "delete")
                .add("playid", playId.toString())
                .add("finalize", "1")

        val request = Builder()
                .url(GEEK_PLAY_URL)
                .post(bodyBuilder.build())
                .build()
        return PlayDeleteResponse(httpClient, request)
    }

    private fun getPlayCountDescription(count: Int, quantity: Int): String {
        return when (quantity) {
            1 -> count.toOrdinal()
            2 -> "${(count - 1).toOrdinal()} & ${count.toOrdinal()}"
            else -> "${(count - quantity + 1).toOrdinal()} - ${count.toOrdinal()}"
        }
    }

    private fun notifyUserOfDelete(@StringRes messageId: Int, play: PlayEntity) {
        NotificationUtils.cancel(context,
                notificationMessageTag,
                NotificationUtils.getIntegerId(currentPlay.internalId).toLong())
        currentPlay.internalId = INVALID_ID.toLong()
        notifyUser(play, context.getText(messageId, play.gameName))
    }

    private fun notifyUser(play: PlayEntity, message: CharSequence) {
        if (play.gameId != INVALID_ID) {
            val gameCursor = context.contentResolver.query(
                    Games.buildGameUri(play.gameId),
                    arrayOf(Games.IMAGE_URL, Games.THUMBNAIL_URL, Games.HERO_IMAGE_URL, Games.CUSTOM_PLAYER_SORT),
                    null,
                    null,
                    null)
            gameCursor?.use {
                if (it.moveToFirst()) {
                    currentPlay.imageUrl = it.getString(0) ?: ""
                    currentPlay.thumbnailUrl = it.getString(1) ?: ""
                    currentPlay.heroImageUrl = it.getString(2) ?: ""
                    currentPlay.customPlayerSort = it.getInt(3) == 1
                }
            }
        }
        notifyUser(play.gameName,
                message,
                NotificationUtils.getIntegerId(currentPlay.internalId),
                currentPlay.imageUrl,
                currentPlay.thumbnailUrl,
                currentPlay.heroImageUrl)
    }

    override fun createMessageAction(): Action? {
        if (currentPlay.internalId != INVALID_ID.toLong()) {
            val intent = LogPlayActivity.createRematchIntent(
                    context,
                    currentPlay.internalId,
                    currentPlay.gameId,
                    currentPlay.gameName,
                    currentPlay.imageUrl,
                    currentPlay.thumbnailUrl,
                    currentPlay.heroImageUrl,
                    currentPlay.customPlayerSort,
            )
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            val builder = Action.Builder(
                    R.drawable.ic_replay_black_24dp,
                    context.getString(R.string.rematch),
                    pendingIntent)
            return builder.build()
        }
        return null
    }

    companion object {
        const val GEEK_PLAY_URL = "https://www.boardgamegeek.com/geekplay.php"

        private fun getMapKey(index: Int, key: String) = "players[$index][$key]"
    }
}
