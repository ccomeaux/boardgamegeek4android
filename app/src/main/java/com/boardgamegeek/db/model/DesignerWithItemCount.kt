package com.boardgamegeek.db.model

import androidx.room.Embedded

data class DesignerWithItemCount (
    @Embedded
    val designer: DesignerEntity,
    val itemCount: Int,
)
