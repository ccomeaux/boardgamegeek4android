package com.boardgamegeek.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "game_colors",
    foreignKeys = [
        ForeignKey(GameEntity::class, ["game_id"], ["game_id"], ForeignKey.CASCADE)
    ],
)
data class GameColorsEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val internalId: Long,
    @ColumnInfo(name = "game_id") // TODO create index
    val gameId: Int,
    @ColumnInfo
    val color: String,
)
