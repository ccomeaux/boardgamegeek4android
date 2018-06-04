package com.boardgamegeek.entities

import com.boardgamegeek.util.PlayerCountRecommendation

const val maxPlayerCount = 100

data class GamePlayerPollEntity(
        val results: List<GamePlayerPollResultsEntity>
) {
    val totalVotes: Int = results.maxBy { it.totalVotes }?.totalVotes ?: 0

    val bestCounts: List<Int> by lazy {
        results.filter { it.recommendation == PlayerCountRecommendation.BEST }.map {
            it.playerCount.toIntOrNull() ?: maxPlayerCount
        }
    }

    val recommendedCounts: List<Int> by lazy {
        results.filter { it.recommendation == PlayerCountRecommendation.BEST || it.recommendation == PlayerCountRecommendation.RECOMMENDED }.map {
            it.playerCount.toIntOrNull() ?: maxPlayerCount
        }
    }
}