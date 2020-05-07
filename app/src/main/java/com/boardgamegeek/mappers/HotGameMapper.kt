package com.boardgamegeek.mappers

import com.boardgamegeek.entities.HotGameEntity
import com.boardgamegeek.io.model.HotGame

class HotGameMapper {
    fun map(from: HotGame): HotGameEntity {
        return HotGameEntity(
                rank = from.rank,
                id = from.id,
                name = from.name,
                yearPublished = from.yearPublished,
                thumbnailUrl = from.thumbnailUrl.orEmpty()
        )
    }
}