package com.boardgamegeek.entities

data class PersonGameEntity(
        val gameId: Int,
        val gameName: String,
        val collectionName: String,
        val yearPublished: Int,
        val collectionThumbnailUrl: String,
        val gameThumbnailUrl: String,
        val gameHeroImageUrl: String
)