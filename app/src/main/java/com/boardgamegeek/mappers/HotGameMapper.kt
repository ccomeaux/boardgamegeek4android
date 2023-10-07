package com.boardgamegeek.mappers

import com.boardgamegeek.entities.HotGame
import com.boardgamegeek.io.model.HotGameRemote

fun HotGameRemote.mapToModel() = HotGame(
    rank = this.rank,
    id = this.id,
    name = this.name,
    yearPublished = this.yearPublished,
    thumbnailUrl = this.thumbnailUrl.orEmpty()
)
