package com.boardgamegeek.model

data class GameDetail(
    val id: Int,
    val name: String,
    val description: String = "",
    val thumbnailUrl: String = "",
)
