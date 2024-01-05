package com.boardgamegeek.db.model

import androidx.room.Embedded

data class MechanicWithItemCount (
    @Embedded
    val mechanic: MechanicEntity,
    val itemCount: Int,
)
