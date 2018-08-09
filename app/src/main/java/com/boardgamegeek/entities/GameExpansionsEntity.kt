package com.boardgamegeek.entities

data class GameExpansionsEntity(
        val id: Int,
        val name: String,
        var own: Boolean = false,
        var previouslyOwned: Boolean = false,
        var preOrdered: Boolean = false,
        var forTrade: Boolean = false,
        var wantInTrade: Boolean = false,
        var wantToPlay: Boolean = false,
        var wantToBuy: Boolean = false,
        var wishList: Boolean = false,
        var wishListPriority: Int = 3,
        var numberOfPlays: Int = 0,
        var rating: Double = 0.0,
        var comment: String = ""
)
