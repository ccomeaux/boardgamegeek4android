package com.boardgamegeek.entities


data class GamePlayerPollEntity(val results: List<GamePlayerPollResultsEntity>) {
    val totalVotes: Int = results.maxByOrNull { it.totalVotes }?.totalVotes ?: 0

    val bestCounts: Set<String> by lazy {
        results
            .filter { it.calculatedRecommendation == GamePlayerPollResultsEntity.BEST }
            .map { it.playerCount }
            .toSet()
    }

    private val recommendedCounts: Set<String> by lazy {
        results
            .filter { it.calculatedRecommendation == GamePlayerPollResultsEntity.RECOMMENDED }
            .map { it.playerCount }
            .toSet()
    }

    val notRecommendedCounts: Set<String> by lazy {
        results
            .filter { it.calculatedRecommendation == GamePlayerPollResultsEntity.NOT_RECOMMENDED }
            .map { it.playerCount }
            .toSet()
    }

    val recommendedAndBestCounts: Set<String> by lazy {
        (bestCounts + recommendedCounts).toSet()
    }

    companion object {
        const val separator = "|"
    }
}
