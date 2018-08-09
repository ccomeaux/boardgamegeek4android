package com.boardgamegeek.entities

data class GameDetailEntity(
        val id: Int,
        val name: String,
        val description: String = ""
)
