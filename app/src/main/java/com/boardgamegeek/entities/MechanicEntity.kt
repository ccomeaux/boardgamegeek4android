package com.boardgamegeek.entities

data class MechanicEntity(
        val id: Int,
        val name: String,
        val itemCount: Int = 0,
)
