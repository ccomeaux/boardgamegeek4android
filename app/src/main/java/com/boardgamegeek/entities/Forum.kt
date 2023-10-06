package com.boardgamegeek.entities

data class Forum(
    val id: Int,
    val title: String,
    val numberOfThreads: Int,
    val lastPostDateTime: Long,
    val isHeader: Boolean
) {
    enum class Type {
        REGION,
        GAME,
        ARTIST,
        DESIGNER,
        PUBLISHER
    }
}
