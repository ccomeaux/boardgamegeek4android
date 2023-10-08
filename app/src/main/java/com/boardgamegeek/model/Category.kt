package com.boardgamegeek.model

data class Category(
    val id: Int,
    val name: String,
    val itemCount: Int = 0,
)
