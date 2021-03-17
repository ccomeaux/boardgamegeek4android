package com.boardgamegeek.model.builder

import android.database.Cursor
import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.Play
import com.boardgamegeek.model.Player
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.PlayPlayers
import com.boardgamegeek.provider.BggContract.Plays
import com.boardgamegeek.util.CursorUtils

object PlayBuilder {
    val PLAY_PROJECTION = arrayOf(
            Plays.PLAY_ID,
            Plays.ITEM_NAME,
            Plays.OBJECT_ID,
            Plays.DATE,
            Plays.LOCATION,
            Plays.LENGTH,
            Plays.QUANTITY,
            Plays.INCOMPLETE,
            Plays.NO_WIN_STATS,
            Plays.COMMENTS,
            Plays.SYNC_TIMESTAMP,
            Plays.START_TIME,
            Plays.DELETE_TIMESTAMP,
            Plays.UPDATE_TIMESTAMP,
            Plays.DIRTY_TIMESTAMP
    )

    val PLAYER_PROJECTION = arrayOf(
            PlayPlayers.USER_ID,
            PlayPlayers.USER_NAME,
            PlayPlayers.NAME,
            PlayPlayers.START_POSITION,
            PlayPlayers.COLOR,
            PlayPlayers.SCORE,
            PlayPlayers.RATING,
            PlayPlayers.NEW,
            PlayPlayers.WIN
    )

    fun fromCursor(cursor: Cursor): Play {
        return Play(
                gameId = cursor.getIntOrNull(Plays.OBJECT_ID) ?: BggContract.INVALID_ID,
                gameName = cursor.getStringOrEmpty(Plays.ITEM_NAME),
                dateInMillis = cursor.getStringOrEmpty(Plays.DATE).toMillis(Play.FORMAT_DATABASE),
                quantity = cursor.getIntOrNull(Plays.QUANTITY) ?: Play.QUANTITY_DEFAULT,
                length = cursor.getIntOrNull(Plays.LENGTH) ?: Play.LENGTH_DEFAULT,
                location = cursor.getStringOrEmpty(Plays.LOCATION),
                comments = cursor.getStringOrEmpty(Plays.COMMENTS),
                playId = cursor.getIntOrNull(Plays.PLAY_ID) ?: BggContract.INVALID_ID,
                incomplete = cursor.getBoolean(Plays.INCOMPLETE),
                noWinStats = cursor.getBoolean(Plays.NO_WIN_STATS),
                startTime = cursor.getLongOrZero(Plays.START_TIME),
                syncTimestamp = cursor.getLongOrZero(Plays.SYNC_TIMESTAMP),
                deleteTimestamp = cursor.getLongOrZero(Plays.DELETE_TIMESTAMP),
                updateTimestamp = cursor.getLongOrZero(Plays.UPDATE_TIMESTAMP),
                dirtyTimestamp = cursor.getLongOrZero(Plays.DIRTY_TIMESTAMP),
        )
    }

    private fun playerFromCursor(cursor: Cursor): Player {
        return Player(
                name = CursorUtils.getString(cursor, PlayPlayers.NAME),
                username = CursorUtils.getString(cursor, PlayPlayers.USER_NAME),
                color = CursorUtils.getString(cursor, PlayPlayers.COLOR),
                startingPosition = CursorUtils.getString(cursor, PlayPlayers.START_POSITION),
                score = CursorUtils.getString(cursor, PlayPlayers.SCORE),
                userId = CursorUtils.getInt(cursor, PlayPlayers.USER_ID),
                rating = CursorUtils.getDouble(cursor, PlayPlayers.RATING, Player.DEFAULT_RATING),
                isNew = CursorUtils.getBoolean(cursor, PlayPlayers.NEW),
                isWin = CursorUtils.getBoolean(cursor, PlayPlayers.WIN),
        )
    }

    fun addPlayers(cursor: Cursor, play: Play) {
        play.clearPlayers()
        while (cursor.moveToNext()) {
            play.addPlayer(playerFromCursor(cursor))
        }
        if (play.getPlayerCount() > 9 && !play.arePlayersCustomSorted()) {
            play.sortPlayers()
        }
    }

    fun rematch(play: Play): Play {
        val rematch = Play(
                gameId = play.gameId,
                gameName = play.gameName,
                location = play.location,
                noWinStats = play.noWinStats,
        )
        for (player in play.players) {
            val p = player.copy(score = "", rating = 0.0, isWin = false, isNew = false)
            if (play.arePlayersCustomSorted()) {
                p.startingPosition = ""
            }
            rematch.addPlayer(p)
        }
        return rematch
    }
}