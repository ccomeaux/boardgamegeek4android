package com.boardgamegeek.db.model

import androidx.room.ColumnInfo

data class DesignerBriefForUpsert (
    @ColumnInfo(name = "_id")
    val internalId: Int,
    @ColumnInfo(name = "designer_id")
    val designerId: Int,
    @ColumnInfo(name = "designer_name")
    val designerName: String,
)
