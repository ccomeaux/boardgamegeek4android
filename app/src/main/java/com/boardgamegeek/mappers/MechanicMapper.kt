package com.boardgamegeek.mappers

import com.boardgamegeek.db.model.MechanicEntity
import com.boardgamegeek.model.GameDetail
import com.boardgamegeek.model.Mechanic

fun MechanicEntity.mapToModel() = Mechanic(
    id = mechanicId,
    name = mechanicName,
)

fun MechanicEntity.mapToGameDetail() = GameDetail(
    id = mechanicId,
    name = mechanicName,
)
