package com.boardgamegeek.db.model

data class DesignerBrief(
    val internalId: Long,
    val designerId: Int,
    val designerName: String,
    val designerThumbnailUrl: String?,
)
