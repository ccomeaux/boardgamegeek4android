package com.boardgamegeek.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Suppress("SpellCheckingInspection")
@Entity(
    tableName = "game_poll_results_result",
    foreignKeys = [
        ForeignKey(GamePollResultsEntity::class, ["_id"], ["pollresults_id"], ForeignKey.CASCADE)
    ],
)
data class GamePollResultsResultEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val internalId: Long,
    @ColumnInfo(name = "pollresults_id")
    val pollResultsId: Int,
    @ColumnInfo(name = "pollresultsresult_key")
    val pollResultsResultKey: String,
    @ColumnInfo(name = "pollresultsresult_level")
    val pollResultsResultLevel: Int?,
    @ColumnInfo(name = "pollresultsresult_value")
    val pollResultsResultValue: String,
    @ColumnInfo(name = "pollresultsresult_votes")
    val pollResultsResultVotes: Int,
    @ColumnInfo(name = "pollresultsresult_sortindex")
    val pollResultsResultSortIndex: Int,
)
