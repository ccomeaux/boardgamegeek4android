package com.boardgamegeek.entities

data class PersonImagesEntity(
        val id: Int,
        override val imageUrl: String,
        override val thumbnailUrl: String,
        override var heroImageUrl: String,
        val updatedTimestamp: Long
) : ImagesEntity {
    override val imagesEntityDescription: String
        get() = id.toString()
}