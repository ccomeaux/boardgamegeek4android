package com.boardgamegeek.db.model

data class ArtistLocal(
    val internalId: Int,
    val updatedTimestamp: Long?,
    val artistId: Int, // unique
    val artistName: String,
    val artistDescription: String?,
    val artistImageUrl: String?,
    val artistThumbnailUrl: String?,
    val artistHeroImageUrl: String?,
    val imagesUpdatedTimestamp: Long?,
    val whitmoreScore: Int?,
    val statsUpdatedTimestamp: Long?,
    val itemCount: Int?, // ignore
)