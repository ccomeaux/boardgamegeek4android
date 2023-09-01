package com.boardgamegeek.mappers

import com.boardgamegeek.db.model.MechanicLocal
import com.boardgamegeek.entities.GameDetailEntity
import com.boardgamegeek.entities.MechanicEntity

fun MechanicLocal.mapToEntity() = MechanicEntity(
    id = id,
    name = name,
    itemCount = itemCount,
)

fun MechanicLocal.mapToGameDetailEntity() = GameDetailEntity(
    id = id,
    name = name,
)
