package com.boardgamegeek.ui.model

data class Game constructor(
        val id: Int,
        val name: String,
        val thumbnailUrl: String,
        val imageUrl: String,
        val heroImageUrl: String,
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
        private val suggestedPlayerCountPollVoteTotal: Int,
        val iconColor: Int,
        val darkColor: Int,
        val winsColor: Int,
        val winnablePlaysColor: Int,
        val allPlaysColor: Int
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
}
