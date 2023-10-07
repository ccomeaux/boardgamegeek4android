package com.boardgamegeek.entities

data class GeekList(
    val id: Int,
    val title: String,
    val username: String,
    val description: String = "",
    val numberOfItems: Int = 0,
    val numberOfThumbs: Int = 0,
    val postTicks: Long = 0L,
    val editTicks: Long = 0L,
    val items: List<GeekListItem> = emptyList(),
    val comments: List<GeekListComment> = emptyList()
)
