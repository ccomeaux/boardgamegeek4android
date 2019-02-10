package com.boardgamegeek.entities

data class GameForPlayStatEntity(
        val id: Int,
        val name: String,
        val playCount: Int,
        val bggRank: Int,
        val isOwned: Boolean = false
)