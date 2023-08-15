package com.boardgamegeek.db.model

data class DesignerLocal(
    val internalId: Int,
    val updatedTimestamp: Long,
    val designerId: Int, // unique
    val designerName: String,
    val designerDescription: String?,
    val designerImageUrl: String?,
    val designerThumbnailUrl: String?,
    val designerHeroImageUrl: String?,
    val imagesUpdatedTimestamp: Long?,
    val whitmoreScore: Int?,
    val statsUpdatedTimestamp: Long?,
    val itemCount: Int?, // ignore
)