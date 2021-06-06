package com.boardgamegeek.model.builder

import android.database.Cursor
import androidx.core.database.getDoubleOrNull
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.boardgamegeek.extensions.getBoolean
import com.boardgamegeek.extensions.toMillis
import com.boardgamegeek.model.Play
import com.boardgamegeek.model.Player
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.PlayPlayers
import com.boardgamegeek.provider.BggContract.Plays

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
            PlayPlayers.NAME,
            PlayPlayers.USER_NAME,
            PlayPlayers.START_POSITION,
            PlayPlayers.COLOR,
            PlayPlayers.SCORE,
            PlayPlayers.RATING,
            PlayPlayers.NEW,
            PlayPlayers.WIN
    )

    fun fromCursor(cursor: Cursor): Play {
        return Play(
                gameId = cursor.getIntOrNull(2) ?: BggContract.INVALID_ID,
                gameName = cursor.getStringOrNull(1).orEmpty(),
                dateInMillis = cursor.getStringOrNull(3).toMillis(Play.FORMAT_DATABASE),
                quantity = cursor.getIntOrNull(6) ?: Play.QUANTITY_DEFAULT,
                length = cursor.getIntOrNull(5) ?: Play.LENGTH_DEFAULT,
                location = cursor.getStringOrNull(4),
                comments = cursor.getStringOrNull(9),
                playId = cursor.getIntOrNull(0) ?: BggContract.INVALID_ID,
                incomplete = cursor.getBoolean(7),
                noWinStats = cursor.getBoolean(8),
                startTime = cursor.getLongOrNull(11) ?: 0L,
                syncTimestamp = cursor.getLongOrNull(10) ?: 0L,
                deleteTimestamp = cursor.getLongOrNull(12) ?: 0L,
                updateTimestamp = cursor.getLongOrNull(13) ?: 0L,
                dirtyTimestamp = cursor.getLongOrNull(14) ?: 0L,
        )
    }

    private fun playerFromCursor(cursor: Cursor): Player {
        return Player(
                userId = cursor.getIntOrNull(0) ?: BggContract.INVALID_ID,
                name = cursor.getStringOrNull(1).orEmpty(),
                username = cursor.getStringOrNull(2).orEmpty(),
                startingPosition = cursor.getStringOrNull(3).orEmpty(),
                color = cursor.getStringOrNull(4).orEmpty(),
                score = cursor.getStringOrNull(5).orEmpty(),
                rating = cursor.getDoubleOrNull(6) ?: Player.DEFAULT_RATING,
                isNew = cursor.getBoolean(7),
                isWin = cursor.getBoolean(8),
        )
    }

    fun addPlayers(cursor: Cursor, play: Play) {
        play.players.clear()
        while (cursor.moveToNext()) {
            play.addPlayer(playerFromCursor(cursor))
        }
        if (play.getPlayerCount() > 9 && !play.arePlayersCustomSorted()) {
            play.players.sortBy { player -> player.seat }
        }
    }
}
