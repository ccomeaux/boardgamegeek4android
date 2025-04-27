package com.boardgamegeek.db.model

import androidx.room.ColumnInfo

data class CollectionGameForUpdate(
    @ColumnInfo(name = "_id")
    val internalId: Long,
    @ColumnInfo(name = "updated_list")
    val updatedList: Long,
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
    @ColumnInfo(name = "num_of_plays")
    val numberOfPlays: Int,
)
