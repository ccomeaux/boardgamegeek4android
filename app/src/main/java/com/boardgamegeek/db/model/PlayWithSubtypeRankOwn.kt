package com.boardgamegeek.db.model

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class PlayWithSubtypeRankOwn(
    @Embedded
    val play: PlayEntity,
    val subtype: String?,
    @ColumnInfo(name = "game_rank")
    val rank: Int?,
    @ColumnInfo(name = "owned")
    val own: Int?,
)
