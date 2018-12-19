package com.boardgamegeek.ui.model

import android.content.Context
import android.database.Cursor
import android.database.CursorIndexOutOfBoundsException
import android.text.format.DateUtils
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.Games
import com.boardgamegeek.provider.BggContract.Plays
import timber.log.Timber
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class PlayModel(
        private val context: Context,
        val internalId: Long,
        val playId: Int,
        val gameId: Int,
        val name: String = "",
        private val rawDate: String = "",
        val location: String = "",
        val quantity: Int = 1,
        val length: Int = 0,
        val playerCount: Int = 0,
        val comments: String = "",
        val thumbnailUrl: String = "",
        val imageUrl: String = "",
        val heroImageUrl: String = "",
        val deleteTimestamp: Long = 0,
        val updateTimestamp: Long = 0,
        val dirtyTimestamp: Long = 0
) {

    private constructor(context: Context) : this(context, BggContract.INVALID_ID.toLong(), BggContract.INVALID_ID, BggContract.INVALID_ID)

    val date: String by lazy {
        if (dateInMillis == UNKNOWN_DATE)
            ""
        else
            DateUtils.formatDateTime(context, dateInMillis, DateUtils.FORMAT_SHOW_WEEKDAY or  DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_ALL)
    }

    val dateInMillis: Long by lazy {
        if (!rawDate.isEmpty()) {
            try {
                return@lazy FORMAT.parse(rawDate).time
            } catch (e: ParseException) {
                Timber.w(e, "Unable to parse %s as %s", rawDate, FORMAT)
            } catch (e: ArrayIndexOutOfBoundsException) {
                Timber.w(e, "Unable to parse %s as %s", rawDate, FORMAT)
            }
        }
        UNKNOWN_DATE
    }

    companion object {
        const val UNKNOWN_DATE: Long = -1
        private val FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        @JvmStatic
        val projection = arrayOf(
                Plays._ID,
                Plays.PLAY_ID,
                Plays.DATE,
                Plays.ITEM_NAME,
                Plays.OBJECT_ID,
                Plays.LOCATION,
                Plays.QUANTITY,
                Plays.LENGTH,
                Plays.PLAYER_COUNT,
                Games.THUMBNAIL_URL,
                Games.IMAGE_URL,
                Plays.COMMENTS,
                Plays.DELETE_TIMESTAMP,
                Plays.UPDATE_TIMESTAMP,
                Plays.DIRTY_TIMESTAMP,
                Games.HERO_IMAGE_URL
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

        @JvmStatic
        fun fromCursor(cursor: Cursor, context: Context): PlayModel {
            try {
                return PlayModel(
                        context,
                        cursor.getLong(INTERNAL_ID),
                        cursor.getInt(PLAY_ID),
                        cursor.getInt(GAME_ID),
                        cursor.getString(GAME_NAME) ?: "",
                        cursor.getString(DATE) ?: "",
                        cursor.getString(LOCATION) ?: "",
                        cursor.getInt(QUANTITY),
                        cursor.getInt(LENGTH),
                        cursor.getInt(PLAYER_COUNT),
                        (cursor.getString(COMMENTS) ?: "").trim(),
                        cursor.getString(THUMBNAIL_URL) ?: "",
                        cursor.getString(IMAGE_URL) ?: "",
                        cursor.getString(HERO_IMAGE_URL) ?: "",
                        cursor.getLong(DELETE_TIMESTAMP),
                        cursor.getLong(UPDATE_TIMESTAMP),
                        cursor.getLong(DIRTY_TIMESTAMP)
                )
            } catch (e: CursorIndexOutOfBoundsException) {
                Timber.w(e)
                return PlayModel(context)
            }
        }
    }
}
