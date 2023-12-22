package com.boardgamegeek.model

class GameLanguagePoll(val results: List<Result>) {
    val totalVotes: Int by lazy { results.sumOf { it.numberOfVotes } }

    fun calculateScore(): Double {
        if (totalVotes == 0) return 0.0
        val totalLevel = results.sumOf { it.numberOfVotes * (it.level?.value ?: 0) }
        return totalLevel.toDouble() / totalVotes
    }

    data class Result(
        val level: Level?,
        val numberOfVotes: Int,
    )

    enum class Level(val value: Int) {
        NONE(1),
        SOME(2),
        MODERATE(3),
        EXTENSIVE(4),
        UNPLAYABLE(5),
    }
}
