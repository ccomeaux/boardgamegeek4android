package com.boardgamegeek.model

import java.util.*

data class Player(
    val name: String,
    val username: String,
    val playCount: Int = 0,
    val winCount: Int = 0,
    val avatarUrl: String = "",
) {
    val id: String
        get() = if (username.isBlank()) "P|$name" else "U|${username.lowercase(Locale.getDefault())}"

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
        val otherPlayer = other as? Player
        return when {
            otherPlayer == null -> return false
            username.isBlank() -> {
                otherPlayer.username.isBlank() && name == otherPlayer.name
            }
            else -> username == otherPlayer.username
        }
    }

    enum class SortType {
        NAME, PLAY_COUNT, WIN_COUNT
    }

    companion object {
        fun createUser(name: String) = Player(name = "", username = name)
        fun createNonUser(name: String) = Player(name = name, username = "")
    }
}
