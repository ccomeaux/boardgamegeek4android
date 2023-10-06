package com.boardgamegeek.entities

data class Thread(
    val threadId: Int,
    val subject: String,
    val author: String,
    val numberOfArticles: Int,
    val lastPostDate: Long
)
