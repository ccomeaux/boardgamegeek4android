package com.boardgamegeek.db.model

import androidx.room.Embedded

data class PublisherWithItemCount (
    @Embedded
    val publisher: PublisherEntity,
    val itemCount: Int,
)
