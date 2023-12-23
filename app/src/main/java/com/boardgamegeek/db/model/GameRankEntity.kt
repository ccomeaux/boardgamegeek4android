package com.boardgamegeek.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Suppress("SpellCheckingInspection")
@Entity(
    tableName = "game_ranks",
    foreignKeys = [
        ForeignKey(GameEntity::class, ["game_id"], ["game_id"], ForeignKey.CASCADE)
    ],
)
data class GameRankEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val internalId: Long,
    @ColumnInfo(name = "game_id", index = true)
    val gameId: Int,
    @ColumnInfo(name = "gamerank_id")
    val gameRankId: Int,
    @ColumnInfo(name = "gamerank_type")
    val gameRankType: String,
    @ColumnInfo(name = "gamerank_name")
    val gameRankName: String,
    @ColumnInfo(name = "gamerank_friendly_name")
    val gameRankFriendlyName: String,
    @ColumnInfo(name = "gamerank_value")
    val gameRankValue: Int,
    @ColumnInfo(name = "gamerank_bayes_average")
    val gameRankBayesAverage: Double,
) {
    companion object {
        const val RANK_UNKNOWN = Integer.MAX_VALUE
    }
}
