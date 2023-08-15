package com.boardgamegeek.db.model

data class PublisherBasic(
    val publisherId: Int,
    val publisherName: String,
    val sortName: String,
    val publisherDescription: String?,
    val publisherImageUrl: String?,
    val publisherThumbnailUrl: String?,
)