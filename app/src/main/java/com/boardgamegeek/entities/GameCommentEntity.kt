package com.boardgamegeek.entities

data class GameCommentEntity(
        val username: String,
        val rating: Double,
        val comment: String
)