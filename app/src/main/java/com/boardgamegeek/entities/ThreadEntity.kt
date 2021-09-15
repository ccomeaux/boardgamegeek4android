package com.boardgamegeek.entities

data class ThreadEntity(
        val threadId: Int,
        val subject: String,
        val author: String,
        val numberOfArticles: Int,
        val lastPostDate: Long
)