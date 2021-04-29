package com.boardgamegeek.entities

data class ForumThreadsEntity(
        val numberOfThreads: Int,
        val threads: List<ThreadEntity>
)
