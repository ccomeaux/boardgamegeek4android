package com.boardgamegeek.db.model

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class GamePollResultsForUpsert(
    @ColumnInfo(name = "_id")
    val internalId: Long,
    @ColumnInfo(name = "poll_id")
    val pollId: Int,
    @ColumnInfo(name = "pollresults_key")
    val pollResultsKey: String,
    @ColumnInfo(name = "pollresults_players")
    val pollResultsPlayers: String?,
    @ColumnInfo(name = "pollresults_sortindex")
    val pollResultsSortIndex: Int,
    @Embedded
    val pollResultsResult: List<GamePollResultsResultEntity>,
)
