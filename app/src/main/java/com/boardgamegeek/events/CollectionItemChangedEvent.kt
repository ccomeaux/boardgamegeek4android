package com.boardgamegeek.events

data class CollectionItemChangedEvent(
        val collectionName: String,
        val thumbnailUrl: String,
        val heroImageUrl: String,
        val yearPublished: Int
)
