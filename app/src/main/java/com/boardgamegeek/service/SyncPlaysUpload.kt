package com.boardgamegeek.service

import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SyncResult
import android.support.annotation.StringRes
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationCompat.Action
import android.util.Pair
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.io.BggService
import com.boardgamegeek.model.Play
import com.boardgamegeek.model.PlayDeleteResponse
import com.boardgamegeek.model.PlaySaveResponse
import com.boardgamegeek.model.builder.PlayBuilder
import com.boardgamegeek.model.persister.PlayPersister
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.tasks.CalculatePlayStatsTask
import com.boardgamegeek.ui.GamePlaysActivity
import com.boardgamegeek.ui.LogPlayActivity
import com.boardgamegeek.ui.PlayActivity
import com.boardgamegeek.ui.PlaysActivity
import com.boardgamegeek.util.*
import hugo.weaving.DebugLog
import okhttp3.FormBody
import okhttp3.Request.Builder

class SyncPlaysUpload @DebugLog
constructor(context: Context, service: BggService, syncResult: SyncResult) : SyncUploadTask(context, service, syncResult) {
    private val httpClient = HttpUtils.getHttpClientWithAuth(context)
    private val persister = PlayPersister(context)
    private var currentInternalId: Long = 0
    private var currentGameIdForNotification: Int = 0
    private var currentGameNameForNotification: String = ""
    private var currentThumbnailUrlForNotification: String = ""
    private var currentImageUrlForNotification: String = ""

    override val syncType = SyncService.FLAG_SYNC_PLAYS_UPLOAD

    override val notificationTitleResId = R.string.sync_notification_title_play_upload

    override val notificationSummaryIntent = Intent(context, PlaysActivity::class.java)

    override val notificationIntent: Intent
        get() = if (currentInternalId == BggContract.INVALID_ID.toLong())
            GamePlaysActivity.createIntent(context,
                    currentGameIdForNotification,
                    currentGameNameForNotification,
                    currentImageUrlForNotification,
                    currentThumbnailUrlForNotification)
        else
            PlayActivity.createIntent(context,
                    currentInternalId,
                    currentGameIdForNotification,
                    currentGameNameForNotification,
                    currentThumbnailUrlForNotification,
                    currentImageUrlForNotification)

    override val notificationMessageTag = NotificationUtils.TAG_UPLOAD_PLAY

    override val notificationErrorTag = NotificationUtils.TAG_UPLOAD_PLAY_ERROR

    override val notificationSummaryMessageId = R.string.sync_notification_plays_upload

    @DebugLog
    override fun execute() {
        deletePendingPlays()
        updatePendingPlays()
        if (SyncPrefs.isPlaysSyncUpToDate(context)) {
            TaskUtils.executeAsyncTask(CalculatePlayStatsTask(context))
        }
    }

    @DebugLog
    private fun updatePendingPlays() {
        val cursor = context.contentResolver.query(
                Plays.CONTENT_SIMPLE_URI,
                PlayBuilder.PLAY_PROJECTION_WITH_ID,
                Plays.UPDATE_TIMESTAMP + ">0",
                null,
                Plays.UPDATE_TIMESTAMP)
        cursor?.use { c ->
            val playCount = c.count
            updateProgressNotificationAsPlural(R.plurals.sync_notification_progress_update, playCount, playCount)

            while (c.moveToNext()) {
                if (isCancelled) break
                if (wasSleepInterrupted(1000)) break

                currentInternalId = CursorUtils.getLong(c, Plays._ID, BggContract.INVALID_ID.toLong())
                val play = PlayBuilder.fromCursor(c)
                val playerCursor = PlayBuilder.queryPlayers(context, currentInternalId)
                playerCursor.use { playerCursor1 ->
                    PlayBuilder.addPlayers(playerCursor1, play)
                }

                val response = postPlayUpdate(play)
                if (response.hasAuthError()) {
                    syncResult.stats.numAuthExceptions++
                    Authenticator.clearPassword(context)
                    break
                } else if (response.hasInvalidIdError()) {
                    syncResult.stats.numConflictDetectedExceptions++
                    notifyUploadError(PresentationUtils.getText(context, R.string.msg_play_update_bad_id, play.playId))
                } else if (response.hasError()) {
                    syncResult.stats.numIoExceptions++
                    notifyUploadError(response.errorMessage)
                } else if (response.playCount <= 0) {
                    syncResult.stats.numIoExceptions++
                    notifyUploadError(context.getString(R.string.msg_play_update_null_response))
                } else {
                    syncResult.stats.numUpdates++
                    val message = if (play.playId > 0)
                        PresentationUtils.getText(context, R.string.msg_play_updated)
                    else
                        PresentationUtils.getText(context, R.string.msg_play_added, getPlayCountDescription(response.playCount, play.quantity))

                    play.playId = response.playId
                    play.dirtyTimestamp = 0
                    play.updateTimestamp = 0
                    play.deleteTimestamp = 0
                    currentGameIdForNotification = play.gameId
                    currentGameNameForNotification = play.gameName

                    notifyUser(play, message)
                    persister.save(play, currentInternalId, false)

                    updateGamePlayCount(play)
                }
            }
        }
    }

    private fun notifyUser(play: Play, message: CharSequence) {
        val imageUrls = queryGameImageUrls(play)
        currentImageUrlForNotification = imageUrls.first
        currentThumbnailUrlForNotification = imageUrls.second
        notifyUser(play.gameName, message, NotificationUtils.getIntegerId(currentInternalId), currentImageUrlForNotification, currentThumbnailUrlForNotification)
    }

    private fun queryGameImageUrls(play: Play): Pair<String, String> {
        var imageUrls = Pair.create("", "")
        val gameCursor = context.contentResolver.query(
                Games.buildGameUri(play.gameId),
                arrayOf(Games.IMAGE_URL, Games.THUMBNAIL_URL),
                null,
                null,
                null)
        gameCursor?.use { c ->
            if (c.moveToFirst()) {
                imageUrls = Pair.create(c.getString(0), c.getString(1))
            }
        }
        return imageUrls
    }

    @DebugLog
    private fun deletePendingPlays() {
        val cursor = context.contentResolver.query(Plays.CONTENT_SIMPLE_URI,
                PlayBuilder.PLAY_PROJECTION_WITH_ID,
                Plays.DELETE_TIMESTAMP + ">0",
                null,
                Plays.DELETE_TIMESTAMP)
        cursor?.use { c ->
            val playCount = c.count
            updateProgressNotificationAsPlural(R.plurals.sync_notification_progress_delete, playCount, playCount)

            while (c.moveToNext()) {
                if (isCancelled) break
                currentInternalId = CursorUtils.getLong(c, Plays._ID, BggContract.INVALID_ID.toLong())
                val play = PlayBuilder.fromCursor(c)
                currentGameIdForNotification = play.gameId
                currentGameNameForNotification = play.gameName
                if (play.playId > 0) {
                    if (wasSleepInterrupted(1000)) break
                    val response = postPlayDelete(play.playId)
                    if (response.isSuccessful) {
                        syncResult.stats.numDeletes++
                        deletePlay(currentInternalId)
                        updateGamePlayCount(play)
                        notifyUserOfDelete(R.string.msg_play_deleted, play)
                    } else if (response.hasInvalidIdError()) {
                        syncResult.stats.numConflictDetectedExceptions++
                        deletePlay(currentInternalId)
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
                    deletePlay(currentInternalId)
                    notifyUserOfDelete(R.string.msg_play_deleted_draft, play)
                }
            }
        }
    }

    @DebugLog
    private fun updateGamePlayCount(play: Play) {
        val resolver = context.contentResolver
        val cursor = resolver.query(Plays.CONTENT_SIMPLE_URI,
                arrayOf(Plays.SUM_QUANTITY),
                String.format("%s=? AND %s", Plays.OBJECT_ID, SelectionBuilder.whereZeroOrNull(Plays.DELETE_TIMESTAMP)),
                arrayOf(play.gameId.toString()),
                null)
        cursor?.use { c ->
            if (c.moveToFirst()) {
                val newPlayCount = c.getInt(0)
                val values = ContentValues()
                values.put(Collection.NUM_PLAYS, newPlayCount)
                resolver.update(Games.buildGameUri(play.gameId), values, null, null)
            }
        }
    }

    @DebugLog
    private fun postPlayUpdate(play: Play): PlaySaveResponse {
        val builder = FormBody.Builder()
                .add("ajax", "1")
                .add("action", "save")
                .add("version", "2")
                .add("objecttype", "thing")
        if (play.playId > 0) {
            builder.add("playid", play.playId.toString())
        }
        builder.add("objectid", play.gameId.toString())
                .add("playdate", play.date)
                .add("dateinput", play.date)
                .add("length", play.length.toString())
                .add("location", play.location)
                .add("quantity", play.quantity.toString())
                .add("incomplete", if (play.Incomplete()) "1" else "0")
                .add("nowinstats", if (play.NoWinStats()) "1" else "0")
                .add("comments", play.comments)
        val players = play.players
        for (i in players.indices) {
            val player = players[i]
            builder
                    .add(getMapKey(i, "playerid"), "player_$i")
                    .add(getMapKey(i, "name"), player.name)
                    .add(getMapKey(i, "username"), player.username)
                    .add(getMapKey(i, "color"), player.color)
                    .add(getMapKey(i, "position"), player.startposition)
                    .add(getMapKey(i, "score"), player.score)
                    .add(getMapKey(i, "rating"), player.rating.toString())
                    .add(getMapKey(i, "new"), player.new_.toString())
                    .add(getMapKey(i, "win"), player.win.toString())
        }

        val request = Builder()
                .url(GEEK_PLAY_URL)
                .post(builder.build())
                .build()
        return PlaySaveResponse(httpClient, request)
    }

    @DebugLog
    private fun postPlayDelete(playId: Int): PlayDeleteResponse {
        val builder = FormBody.Builder()
                .add("ajax", "1")
                .add("action", "delete")
                .add("playid", playId.toString())
                .add("finalize", "1")

        val request = Builder()
                .url(GEEK_PLAY_URL)
                .post(builder.build())
                .build()
        return PlayDeleteResponse(httpClient, request)
    }

    /**
     * Deletes the specified play from the content provider
     */
    @DebugLog
    private fun deletePlay(internalId: Long) {
        persister.delete(internalId)
    }

    @DebugLog
    private fun getPlayCountDescription(count: Int, quantity: Int): String {
        return when (quantity) {
            1 -> StringUtils.getOrdinal(count)
            2 -> StringUtils.getOrdinal(count - 1) + " & " + StringUtils.getOrdinal(count)
            else -> StringUtils.getOrdinal(count - quantity + 1) + " - " + StringUtils.getOrdinal(count)
        }
    }

    @DebugLog
    private fun notifyUserOfDelete(@StringRes messageId: Int, play: Play) {
        NotificationUtils.cancel(context, notificationMessageTag, NotificationUtils.getIntegerId(currentInternalId).toLong())
        currentInternalId = BggContract.INVALID_ID.toLong()
        notifyUser(play, PresentationUtils.getText(context, messageId, play.gameName))
    }

    @DebugLog
    override fun createMessageAction(): Action? {
        if (currentInternalId != BggContract.INVALID_ID.toLong()) {
            val intent = LogPlayActivity.createRematchIntent(context,
                    currentInternalId,
                    currentGameIdForNotification,
                    currentGameNameForNotification,
                    currentThumbnailUrlForNotification,
                    currentImageUrlForNotification)
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            val builder = NotificationCompat.Action.Builder(
                    R.drawable.ic_replay_black_24dp,
                    context.getString(R.string.rematch),
                    pendingIntent)
            return builder.build()
        }
        return null
    }

    companion object {
        const val GEEK_PLAY_URL = "https://www.boardgamegeek.com/geekplay.php"

        @DebugLog
        private fun getMapKey(index: Int, key: String): String {
            return "players[$index][$key]"
        }
    }
}
