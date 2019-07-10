package com.boardgamegeek.entities

import android.content.Context
import android.database.Cursor
import android.database.CursorIndexOutOfBoundsException
import android.text.format.DateUtils
import com.boardgamegeek.R
import com.boardgamegeek.extensions.asPastDaySpan
import com.boardgamegeek.extensions.asTime
import com.boardgamegeek.extensions.toMillis
import com.boardgamegeek.provider.BggContract
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

data class PlayEntity(
        val internalId: Long,
        val playId: Int,
        private val rawDate: String,
        val gameId: Int,
        val gameName: String,
        val quantity: Int,
        val length: Int,
        val location: String,
        val incomplete: Boolean,
        val noWinStats: Boolean,
        val comments: String,
        val syncTimestamp: Long,
        val playerCount: Int,
        val dirtyTimestamp: Long,
        val updateTimestamp: Long,
        val deleteTimestamp: Long,
        val startTime: Long,
        val imageUrl: String = "",
        val thumbnailUrl: String = "",
        val heroImageUrl: String = ""
) {
    val dateInMillis: Long by lazy {
        rawDate.toMillis(FORMAT, UNKNOWN_DATE)
    }

    fun dateForDisplay(context: Context): CharSequence {
        return dateInMillis.asPastDaySpan(context, includeWeekDay = true)
    }

    fun describe(context: Context, includeDate: Boolean = false): String {
        val info = StringBuilder()
        if (quantity > 1) info.append(context.resources.getQuantityString(R.plurals.play_description_quantity_segment, quantity, quantity))
        if (includeDate && dateInMillis != UNKNOWN_DATE) {
            val date = DateUtils.formatDateTime(context, dateInMillis, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_ALL or DateUtils.FORMAT_SHOW_WEEKDAY)
            info.append(context.getString(R.string.play_description_date_segment, date))
        }
        if (location.isNotBlank()) info.append(context.getString(R.string.play_description_location_segment, location))
        if (length > 0) info.append(context.getString(R.string.play_description_length_segment, length.asTime()))
        if (playerCount > 0) info.append(context.resources.getQuantityString(R.plurals.play_description_players_segment, playerCount, playerCount))
        return info.trim().toString()
    }

    companion object {
        private val FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        private const val UNKNOWN_DATE: Long = -1L

        // TEMP for ViewModel
        @JvmStatic
        val projection = arrayOf(
                BggContract.Plays._ID,
                BggContract.Plays.PLAY_ID,
                BggContract.Plays.DATE,
                BggContract.Plays.ITEM_NAME,
                BggContract.Plays.OBJECT_ID,
                BggContract.Plays.LOCATION,
                BggContract.Plays.QUANTITY,
                BggContract.Plays.LENGTH,
                BggContract.Plays.PLAYER_COUNT,
                BggContract.Games.THUMBNAIL_URL,
                BggContract.Games.IMAGE_URL,
                BggContract.Plays.COMMENTS,
                BggContract.Plays.DELETE_TIMESTAMP,
                BggContract.Plays.UPDATE_TIMESTAMP,
                BggContract.Plays.DIRTY_TIMESTAMP,
                BggContract.Games.HERO_IMAGE_URL,
                BggContract.Plays.SYNC_TIMESTAMP
        )

        private const val INTERNAL_ID = 0
        private const val PLAY_ID = 1
        private const val DATE = 2
        private const val GAME_NAME = 3
        private const val GAME_ID = 4
        private const val LOCATION = 5
        private const val QUANTITY = 6
        private const val LENGTH = 7
        private const val PLAYER_COUNT = 8
        private const val THUMBNAIL_URL = 9
        private const val IMAGE_URL = 10
        private const val COMMENTS = 11
        private const val DELETE_TIMESTAMP = 12
        private const val UPDATE_TIMESTAMP = 13
        private const val DIRTY_TIMESTAMP = 14
        private const val HERO_IMAGE_URL = 15
        private const val SYNC_TIMESTAMP = 16

        @JvmStatic
        fun fromCursor(cursor: Cursor): PlayEntity {
            try {
                return PlayEntity(
                        cursor.getLong(INTERNAL_ID),
                        cursor.getInt(PLAY_ID),
                        cursor.getString(DATE) ?: "",
                        cursor.getInt(GAME_ID),
                        cursor.getString(GAME_NAME) ?: "",
                        cursor.getInt(QUANTITY),
                        cursor.getInt(LENGTH),
                        cursor.getString(LOCATION) ?: "",
                        incomplete = false, // TODO
                        noWinStats = false, // TODO
                        comments = (cursor.getString(COMMENTS) ?: "").trim(),
                        syncTimestamp = cursor.getLong(SYNC_TIMESTAMP),
                        playerCount = cursor.getInt(PLAYER_COUNT),
                        dirtyTimestamp = cursor.getLong(DIRTY_TIMESTAMP),
                        updateTimestamp = cursor.getLong(UPDATE_TIMESTAMP),
                        deleteTimestamp = cursor.getLong(DELETE_TIMESTAMP),
                        startTime = 0,
                        imageUrl = cursor.getString(IMAGE_URL) ?: "",
                        thumbnailUrl = cursor.getString(THUMBNAIL_URL) ?: "",
                        heroImageUrl = cursor.getString(HERO_IMAGE_URL) ?: ""
                )
            } catch (e: CursorIndexOutOfBoundsException) {
                Timber.w(e)
                return PlayEntity(
                        BggContract.INVALID_ID.toLong(),
                        BggContract.INVALID_ID,
                        "",
                        BggContract.INVALID_ID,
                        "",
                        0,
                        0,
                        "",
                        incomplete = false,
                        noWinStats = false,
                        comments = "",
                        syncTimestamp = 0,
                        playerCount = 0,
                        dirtyTimestamp = 0,
                        updateTimestamp = 0,
                        deleteTimestamp = 0,
                        startTime = 0
                )
            }
        }
    }
}
