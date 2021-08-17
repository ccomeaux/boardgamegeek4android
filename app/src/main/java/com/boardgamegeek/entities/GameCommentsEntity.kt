package com.boardgamegeek.entities

data class GameCommentsEntity(
    val numberOfRatings: Int,
    val ratings: List<GameCommentEntity>
)
