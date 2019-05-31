package com.boardgamegeek.entities

data class CompanyEntity(
        val id: Int,
        val name: String,
        val description: String,
        override val imageUrl: String,
        override val thumbnailUrl: String,
        override var heroImageUrl: String,
        val updatedTimestamp: Long,
        val itemCount: Int = 0,
        val whitmoreScore: Int = 0
) : ImagesEntity {
    override val imagesEntityDescription: String
        get() = id.toString()
}
