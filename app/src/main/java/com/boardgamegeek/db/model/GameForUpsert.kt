package com.boardgamegeek.db.model

data class GameForUpsert(
    val header: GameForUpsertHeader,
    val ranks: List<GameRankLocal>,
    val polls: List<GamePollLocal>,
    val playerPoll: List<GameSuggestedPlayerCountPollResultsLocal>,
    val designers: List<GameDesignerEntity>,
    val artists: List<GameArtistEntity>,
    val publishers: List<GamePublisherEntity>,
    val categories: List<GameCategoryEntity>,
    val mechanics: List<GameMechanicEntity>,
    val expansions: List<GameExpansionEntity>,
) {
    override fun toString() = "${header.gameName} [${header.gameId}]"
}
