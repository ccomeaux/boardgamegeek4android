package com.boardgamegeek.entities

data class CompanyEntity(
        val id: Int,
        val name: String,
        val description: String,
        override val imageUrl: String,
        override val thumbnailUrl: String,
        override var heroImageUrl: String,
        val updatedTimestamp: Long
) : ImagesEntity {
    override val imagesEntityDescription: String
        get() = id.toString()
}
