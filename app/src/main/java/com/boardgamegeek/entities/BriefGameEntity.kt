package com.boardgamegeek.entities

data class BriefGameEntity(
        val internalId: Long,
        val gameId: Int,
        val gameName: String,
        val collectionName: String,
        val yearPublished: Int,
        val collectionYearPublished: Int,
        val collectionThumbnailUrl: String,
        val gameThumbnailUrl: String,
        val gameHeroImageUrl: String,
        val personalRating: Double = 0.0,
        val isFavorite: Boolean = false,
        val subtype: String = "boardgame",
        val playCount: Int = 0
) {
    val name = if (collectionName.isBlank()) gameName else collectionName
    val thumbnailUrl = if (collectionThumbnailUrl.isBlank()) gameThumbnailUrl else collectionThumbnailUrl
    val year = if (collectionYearPublished == YEAR_UNKNOWN) yearPublished else collectionYearPublished
}
