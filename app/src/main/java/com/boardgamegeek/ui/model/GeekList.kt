package com.boardgamegeek.ui.model

data class GeekList(
        val id: Int,
        val title: String,
        val username: String,
        val description: String,
        val numberOfItems: Int,
        val numberOfThumbs: Int,
        val postTicks: Long,
        val EditTicks: Long
)
