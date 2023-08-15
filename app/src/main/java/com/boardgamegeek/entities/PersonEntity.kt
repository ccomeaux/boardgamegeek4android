package com.boardgamegeek.entities

data class PersonEntity(
    val internalId: Int,
    val id: Int,
    val name: String,
    val description: String,
    val updatedTimestamp: Long,
    val thumbnailUrl: String = "",
    val heroImageUrl: String = "",
    val imageUrl: String = "",
    val itemCount: Int = 0,
    val whitmoreScore: Int = 0,
    val statsUpdatedTimestamp: Long = 0L,
    val imagesUpdatedTimestamp: Long = 0L,
) {
    val heroImageUrls = listOf(heroImageUrl, thumbnailUrl, imageUrl)
}
