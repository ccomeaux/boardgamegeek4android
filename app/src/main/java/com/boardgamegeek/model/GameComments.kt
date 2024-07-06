package com.boardgamegeek.model

data class GameComments(
    val numberOfRatings: Int,
    val ratings: List<GameComment>,
)
