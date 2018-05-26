package com.boardgamegeek.entities

data class GameSuggestedAgePollEntity(val results: List<GamePollResultEntity>) {
    val modalValue: String by lazy { results.maxBy { it.numberOfVotes }?.value ?: "" }
}