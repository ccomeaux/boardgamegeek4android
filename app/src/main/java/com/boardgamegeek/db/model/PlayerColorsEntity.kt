package com.boardgamegeek.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_colors")
data class PlayerColorsEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val internalId: Int,
    @ColumnInfo("player_type")
    val playerType: Int,
    @ColumnInfo("player_name")
    val playerName: String,
    @ColumnInfo("player_color")
    val playerColor: String,
    @ColumnInfo("player_color_sort")
    val playerColorSortOrder: Int,
)
