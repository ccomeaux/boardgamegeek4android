package com.boardgamegeek.entities

data class PlayerDetailEntity(
    val id: Long,
    val name: String,
    val username: String,
    val color: String
) {
    val description: String = if (username.isBlank()) name else "$name ($username)"
}
