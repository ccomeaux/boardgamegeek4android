package com.boardgamegeek.entities

data class ThreadArticlesEntity(
        val threadId: Int,
        val subject: String,
        val articles: List<ArticleEntity>
)