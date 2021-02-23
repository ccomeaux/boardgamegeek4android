package com.boardgamegeek.mappers

import com.boardgamegeek.entities.PlayEntity
import com.boardgamegeek.entities.PlayPlayerEntity
import com.boardgamegeek.io.model.Play
import com.boardgamegeek.io.model.Player
import com.boardgamegeek.provider.BggContract
import java.text.SimpleDateFormat
import java.util.*

class PlayMapper {
    companion object {
        val FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }

    fun map(from: List<Play>?, syncTimestamp: Long = System.currentTimeMillis()): List<PlayEntity> {
        val plays = mutableListOf<PlayEntity>()
        from?.forEach {
            plays += map(it, syncTimestamp)
        }
        return plays
    }

    private fun map(from: Play, syncTimestamp: Long): PlayEntity {
        val play = PlayEntity(
                internalId = BggContract.INVALID_ID.toLong(),
                playId = from.id,
                rawDate = from.date,
                gameId = from.objectid,
                gameName = from.name,
                location = from.location,
                quantity = from.quantity,
                length = from.length,
                incomplete = from.incomplete == 1,
                noWinStats = from.nowinstats == 1,
                comments = from.comments.orEmpty(),
                syncTimestamp = syncTimestamp,
                initialPlayerCount = from.players.size,
                subtypes = from.subtypes.map { it.value }
        )
        from.players?.forEach {
            play.addPlayer(map(it))
        }
        return play
    }

    private fun map(from: Player): PlayPlayerEntity {
        return PlayPlayerEntity(
                username = from.username,
                name = from.name,
                startingPosition = from.startposition,
                color = from.color,
                score = from.score,
                rating = from.rating,
                userId = from.userid.toString(),
                isNew = from.new_ == 1,
                isWin = from.win == 1,
        )
    }
}