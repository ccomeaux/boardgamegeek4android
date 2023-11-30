package com.boardgamegeek.db.model

import androidx.room.ColumnInfo
import java.util.Date

data class DesignerForUpsert (
    @ColumnInfo(name = "_id")
    val internalId: Long,
    @ColumnInfo(name = "designer_id")
    val designerId: Int,
    @ColumnInfo(name = "designer_name")
    val designerName: String,
    @ColumnInfo(name = "designer_description")
    val designerDescription: String?,
    @ColumnInfo(name = "updated")
    val updatedTimestamp: Date?,
)
