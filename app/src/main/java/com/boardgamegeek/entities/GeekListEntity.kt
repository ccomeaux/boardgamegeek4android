package com.boardgamegeek.entities

data class GeekListEntity(
        val id: Int,
        val title: String,
        val username: String,
        val description: String,
        val numberOfItems: Int,
        val numberOfThumbs: Int,
        val postTicks: Long,
        val editTicks: Long,
        val items: List<GeekListItemEntity>
)
