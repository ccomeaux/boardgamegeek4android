package com.boardgamegeek.db.model

data class DesignerBasic (
    val designerId: Int,
    val designerName: String,
    val designerDescription: String?,
    val updatedTimestamp: Long?,
)
