package com.boardgamegeek.mappers

import com.boardgamegeek.db.model.MechanicEntity
import com.boardgamegeek.db.model.MechanicWithItemCount
import com.boardgamegeek.model.GameDetail
import com.boardgamegeek.model.Mechanic

fun MechanicWithItemCount.mapToModel() = Mechanic(
    id = mechanic.mechanicId,
    name = mechanic.mechanicName,
    itemCount = itemCount,
)

fun MechanicEntity.mapToGameDetail() = GameDetail(
    id = mechanicId,
    name = mechanicName,
)
