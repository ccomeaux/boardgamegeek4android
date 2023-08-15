package com.boardgamegeek.db.model

data class ArtistBasic (
    val artistId: Int,
    val artistName: String,
    val artistDescription: String?,
    val updatedTimestamp: Long?,
)
