package com.boardgamegeek.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mechanics")
data class MechanicEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val internalId: Int,
    @ColumnInfo(name = "mechanic_id")
    val mechanicId: Int, // unique
    @ColumnInfo(name = "mechanic_name")
    val mechanicName: String,
)