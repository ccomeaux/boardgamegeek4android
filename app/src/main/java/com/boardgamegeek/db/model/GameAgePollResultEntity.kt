package com.boardgamegeek.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "game_poll_age_results",
    primaryKeys = ["game_id", "value"],
    foreignKeys = [
        ForeignKey(GameEntity::class, ["game_id"], ["game_id"], ForeignKey.CASCADE)
    ],
)
data class GameAgePollResultEntity(
    @ColumnInfo(name = "game_id", index = true)
    val gameId: Int,
    @ColumnInfo
    val value: Int,
    @ColumnInfo
    val votes: Int,
)
