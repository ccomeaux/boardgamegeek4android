package com.boardgamegeek.entities

import com.boardgamegeek.util.PresentationUtils

data class PlayerEntity(
        val id: Int,
        val name: String,
        val username: String,
        val playCount: Int = 0,
        val winCount: Int = 0) {
    val description: String = PresentationUtils.describePlayer(name, username)
}