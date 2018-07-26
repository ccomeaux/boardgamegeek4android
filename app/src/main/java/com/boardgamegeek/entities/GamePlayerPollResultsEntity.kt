package com.boardgamegeek.entities

data class GamePlayerPollResultsEntity(
        val totalVotes: Int = 0,
        val playerCount: String = "0",
        val bestVoteCount: Int = 0,
        val recommendedVoteCount: Int = 0,
        val notRecommendedVoteCount: Int = 0,
        val recommendation: Int = 0
) {
    fun calculateRecommendation(): Int {
        val halfTotalVoteCount = ((bestVoteCount + recommendedVoteCount + notRecommendedVoteCount) / 2) + 1
        return when {
            halfTotalVoteCount == 0 -> UNKNOWN
            bestVoteCount >= halfTotalVoteCount -> BEST
            bestVoteCount + recommendedVoteCount >= halfTotalVoteCount -> RECOMMENDED
            notRecommendedVoteCount >= halfTotalVoteCount -> NOT_RECOMMENDED
            else -> UNKNOWN
        }
    }

    companion object {
        const val BEST = 2
        const val RECOMMENDED = 1
        const val UNKNOWN = 0
        const val NOT_RECOMMENDED = -1
    }
}
