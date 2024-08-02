package com.boardgamegeek.db.model

import androidx.room.*
import java.util.Date

data class PlayerWithPlayEntity(
    @Embedded
    val player: PlayPlayerEntity,
    val quantity: Int,
    val noWinStats: Boolean,
    val incomplete: Boolean,
    val avatarUrl: String?,
    val firstName: String?,
    val lastName: String?,
    val userUpdatedTimestamp: Date?,
) {
    fun fullName(): String {
        return firstName?.let { fn ->
            lastName?.let { ln ->
                "$fn $ln".trim()
            } ?: fn.trim()
        } ?: ""
    }

    fun key(): String {
        return if (player.username.isNullOrBlank()) {
            "P|${player.name}"
        } else {
            "U|${player.username}"
        }
    }
}
