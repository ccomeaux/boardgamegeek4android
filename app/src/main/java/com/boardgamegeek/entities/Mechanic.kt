package com.boardgamegeek.entities

data class Mechanic(
    val id: Int,
    val name: String,
    val itemCount: Int = 0,
)
