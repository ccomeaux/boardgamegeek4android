package com.boardgamegeek.db.model

data class PlayerColorsLocal(
    val internalId: Int,
    val playerType: Int,
    val playerName: String,
    val playerColor: String,
    val playerColorSortOrder: Int,
)
