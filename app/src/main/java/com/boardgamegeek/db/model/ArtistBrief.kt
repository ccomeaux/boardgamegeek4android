package com.boardgamegeek.db.model

data class ArtistBrief (
    val internalId: Long,
    val artistId: Int,
    val artistName: String,
    val artistThumbnailUrl: String?,
)
