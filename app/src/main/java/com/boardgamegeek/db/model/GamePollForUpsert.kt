package com.boardgamegeek.db.model

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class GamePollForUpsert(
    @ColumnInfo(name = "_id")
    val internalId: Long,
    @ColumnInfo(name = "game_id")
    val gameId: Int,
    @ColumnInfo(name = "poll_name")
    val pollName: String,
    @ColumnInfo(name = "poll_title")
    val pollTitle: String,
    @ColumnInfo(name = "poll_total_votes")
    val pollTotalVotes: Int,
    @Embedded
    val results: List<GamePollResultsForUpsert>,
)
