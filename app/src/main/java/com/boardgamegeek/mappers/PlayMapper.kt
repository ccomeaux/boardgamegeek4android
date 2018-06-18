package com.boardgamegeek.mappers

import com.boardgamegeek.io.model.Play
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
            nowinstats = from.nowinstats == 1
            subtypes = from.subtypes.map { it.value }
        }
    }
}