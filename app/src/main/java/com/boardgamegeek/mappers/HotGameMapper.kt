package com.boardgamegeek.mappers

import com.boardgamegeek.entities.HotGameEntity
import com.boardgamegeek.io.model.HotGame
import com.boardgamegeek.io.model.HotnessResponse

fun HotGame.mapToEntity() = HotGameEntity(
        rank = this.rank,
        id = this.id,
        name = this.name,
        yearPublished = this.yearPublished,
        thumbnailUrl = this.thumbnailUrl.orEmpty()
)

fun HotnessResponse.mapToEntity() = this.games.map { it.mapToEntity() }
