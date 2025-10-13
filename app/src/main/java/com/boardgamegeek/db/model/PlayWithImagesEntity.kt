package com.boardgamegeek.db.model

import androidx.room.*

data class PlayWithImagesEntity(
    @Embedded
    val play: PlayEntity,
    val gameImageUrl: String?,
    val gameThumbnailUrl: String?,
    val gameHeroImageUrl: String?,
)
