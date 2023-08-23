package com.boardgamegeek.db.model

// TODO unique on playerType, playerName, & playerColor
data class PlayerColorsLocal(
    val internalId: Int,
    val playerType: Int,
    val playerName: String,
    val playerColor: String,
    val playerColorSortOrder: Int,
)
