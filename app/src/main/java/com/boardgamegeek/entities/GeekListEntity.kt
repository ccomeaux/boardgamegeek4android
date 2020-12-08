package com.boardgamegeek.entities

data class GeekListEntity(
        val id: Int,
        val title: String,
        val username: String,
        val description: String = "",
        val numberOfItems: Int = 0,
        val numberOfThumbs: Int = 0,
        val postTicks: Long = 0L,
        val editTicks: Long = 0L,
        val items: List<GeekListItemEntity> = emptyList(),
        val comments: List<GeekListCommentEntity> = emptyList()
)
