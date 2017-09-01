package com.boardgamegeek.events

class GameInfoChangedEvent(
        val gameName: String,
        val subtype: String,
        val imageUrl: String,
        val thumbnailUrl: String,
        val arePlayersCustomSorted: Boolean,
        val isFavorite: Boolean)

