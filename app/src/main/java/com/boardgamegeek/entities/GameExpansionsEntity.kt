package com.boardgamegeek.entities

data class GameExpansionsEntity(
    val id: Int,
    val name: String,
    val own: Boolean = false,
    val previouslyOwned: Boolean = false,
    val preOrdered: Boolean = false,
    val forTrade: Boolean = false,
    val wantInTrade: Boolean = false,
    val wantToPlay: Boolean = false,
    val wantToBuy: Boolean = false,
    val wishList: Boolean = false,
    val wishListPriority: Int = 3,
    val numberOfPlays: Int = 0,
    val rating: Double = 0.0,
    val comment: String = ""
)
