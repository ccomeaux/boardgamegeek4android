package com.boardgamegeek.model

data class ThreadArticles(
    val threadId: Int,
    val subject: String,
    val articles: List<Article>
)
