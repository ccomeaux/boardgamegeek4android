package com.boardgamegeek.entities

data class ArtistImagesEntity(
        override val id: Int,
        val imageUrl: String,
        override val thumbnailUrl: String,
        override var heroImageUrl: String,
        val updatedTimestamp: Long
) : ImagesEntity