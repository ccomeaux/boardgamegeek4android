package com.boardgamegeek.mappers

import com.boardgamegeek.io.model.Play
import com.boardgamegeek.model.Player
import com.boardgamegeek.toMillis
import java.text.SimpleDateFormat
import java.util.*

class PlayMapper {
    companion object {
        val FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }

    fun map(from: List<Play>?): List<com.boardgamegeek.model.Play> {
        val plays = mutableListOf<com.boardgamegeek.model.Play>()
        from?.forEach {
            plays += map(it)
        }
        return plays
    }

    fun map(from: Play): com.boardgamegeek.model.Play {
        val p = mutableListOf<Player>()
        from.players?.forEach {
            p += map(it)
        }

        return com.boardgamegeek.model.Play().apply {
            playId = from.id
            dateInMillis = from.date.toMillis(FORMAT)
            gameId = from.objectid
            gameName = from.name
            location = from.location
            quantity = from.quantity
            length = from.length
            comments = from.comments
            incomplete = from.incomplete == 1
            noWinStats = from.nowinstats == 1
            subtypes = from.subtypes.map { it.value }
            players = p
        }
    }

    fun map(from: com.boardgamegeek.io.model.Player): Player {
        return Player().apply {
            userId = from.userid
            username = from.username
            name = from.name
            startingPosition = from.startposition
            color = from.color
            score = from.score
            isNew = from.new_ == 1
            rating = from.rating
            isWin = from.win == 1
        }
    }
}