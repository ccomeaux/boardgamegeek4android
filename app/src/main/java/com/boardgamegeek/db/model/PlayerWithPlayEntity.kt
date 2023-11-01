package com.boardgamegeek.db.model

import androidx.room.*

data class PlayerWithPlayEntity(
    @Embedded
    val player: PlayPlayerEntity,
    val quantity: Int,
    val noWinStats: Boolean,
    val incomplete: Boolean,
    val avatarUrl: String?,
) {
    fun key(): String {
        return if (player.username.isNullOrBlank()) {
            "P|${player.name}"
        } else {
            "U|${player.username}"
        }
    }
}
