package com.boardgamegeek.model

import androidx.annotation.ColorInt
import java.util.Locale

/**
 * Represents a player in the system, which can be either a registered user or a non-user.
 *
 * @property name The display name (nickname) of the player.
 * @property username The unique username of the player if it's a registered user; empty for non-user players.
 * @property playCount The total number of plays this player has participated in.
 * @property winCount The total number of plays this player has won.
 * @property userAvatarUrl The URL of the user's avatar image. Null for non-user players.
 * @property userFullName The full name of the user. Null for non-user players.
 * @property userUpdatedTimestamp The timestamp (in milliseconds since epoch) indicating when the user's information was last updated. Null for non-user players.
 */
data class Player(
    val name: String,
    val username: String,
    val playCount: Int = 0,
    val winCount: Int = 0,
    val userAvatarUrl: String? = null,
    val userFullName: String? = null,
    val userUpdatedTimestamp: Long? = null,
) {
    /**
     * A unique identifier for the player that is valid for user and non-user players.
     */
    val id: String
        get() = if (username.isBlank()) "P|$name" else "U|${username.lowercase(Locale.getDefault())}"

    /**
     * A user-friendly description of the player.
     */
    val description: String = if (isUser()) "$name ($username)" else name

    /**
     * The player's favorite color, or null if not known.
     */
    @ColorInt
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

        fun List<Player>.applySort(sortBy: SortType): List<Player> {
            return sortedWith(
                when (sortBy) {
                    SortType.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                    SortType.PLAY_COUNT -> compareByDescending<Player> { it.playCount }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                    SortType.WIN_COUNT -> compareByDescending<Player> { it.winCount }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                }
            )
        }
    }
}
