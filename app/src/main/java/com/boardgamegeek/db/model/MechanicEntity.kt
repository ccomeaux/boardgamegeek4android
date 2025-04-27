package com.boardgamegeek.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "mechanics",
    indices = [Index("mechanic_id", name = "index_mechanics_mechanic_id", unique = true)]
)
data class MechanicEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val internalId: Long,
    @ColumnInfo(name = "mechanic_id")
    val mechanicId: Int,
    @ColumnInfo(name = "mechanic_name")
    val mechanicName: String,
)