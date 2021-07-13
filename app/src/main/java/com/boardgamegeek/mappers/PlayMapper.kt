package com.boardgamegeek.mappers

import com.boardgamegeek.entities.PlayEntity
import com.boardgamegeek.entities.PlayPlayerEntity
import com.boardgamegeek.io.model.Play
import com.boardgamegeek.io.model.Player
import com.boardgamegeek.provider.BggContract

fun List<Play>?.mapToEntity(syncTimestamp: Long = System.currentTimeMillis()): List<PlayEntity> {
    return this?.map { it.mapToEntity(syncTimestamp) }.orEmpty()
}

private fun Play.mapToEntity(syncTimestamp: Long): PlayEntity {
    val play = PlayEntity(
        internalId = BggContract.INVALID_ID.toLong(),
        playId = id,
        rawDate = date,
        gameId = objectid,
        gameName = name,
        location = location,
        quantity = quantity,
        length = length,
        incomplete = incomplete == 1,
        noWinStats = nowinstats == 1,
        comments = comments.orEmpty(),
        syncTimestamp = syncTimestamp,
        initialPlayerCount = players?.size ?: 0,
        subtypes = subtypes.map { it.value }
    )
    players?.forEach {
        play.addPlayer(it.mapToEntity())
    }
    return play
}

private fun Player.mapToEntity(): PlayPlayerEntity {
    return PlayPlayerEntity(
        username = username,
        name = name,
        startingPosition = startposition,
        color = color,
        score = score,
        rating = rating,
        userId = userid,
        isNew = new_ == 1,
        isWin = win == 1,
    )
}
