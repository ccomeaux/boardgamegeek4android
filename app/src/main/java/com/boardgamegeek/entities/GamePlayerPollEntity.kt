package com.boardgamegeek.entities


data class GamePlayerPollEntity(val results: List<GamePlayerPollResultsEntity>) {
    val totalVotes: Int = results.maxByOrNull { it.totalVotes }?.totalVotes ?: 0

    val bestCounts: Set<Int> by lazy {
        results.filter { it.calculatedRecommendation == GamePlayerPollResultsEntity.BEST }.map {
            it.playerCount.toIntOrNull() ?: maxPlayerCount
        }.sorted().toSet()
    }

    private val recommendedCounts: Set<Int> by lazy {
        results.filter { it.calculatedRecommendation == GamePlayerPollResultsEntity.RECOMMENDED }.map {
            it.playerCount.toIntOrNull() ?: maxPlayerCount
        }.sorted().toSet()
    }

    val notRecommendedCounts: Set<Int> by lazy {
        results.filter { it.calculatedRecommendation == GamePlayerPollResultsEntity.NOT_RECOMMENDED }.map {
            it.playerCount.toIntOrNull() ?: maxPlayerCount
        }.sorted().toSet()
    }

    val recommendedAndBestCounts: Set<Int> by lazy {
        (bestCounts + recommendedCounts).sorted().toSet()
    }

    companion object {
        const val maxPlayerCount = 100
        const val separator = "|"
    }
}
