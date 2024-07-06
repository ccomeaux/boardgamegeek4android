package com.boardgamegeek.db.model

import androidx.room.ColumnInfo
import java.util.Date

data class UserForUpsert(
    val username: String,
    @ColumnInfo(name = "first_name")
    val firstName: String?,
    @ColumnInfo(name = "last_name")
    val lastName: String?,
    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String?,
    @ColumnInfo(name = "sync_hash_code")
    val syncHashCode: Int?,
    @ColumnInfo(name = "updated_detail_timestamp")
    val updatedDetailTimestamp: Date,
)
