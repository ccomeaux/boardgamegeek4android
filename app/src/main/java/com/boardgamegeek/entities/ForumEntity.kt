package com.boardgamegeek.entities

data class ForumEntity(
        val id: Int,
        val title: String,
        val numberOfThreads: Int,
        val lastPostDateTime: Long,
        val isHeader: Boolean
) {
    enum class ForumType {
        REGION,
        GAME,
        ARTIST
    }
}
