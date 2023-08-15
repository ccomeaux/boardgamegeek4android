package com.boardgamegeek.db.model

data class DesignerImages (
    val designerId: Int,
    val imageUrl: String?,
    val thumbnailUrl: String?,
    val updatedTimestamp: Long?,
)
