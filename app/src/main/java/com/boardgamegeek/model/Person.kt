package com.boardgamegeek.model

import java.util.Date

data class Person(
    val internalId: Long,
    val id: Int,
    val name: String,
    val description: String,
    val updatedTimestamp: Date?,
    val thumbnailUrl: String = "",
    val heroImageUrl: String = "",
    val imageUrl: String = "",
    val itemCount: Int = 0,
    val whitmoreScore: Int = 0,
    val statsUpdatedTimestamp: Date? = null,
    val imagesUpdatedTimestamp: Date? = null,
) {
    val heroImageUrls = listOf(heroImageUrl, thumbnailUrl, imageUrl)
}
