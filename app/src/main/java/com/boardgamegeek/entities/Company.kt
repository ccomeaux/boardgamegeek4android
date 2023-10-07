package com.boardgamegeek.entities

data class Company(
    val id: Int,
    val name: String,
    val sortName: String,
    val description: String,
    val imageUrl: String,
    val thumbnailUrl: String,
    val heroImageUrl: String = "",
    val updatedTimestamp: Long = 0L,
    val itemCount: Int = 0,
    val whitmoreScore: Int = 0,
    val statsUpdatedTimestamp: Long = 0L,
)
