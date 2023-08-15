package com.boardgamegeek.db.model

data class PublisherLocal(
    val internalId: Int,
    val updatedTimestamp: Long,
    val publisherId: Int, // unique
    val publisherName: String,
    val publisherDescription: String?,
    val publisherImageUrl: String?,
    val publisherThumbnailUrl: String?,
    val publisherHeroImageUrl: String?,
    val whitmoreScore: Int?,
    val statsUpdatedTimestamp: Long?,
    val itemCount: Int?, // ignore
)