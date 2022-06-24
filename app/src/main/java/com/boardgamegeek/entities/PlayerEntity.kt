package com.boardgamegeek.entities

import java.util.*

data class PlayerEntity(
    val name: String,
    val username: String,
    val playCount: Int = 0,
    val winCount: Int = 0,
    val rawAvatarUrl: String = "",
) {

    val id: String
        get() = if (username.isBlank()) "P|$name" else "U|${username.lowercase(Locale.getDefault())}"

    val avatarUrl: String = rawAvatarUrl
        get() = if (field == "N/A") "" else field

    val description: String = if (isUser()) "$name ($username)" else name

    val playerName = if (isUser()) username else name

    var favoriteColor: Int? = null

    fun isUser() = username.isNotBlank()

    override fun hashCode(): Int {
        var hash = 3
        hash = 53 * hash + name.hashCode()
        hash = 53 * hash + username.hashCode()
        return hash
    }

    override fun equals(other: Any?): Boolean {
        val otherPlayerEntity = other as? PlayerEntity
        return when {
            otherPlayerEntity == null -> return false
            username.isBlank() -> {
                otherPlayerEntity.username.isBlank() && name == otherPlayerEntity.name
            }
            else -> username == otherPlayerEntity.username
        }
    }
}
