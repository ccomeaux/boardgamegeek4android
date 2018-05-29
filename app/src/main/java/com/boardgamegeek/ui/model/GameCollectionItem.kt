package com.boardgamegeek.ui.model

data class GameCollectionItem(
        val internalId: Long,
        val collectionId: Int,
        val collectionName: String,
        val gameName: String,
        val collectionYearPublished: Int,
        val yearPublished: Int,
        val imageUrl: String,
        val thumbnailUrl: String,
        val comment: String,
        val numberOfPlays: Int,
        val rating: Double,
        val syncTimestamp: Long,
        val statuses: List<String>
)