package com.boardgamegeek.db.model

data class GameRankLocal(
    val internalId: Long,
    val gameId: Int,
    val gameRankId: Int,
    val gameRankType: String,
    val gameRankName: String,
    val gameFriendlyRankName: String,
    val gameRankValue: Int,
    val gameRankBayesAverage: Double,
) {
    companion object {
        const val RANK_UNKNOWN = Integer.MAX_VALUE
    }
}
