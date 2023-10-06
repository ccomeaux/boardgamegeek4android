package com.boardgamegeek.entities

data class ThreadArticles(
    val threadId: Int,
    val subject: String,
    val articles: List<Article>
)
