package com.boardgamegeek.entities

data class PlayerEntity(
        val name: String,
        val username: String,
        val playCount: Int = 0,
        val winCount: Int = 0,
        val avatarUrl: String? = null) {
    val id: String
        get() {
            return if (username.isBlank()) name else username
        }
    val description: String = if (username.isBlank()) name else "$name ($username)"
}
