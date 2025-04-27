package com.boardgamegeek.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "games", indices = [Index("game_id", unique = true)])
data class GameEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val internalId: Long,
    @ColumnInfo
    val updated: Long?,
    @ColumnInfo(name = "updated_list")
    val updatedList: Long,
    @ColumnInfo(name = "game_id")
    val gameId: Int,
    @ColumnInfo(name = "game_name")
    val gameName: String,
    @ColumnInfo(name = "game_sort_name")
    val gameSortName: String,
    @ColumnInfo(name = "year_published")
    val yearPublished: Int?,
    @ColumnInfo(name = "image_url")
    val imageUrl: String?,
    @ColumnInfo(name = "thumbnail_url")
    val thumbnailUrl: String?,
    @ColumnInfo(name = "min_players")
    val minPlayers: Int?,
    @ColumnInfo(name = "max_players")
    val maxPlayers: Int?,
    @ColumnInfo(name = "playing_time")
    val playingTime: Int?,
    @ColumnInfo(name = "min_playing_time")
    val minPlayingTime: Int?,
    @ColumnInfo(name = "max_playing_time")
    val maxPlayingTime: Int?,
    @ColumnInfo(name = "num_of_plays", defaultValue = "0")
    val numberOfPlays: Int,
    @ColumnInfo(name = "age")
    val minimumAge: Int?,
    @ColumnInfo
    val description: String?,
    @ColumnInfo
    val subtype: String?,
    @ColumnInfo(name = "usersrated")
    val numberOfRatings: Int?,
    @ColumnInfo
    val average: Double?,
    @ColumnInfo(name = "bayes_average")
    val bayesAverage: Double?,
    @ColumnInfo(name = "standard_deviation")
    val standardDeviation: Double?,
    @ColumnInfo
    val median: Double?,
    @ColumnInfo(name = "number_owned")
    val numberOfUsersOwned: Int?,
    @ColumnInfo(name = "number_trading")
    val numberOfUsersTrading: Int?,
    @ColumnInfo(name = "number_wanting")
    val numberOfUsersWanting: Int?,
    @ColumnInfo(name = "number_wishing")
    val numberOfUsersWishListing: Int?,
    @ColumnInfo(name = "number_commenting")
    val numberOfComments: Int?,
    @ColumnInfo(name = "number_weighting")
    val numberOfUsersWeighting: Int?,
    @ColumnInfo(name = "average_weight")
    val averageWeight: Double?,
    @ColumnInfo(name = "last_viewed")
    val lastViewedTimestamp: Long?,
    @ColumnInfo(name = "starred")
    val isStarred: Boolean?,
    @ColumnInfo(name = "updated_plays")
    val updatedPlays: Long?,
    @ColumnInfo(name = "custom_player_sort")
    val customPlayerSort: Boolean?,
    @ColumnInfo(name = "game_rank")
    val gameRank: Int?,
    @ColumnInfo(name = "suggested_player_count_poll_vote_total")
    val suggestedPlayerCountPollVoteTotal: Int?,
    @ColumnInfo(name = "hero_image_url")
    val heroImageUrl: String?,
    @ColumnInfo(name = "ICON_COLOR")
    val iconColor: Int?,
    @ColumnInfo(name = "DARK_COLOR")
    val darkColor: Int?,
    @ColumnInfo(name = "WINS_COLOR")
    val winsColor: Int?,
    @ColumnInfo(name = "WINNABLE_PLAYS_COLOR")
    val winnablePlaysColor: Int?,
    @ColumnInfo(name = "ALL_PLAYS_COLOR")
    val allPlaysColor: Int?,
    @ColumnInfo(name = "player_counts_best")
    val playerCountsBest: String?,
    @ColumnInfo(name = "player_counts_recommended")
    val playerCountsRecommended: String?,
    @ColumnInfo(name = "player_count_nots_recommended")
    val playerCountsNotRecommended: String?,
)
