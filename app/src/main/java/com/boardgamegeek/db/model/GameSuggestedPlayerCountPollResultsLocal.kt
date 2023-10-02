package com.boardgamegeek.db.model

data class GameSuggestedPlayerCountPollResultsLocal(
    val internalId: Long,
    val gameId: Int,
    val playerCount: String,
    val sortIndex: Int?,
    val bestVoteCount: Int,
    val recommendedVoteCount: Int,
    val notRecommendedVoteCount: Int,
) {
    val recommendation: Int? by lazy {
        val halfTotalVoteCount = ((bestVoteCount + recommendedVoteCount + notRecommendedVoteCount) / 2) + 1
        when {
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
