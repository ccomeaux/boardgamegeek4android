package com.boardgamegeek.db.model

import androidx.room.Embedded

data class ArtistWithItemCount (
    @Embedded
    val artist: ArtistEntity,
    val itemCount: Int,
)
