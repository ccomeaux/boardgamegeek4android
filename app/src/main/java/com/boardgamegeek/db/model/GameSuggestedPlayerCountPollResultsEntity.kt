package com.boardgamegeek.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "game_suggested_player_count_poll_results",
    foreignKeys = [ForeignKey(GameEntity::class, ["game_id"], ["game_id"], ForeignKey.CASCADE)],
)
data class GameSuggestedPlayerCountPollResultsEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val internalId: Long,
    @ColumnInfo(name = "game_id", index = true)
    val gameId: Int,
    @ColumnInfo(name = "player_count")
    val playerCount: String,
    @ColumnInfo(name = "sort_index")
    val sortIndex: Int?,
    @ColumnInfo(name = "best_vote_count")
    val bestVoteCount: Int?,
    @ColumnInfo(name = "recommended_vote_count")
    val recommendedVoteCount: Int?,
    @ColumnInfo(name = "not_recommended_vote_count")
    val notRecommendedVoteCount: Int?,
    @ColumnInfo(name = "recommendation")
    val recommendation: Int?,
) {
    companion object {
        const val BEST = 2
        const val RECOMMENDED = 1
        const val UNKNOWN = 0
        const val NOT_RECOMMENDED = -1
    }
}
