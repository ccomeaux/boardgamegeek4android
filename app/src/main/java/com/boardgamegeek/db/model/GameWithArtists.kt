package com.boardgamegeek.db.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class GameWithArtists(
    @Embedded val game: GameEntity,
    @Relation(
        parentColumn = "game_id",
        entityColumn = "artist_id",
        associateBy = Junction(GameArtistEntity::class)
    )
    val artists: List<ArtistEntity>
)