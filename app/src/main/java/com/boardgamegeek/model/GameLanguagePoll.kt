package com.boardgamegeek.model

class GameLanguagePoll(val results: List<Result>) {
    val totalVotes: Int by lazy { results.sumOf { it.numberOfVotes } }

    fun calculateScore(): Double {
        if (totalVotes == 0) return 0.0
        val totalLevel = results.sumOf { it.numberOfVotes * ((it.level - 1) % 5 + 1) }
        return totalLevel.toDouble() / totalVotes
    }

    data class Result(
        val level: Int = 0,
        val numberOfVotes: Int = 0,
    )
}
