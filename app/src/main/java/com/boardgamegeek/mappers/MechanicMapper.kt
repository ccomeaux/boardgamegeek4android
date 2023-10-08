package com.boardgamegeek.mappers

import com.boardgamegeek.db.model.MechanicLocal
import com.boardgamegeek.entities.GameDetail
import com.boardgamegeek.entities.Mechanic

fun MechanicLocal.mapToModel() = Mechanic(
    id = id,
    name = name,
    itemCount = itemCount,
)

fun MechanicLocal.mapToGameDetail() = GameDetail(
    id = id,
    name = name,
)
