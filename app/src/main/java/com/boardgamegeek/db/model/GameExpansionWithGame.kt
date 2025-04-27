package com.boardgamegeek.db.model

import androidx.room.Embedded
import androidx.room.Entity

@Entity(tableName = "games_expansions")
data class GameExpansionWithGame(
    val thumbnailUrl: String?,
    @Embedded
    val gameExpansionEntity: GameExpansionEntity,
)