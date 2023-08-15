package com.boardgamegeek.db.model

data class ArtistImages (
    val artistId: Int,
    val imageUrl: String?,
    val thumbnailUrl: String?,
    val updatedTimestamp: Long?,
)
