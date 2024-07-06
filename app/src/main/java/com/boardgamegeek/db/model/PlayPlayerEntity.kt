package com.boardgamegeek.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "play_players")
data class PlayPlayerEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val internalId: Long,
    @ColumnInfo(name = "_play_id")
    val internalPlayId: Long,
    @ColumnInfo(name = "user_name")
    val username: String?,
    @ColumnInfo(name = "user_id")
    val userId: Int?,
    @ColumnInfo
    val name: String?,
    @ColumnInfo("start_position")
    val startingPosition: String?,
    @ColumnInfo
    val color: String?,
    @ColumnInfo
    val score: String?,
    @ColumnInfo(name = "new")
    val isNew: Boolean?,
    @ColumnInfo
    val rating: Double?,
    @ColumnInfo(name = "win")
    val isWin: Boolean?,
)
