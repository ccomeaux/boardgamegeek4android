package com.boardgamegeek.entities

data class GamePoll(val results: List<GamePollResult>) {
    val modalValue: String by lazy { results.maxByOrNull { it.numberOfVotes }?.value ?: "" }

    val totalVotes: Int by lazy { results.sumOf { it.numberOfVotes } }

    fun calculateScore(): Double {
        if (totalVotes == 0) return 0.0
        val totalLevel = results.sumOf { it.numberOfVotes * ((it.level - 1) % 5 + 1) }
        return totalLevel.toDouble() / totalVotes
    }
}
