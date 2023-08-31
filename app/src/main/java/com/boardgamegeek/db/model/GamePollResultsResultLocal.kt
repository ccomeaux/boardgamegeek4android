package com.boardgamegeek.db.model

data class GamePollResultsResultLocal(
    val internalId: Long,
    val pollResultsId: Int,
    val pollResultsResultLevel: Int?,
    val pollResultsResultValue: String,
    val pollResultsResultVotes: Int,
    val pollResultsResulSortIndex: Int,
) {
    val pollResultsResultKey: String
        get() {
            return pollResultsResultLevel?.toString() ?: pollResultsResultValue.substringBefore(" ")
        }
}
