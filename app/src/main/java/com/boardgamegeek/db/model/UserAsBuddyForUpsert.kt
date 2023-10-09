package com.boardgamegeek.db.model

import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import java.util.Date

data class UserAsBuddyForUpsert(
    @PrimaryKey
    val username: String,
    @ColumnInfo(name = "buddy_flag")
    val buddyFlag: Boolean,
    @ColumnInfo(name = "updated_list_timestamp")
    val updatedListDate: Date,
)
