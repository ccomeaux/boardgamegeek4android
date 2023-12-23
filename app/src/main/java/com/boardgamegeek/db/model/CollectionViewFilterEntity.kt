package com.boardgamegeek.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "collection_filters_details",
    foreignKeys = [
        ForeignKey(CollectionViewEntity::class, ["_id"], ["filter_id"], ForeignKey.CASCADE)
    ],
)
data class CollectionViewFilterEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val id: Int,
    @ColumnInfo(name = "filter_id", index = true) // TODO index!
    val viewId: Int,
    @ColumnInfo
    val type: Int?,
    @ColumnInfo
    val data: String?,
)
