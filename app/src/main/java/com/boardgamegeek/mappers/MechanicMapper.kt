package com.boardgamegeek.mappers

import com.boardgamegeek.db.model.MechanicEntity
import com.boardgamegeek.db.model.MechanicLocal
import com.boardgamegeek.model.GameDetail
import com.boardgamegeek.model.Mechanic

fun MechanicEntity.mapToModel() = Mechanic(
    id = mechanicId,
    name = mechanicName,
)

fun MechanicLocal.mapToGameDetail() = GameDetail(
    id = id,
    name = name,
)
