package com.boardgamegeek.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "game_polls",
    foreignKeys = [
        ForeignKey(GameEntity::class, ["game_id"], ["game_id"], ForeignKey.CASCADE)
    ],
)
data class GamePollEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val internalId: Long,
    @ColumnInfo(name = "game_id") // TODO: add index
    val gameId: Int,
    @ColumnInfo(name = "poll_name")
    val pollName: String,
    @ColumnInfo(name = "poll_title")
    val pollTitle: String,
    @ColumnInfo(name = "poll_total_votes")
    val pollTotalVotes: Int,
)