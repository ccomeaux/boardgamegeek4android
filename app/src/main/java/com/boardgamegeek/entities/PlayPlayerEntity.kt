package com.boardgamegeek.entities

data class PlayPlayerEntity(
        val name: String,
        val username: String) {
    val id: String
        get() {
            return if (username.isBlank()) name else username
        }
    val description: String = if (username.isBlank()) name else "$name ($username)"
}
