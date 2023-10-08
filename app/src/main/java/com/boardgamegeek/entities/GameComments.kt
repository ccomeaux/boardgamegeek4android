package com.boardgamegeek.entities

data class GameComments(
    val numberOfRatings: Int,
    val ratings: List<GameComment>,
)
