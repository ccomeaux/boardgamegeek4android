package com.boardgamegeek.events

data class GameInfoChangedEvent(
        val gameName: String,
        val subtype: String,
        val imageUrl: String,
        val thumbnailUrl: String,
        val heroImageUrl: String,
        val arePlayersCustomSorted: Boolean,
        val isFavorite: Boolean
)

