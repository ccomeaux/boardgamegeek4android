package com.boardgamegeek.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games_expansions")
data class GameExpansionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val internalId: Long,
    @ColumnInfo(name = "game_id")
    val gameId: Int,
    @ColumnInfo(name = "expansion_id")
    val expansionId: Int,
    @ColumnInfo(name = "expansion_name")
    val expansionName: String,
    @ColumnInfo
    val inbound: Boolean?,
)