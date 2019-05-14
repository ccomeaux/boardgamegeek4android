package com.boardgamegeek.entities

data class CategoryEntity(
        val id: Int,
        val name: String,
        val itemCount: Int = 0
)