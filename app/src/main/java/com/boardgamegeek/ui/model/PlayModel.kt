package com.boardgamegeek.ui.model

import android.content.Context
import android.database.Cursor
import android.database.CursorIndexOutOfBoundsException
import com.boardgamegeek.provider.BggContract

import com.boardgamegeek.provider.BggContract.Games
import com.boardgamegeek.provider.BggContract.Plays
import com.boardgamegeek.util.CursorUtils
import timber.log.Timber

class PlayModel(
        val playId: Int,
        val gameId: Int,
        val name: String = "",
        val date: String = "",
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

    private constructor() : this(BggContract.INVALID_ID, BggContract.INVALID_ID)

    companion object {
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
                        cursor.getInt(PLAY_ID),
                        cursor.getInt(GAME_ID),
                        cursor.getString(GAME_NAME) ?: "",
                        CursorUtils.getFormattedDateAbbreviated(cursor, context, DATE),
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
                return PlayModel()
            }
        }
    }
}
