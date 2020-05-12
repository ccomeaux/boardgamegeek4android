package com.boardgamegeek.entities

data class HotGameEntity(
        val rank: Int = 0,
        val id: Int = 0,
        val name: String = "",
        val thumbnailUrl: String = "",
        val yearPublished: Int = YEAR_UNKNOWN)