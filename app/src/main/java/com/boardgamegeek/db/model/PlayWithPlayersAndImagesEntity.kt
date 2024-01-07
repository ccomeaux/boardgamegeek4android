package com.boardgamegeek.db.model

import androidx.room.*

data class PlayWithPlayersAndImagesEntity(
    @Embedded
    val play: PlayEntity,
    val gameImageUrl: String?,
    val gameThumbnailUrl: String?,
    val gameHeroImageUrl: String?,
    @Relation(parentColumn = "_id", entityColumn = "_play_id")
    val players: List<PlayPlayerEntity>,
)
