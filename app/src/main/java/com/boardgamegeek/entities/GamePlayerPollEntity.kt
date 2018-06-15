package com.boardgamegeek.entities

const val maxPlayerCount = 100

data class GamePlayerPollEntity(
        val results: List<GamePlayerPollResultsEntity>
) {
    val totalVotes: Int = results.maxBy { it.totalVotes }?.totalVotes ?: 0

    val bestCounts: List<Int> by lazy {
        results.filter { it.recommendation == GamePlayerPollResultsEntity.BEST }.map {
            it.playerCount.toIntOrNull() ?: maxPlayerCount
        }
    }

    val recommendedCounts: List<Int> by lazy {
        results.filter { it.recommendation == GamePlayerPollResultsEntity.BEST || it.recommendation == GamePlayerPollResultsEntity.RECOMMENDED }.map {
            it.playerCount.toIntOrNull() ?: maxPlayerCount
        }
    }
}