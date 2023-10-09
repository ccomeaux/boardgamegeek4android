package com.boardgamegeek.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val username: String,
    @ColumnInfo(name = "first_name")
    val firstName: String?,
    @ColumnInfo(name = "last_name")
    val lastName: String?,
    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String?,
    @ColumnInfo(name = "play_nickname")
    val playNickname: String?,
    @ColumnInfo(name = "buddy_flag")
    val buddyFlag: Boolean?,
    @ColumnInfo(name = "sync_hash_code")
    val syncHashCode: Int?,
    @ColumnInfo(name = "updated_list_timestamp")
    val updatedListDate: Date?,
    @ColumnInfo(name = "updated_detail_timestamp")
    val updatedDetailDate: Date?,
)
