package com.boardgamegeek.db.model

import androidx.room.ColumnInfo

data class GameIdAndName(
    @ColumnInfo(name = "game_id")
    val gameId: Int,
    @ColumnInfo(name = "game_name")
    val gameName: String,
)
