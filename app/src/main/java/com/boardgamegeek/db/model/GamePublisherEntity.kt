package com.boardgamegeek.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games_publishers")
data class GamePublisherEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val internalId: Long,
    @ColumnInfo(name = "game_id", index = true)
    val gameId: Int,
    @ColumnInfo(name = "publisher_id", index = true)
    val publisherId: Int,
)