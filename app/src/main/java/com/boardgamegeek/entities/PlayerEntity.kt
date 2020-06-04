package com.boardgamegeek.entities

import java.util.*

data class PlayerEntity(
        val name: String,
        val username: String,
        val playCount: Int = 0,
        val winCount: Int = 0) {

    val id: String
        get() = if (username.isBlank()) "P|$name" else "U|${username.toLowerCase(Locale.getDefault())}"

    val description: String = if (username.isBlank()) name else "$name ($username)"
}
