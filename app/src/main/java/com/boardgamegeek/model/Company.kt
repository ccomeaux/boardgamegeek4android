package com.boardgamegeek.model

import java.util.Date

data class Company(
    val id: Int,
    val name: String,
    val sortName: String,
    val description: String,
    val imageUrl: String,
    val thumbnailUrl: String,
    val heroImageUrl: String = "",
    val updatedTimestamp: Date?,
    val itemCount: Int = 0,
    val whitmoreScore: Int = 0,
    val statsUpdatedTimestamp: Date? = null,
)
