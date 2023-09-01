package com.boardgamegeek.db.model

data class PublisherBrief(
    val internalId: Long,
    val publisherId: Int,
    val publisherName: String,
    val publisherThumbnailUrl: String?,
)