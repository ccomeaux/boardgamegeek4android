package com.boardgamegeek.events

data class CollectionItemChangedEvent(
        val collectionName: String,
        val imageUrl: String,
        val thumbnailUrl: String,
        val yearPublished: Int
)
