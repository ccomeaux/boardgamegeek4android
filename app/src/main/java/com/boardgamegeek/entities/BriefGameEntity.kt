package com.boardgamegeek.entities

data class BriefGameEntity(
        val gameId: Int,
        val gameName: String,
        val collectionName: String,
        val yearPublished: Int,
        val collectionThumbnailUrl: String,
        val gameThumbnailUrl: String,
        val gameHeroImageUrl: String,
        val personalRating: Double = 0.0,
        val isFavorite: Boolean = false,
        val subtype: String = "boardgame"
)