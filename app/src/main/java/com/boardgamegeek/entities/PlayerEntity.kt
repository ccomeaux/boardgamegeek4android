package com.boardgamegeek.entities

data class PlayerEntity(
        val id: Long,
        val name: String,
        val username: String,
        val playCount: Int = 0,
        val winCount: Int = 0) {
    val description: String = if (username.isBlank()) name else "$name ($username)"
}