package com.boardgamegeek.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plays")
data class PlayEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val internalId: Long,
    @ColumnInfo(name = "updated_list")
    val syncTimestamp: Long,
    @ColumnInfo(name = "play_id")
    val playId: Int?,
    @ColumnInfo
    val date: String,
    @ColumnInfo(name = "quantity")
    val quantity: Int,
    @ColumnInfo
    val length: Int,
    @ColumnInfo
    val incomplete: Boolean,
    @ColumnInfo(name = "no_win_stats")
    val noWinStats: Boolean,
    @ColumnInfo
    val location: String?,
    @ColumnInfo
    val comments: String?,
    @ColumnInfo(name = "start_time")
    val startTime: Long?,
    @ColumnInfo(name = "player_count")
    val playerCount: Int?,
    @ColumnInfo(name = "sync_hash_code")
    val syncHashCode: Int?,
    @ColumnInfo(name = "item_name")
    val itemName: String,
    @ColumnInfo(name = "object_id")
    val objectId: Int,
    @ColumnInfo(name = "delete_timestamp")
    val deleteTimestamp: Long?,
    @ColumnInfo(name = "update_timestamp")
    val updateTimestamp: Long?,
    @ColumnInfo(name = "dirty_timestamp")
    val dirtyTimestamp: Long?,
)
