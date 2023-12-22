package com.boardgamegeek.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "game_poll_language_results",
    primaryKeys = ["game_id", "level"],
    foreignKeys = [
        ForeignKey(GameEntity::class, ["game_id"], ["game_id"], ForeignKey.CASCADE)
    ],
)
data class GameLanguagePollResultEntity(
    @ColumnInfo(name = "game_id", index = true)
    val gameId: Int,
    @ColumnInfo
    val level: Int,
    @ColumnInfo
    val votes: Int,
)
