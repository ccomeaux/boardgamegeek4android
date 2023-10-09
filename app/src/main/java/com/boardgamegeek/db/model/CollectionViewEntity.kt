package com.boardgamegeek.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "collection_filters")
data class CollectionViewEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val id: Int,
    @ColumnInfo
    val name: String?,
    @ColumnInfo(name = "starred")
    val starred: Boolean?,
    @ColumnInfo(name = "sort_type")
    val sortType: Int?,
    @ColumnInfo(name = "selected_count")
    val selectedCount: Int?,
    @ColumnInfo(name = "selected_timestamp")
    val selectedTimestamp: Long?,
)
