package com.boardgamegeek.model

data class GameAgePoll(val results: List<Result>) {
    val modalValue: String by lazy { results.maxByOrNull { it.numberOfVotes }?.value ?: "" }

    val totalVotes: Int by lazy { results.sumOf { it.numberOfVotes } }

    data class Result(
        val value: String = "",
        val numberOfVotes: Int = 0,
    )
}
