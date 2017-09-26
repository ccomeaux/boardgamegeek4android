package com.boardgamegeek.ui.model

import android.database.Cursor
import com.boardgamegeek.provider.BggContract.Games

class Game private constructor(
        val id: Int,
        val name: String,
        val thumbnailUrl: String,
        val imageUrl: String,
        val rating: Double,
        val yearPublished: Int,
        val minPlayers: Int,
        val maxPlayers: Int,
        val playingTime: Int,
        val minPlayingTime: Int,
        val maxPlayingTime: Int,
        val minimumAge: Int,
        val description: String,
        val usersRated: Int,
        val usersCommented: Int,
        val updated: Long,
        val rank: Int,
        val averageWeight: Double,
        val numberWeights: Int,
        private val numberOwned: Int,
        private val numberTrading: Int,
        private val numberWanting: Int,
        private val numberWishing: Int,
        val subtype: String,
        val customPlayerSort: Boolean,
        val isFavorite: Boolean,
        private val pollVoteTotal: Int,
        private val suggestedPlayerCountPollVoteTotal: Int
) {

    val maxUsers: Int
        get() {
            var max = Math.max(usersRated, usersCommented)
            max = Math.max(max, numberOwned)
            max = Math.max(max, numberTrading)
            max = Math.max(max, numberWanting)
            max = Math.max(max, numberWeights)
            max = Math.max(max, numberWishing)
            return max
        }

    val pollsVoteCount: Int
        get() {
            return pollVoteTotal + suggestedPlayerCountPollVoteTotal
        }

    companion object {
        val projection = arrayOf(
                Games.GAME_ID,
                Games.STATS_AVERAGE,
                Games.YEAR_PUBLISHED,
                Games.MIN_PLAYERS,
                Games.MAX_PLAYERS,
                Games.PLAYING_TIME,
                Games.MINIMUM_AGE,
                Games.DESCRIPTION,
                Games.STATS_USERS_RATED,
                Games.UPDATED,
                Games.GAME_RANK,
                Games.GAME_NAME,
                Games.THUMBNAIL_URL,
                Games.STATS_BAYES_AVERAGE,
                Games.STATS_MEDIAN,
                Games.STATS_STANDARD_DEVIATION,
                Games.STATS_NUMBER_WEIGHTS,
                Games.STATS_AVERAGE_WEIGHT,
                Games.STATS_NUMBER_OWNED,
                Games.STATS_NUMBER_TRADING,
                Games.STATS_NUMBER_WANTING,
                Games.STATS_NUMBER_WISHING,
                Games.IMAGE_URL,
                Games.SUBTYPE,
                Games.CUSTOM_PLAYER_SORT,
                Games.STATS_NUMBER_COMMENTS,
                Games.MIN_PLAYING_TIME,
                Games.MAX_PLAYING_TIME,
                Games.STARRED,
                Games.POLLS_COUNT,
                Games.SUGGESTED_PLAYER_COUNT_POLL_VOTE_TOTAL
        )

        private val GAME_ID = 0
        private val STATS_AVERAGE = 1
        private val YEAR_PUBLISHED = 2
        private val MIN_PLAYERS = 3
        private val MAX_PLAYERS = 4
        private val PLAYING_TIME = 5
        private val MINIMUM_AGE = 6
        private val DESCRIPTION = 7
        private val STATS_USERS_RATED = 8
        private val UPDATED = 9
        private val GAME_RANK = 10
        private val GAME_NAME = 11
        private val THUMBNAIL_URL = 12
        private val STATS_NUMBER_WEIGHTS = 16
        private val STATS_AVERAGE_WEIGHT = 17
        private val STATS_NUMBER_OWNED = 18
        private val STATS_NUMBER_TRADING = 19
        private val STATS_NUMBER_WANTING = 20
        private val STATS_NUMBER_WISHING = 21
        private val IMAGE_URL = 22
        private val SUBTYPE = 23
        private val CUSTOM_PLAYER_SORT = 24
        private val STATS_NUMBER_COMMENTS = 25
        private val MIN_PLAYING_TIME = 26
        private val MAX_PLAYING_TIME = 27
        private val STARRED = 28
        private val POLLS_COUNT = 29
        private val SUGGESTED_PLAYER_COUNT_POLL_VOTE_TOTAL = 30

        fun fromCursor(cursor: Cursor): Game {
            return Game(
                    cursor.getInt(GAME_ID),
                    cursor.getString(GAME_NAME),
                    cursor.getString(THUMBNAIL_URL) ?: "",
                    cursor.getString(IMAGE_URL) ?: "",
                    cursor.getDouble(STATS_AVERAGE),
                    cursor.getInt(YEAR_PUBLISHED),
                    cursor.getInt(MIN_PLAYERS),
                    cursor.getInt(MAX_PLAYERS),
                    cursor.getInt(PLAYING_TIME),
                    cursor.getInt(MIN_PLAYING_TIME),
                    cursor.getInt(MAX_PLAYING_TIME),
                    cursor.getInt(MINIMUM_AGE),
                    cursor.getString(DESCRIPTION) ?: "",
                    cursor.getInt(STATS_USERS_RATED),
                    cursor.getInt(STATS_NUMBER_COMMENTS),
                    cursor.getLong(UPDATED),
                    cursor.getInt(GAME_RANK),
                    cursor.getDouble(STATS_AVERAGE_WEIGHT),
                    cursor.getInt(STATS_NUMBER_WEIGHTS),
                    cursor.getInt(STATS_NUMBER_OWNED),
                    cursor.getInt(STATS_NUMBER_TRADING),
                    cursor.getInt(STATS_NUMBER_WANTING),
                    cursor.getInt(STATS_NUMBER_WISHING),
                    cursor.getString(SUBTYPE) ?: "",
                    cursor.getInt(CUSTOM_PLAYER_SORT) == 1,
                    cursor.getInt(STARRED) == 1,
                    cursor.getInt(POLLS_COUNT),
                    cursor.getInt(SUGGESTED_PLAYER_COUNT_POLL_VOTE_TOTAL)
            )
        }
    }
}
