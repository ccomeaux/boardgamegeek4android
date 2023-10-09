package com.boardgamegeek.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "game_poll_results",
    foreignKeys = [
        ForeignKey(GamePollEntity::class, ["_id"], ["poll_id"], ForeignKey.CASCADE)
    ],
)
@Suppress("SpellCheckingInspection")
data class GamePollResultsEntity(
    @PrimaryKey(autoGenerate = true)
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
)

