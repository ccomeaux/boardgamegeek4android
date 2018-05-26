package com.boardgamegeek.entities

data class GameSuggestedLanguagePollEntity(val results: List<GamePollResultEntity>) {
    val totalVotes: Int by lazy { results.sumBy { it.numberOfVotes } }

    fun calculateScore(): Double {
        if (totalVotes == 0) return 0.0
        val totalLevel = results.sumBy { it.numberOfVotes * ((it.level - 1) % 5 + 1) }
        return totalLevel.toDouble() / totalVotes
    }
}