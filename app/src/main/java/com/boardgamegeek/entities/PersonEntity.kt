package com.boardgamegeek.entities

data class PersonEntity(
        val id: Int,
        val name: String,
        val description: String,
        val updatedTimestamp: Long,
        val thumbnailUrl: String = "",
        val itemCount: Int = 0
)
