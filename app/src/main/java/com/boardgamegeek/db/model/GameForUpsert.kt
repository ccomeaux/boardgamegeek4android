package com.boardgamegeek.db.model

import androidx.room.ColumnInfo

data class GameForUpsert(
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
    @ColumnInfo(name = "game_rank")
    val gameRank: Int?,
    @ColumnInfo(name = "suggested_player_count_poll_vote_total")
    val suggestedPlayerCountPollVoteTotal: Int?,
    @ColumnInfo(name = "player_counts_best")
    val playerCountsBest: String?,
    @ColumnInfo(name = "player_counts_recommended")
    val playerCountsRecommended: String?,
    @ColumnInfo(name = "player_count_nots_recommended")
    val playerCountsNotRecommended: String?,
    val ranks: List<GameRankLocal>?,
    val polls: List<GamePollLocal>?,
    val playerPoll: List<GameSuggestedPlayerCountPollResultsLocal>?,
    val designers: List<Pair<Int, String>>?,
    val artists: List<Pair<Int, String>>?,
    val publishers: List<Pair<Int, String>>?,
    val categories: List<Pair<Int, String>>?,
    val mechanics: List<Pair<Int, String>>?,
    val expansions: List<Triple<Int, String, Boolean>>?,
) {
    override fun toString() = "$gameName [$gameId]"
}
