package com.boardgamegeek.mappers

import com.boardgamegeek.entities.GameEntity
import com.boardgamegeek.entities.GameRank
import com.boardgamegeek.io.model.Game
import com.boardgamegeek.model.Constants
import com.boardgamegeek.replaceHtmlLineFeeds
import com.boardgamegeek.sortName

class GameMapper {
    fun map(from: Game): GameEntity {
        val game = GameEntity()
        game.apply {
            id = from.id
            val (primaryName, sortIndex) = findPrimaryName(from)
            name = primaryName
            sortName = primaryName.sortName(sortIndex)
            imageUrl = from.image ?: ""
            thumbnailUrl = from.thumbnail ?: ""
            description = from.description.replaceHtmlLineFeeds().trim()
            subtype = from.type ?: ""
            yearPublished = from.yearpublished?.toIntOrNull() ?: Constants.YEAR_UNKNOWN
            minPlayers = from.minplayers?.toIntOrNull() ?: 0
            maxPlayers = from.maxplayers?.toIntOrNull() ?: 0
            playingTime = from.playingtime?.toIntOrNull() ?: 0
            maxPlayingTime = from.maxplaytime?.toIntOrNull() ?: 0
            minPlayingTime = from.minplaytime?.toIntOrNull() ?: 0
            minAge = from.minage?.toIntOrNull() ?: 0
            if (from.statistics != null) {
                hasStatistics = true
                numberOfRatings = from.statistics.usersrated?.toIntOrNull() ?: 0
                average = from.statistics.average?.toDoubleOrNull() ?: 0.0
                bayesAverage = from.statistics.bayesaverage?.toDoubleOrNull() ?: 0.0
                standardDeviation = from.statistics.stddev?.toDoubleOrNull() ?: 0.0
                median = from.statistics.median?.toDoubleOrNull() ?: 0.0
                numberOfUsersOwned = from.statistics.owned?.toIntOrNull() ?: 0
                numberOfUsersTrading = from.statistics.trading?.toIntOrNull() ?: 0
                numberOfUsersWanting = from.statistics.wanting?.toIntOrNull() ?: 0
                numberOfUsersWishListing = from.statistics.wishing?.toIntOrNull() ?: 0
                numberOfComments = from.statistics.numcomments?.toIntOrNull() ?: 0
                numberOfUsersWeighting = from.statistics.numweights?.toIntOrNull() ?: 0
                averageWeight = from.statistics.averageweight?.toDoubleOrNull() ?: 0.0
                ranks.addAll(createRanks(from))
                overallRank = ranks.find { it.type == "subtype" }?.value ?: Int.MAX_VALUE
            }

            polls.addAll(createPolls(from))

            from.links?.forEach {
                when (it.type) {
                    "boardgamedesigner" -> designers.add(it.id to it.value)
                    "boardgameartist" -> artists.add(it.id to it.value)
                    "boardgamepublisher" -> publishers.add(it.id to it.value)
                    "boardgamecategory" -> categories.add(it.id to it.value)
                    "boardgamemechanic" -> mechanics.add(it.id to it.value)
                    "boardgameexpansion" -> expansions.add(Triple(it.id, it.value, "true" == it.inbound))
                    "boardgamefamily" -> families.add(it.id to it.value)
                // "boardgameimplementation"
                }
            }
        }
        return game
    }

    private fun findPrimaryName(from: Game): Pair<String, Int> {
        return from.names?.find { "primary" == it.type }?.let { it.value to it.sortindex }
                ?: from.names?.firstOrNull()?.let { it.value to it.sortindex } ?: "" to 0
    }

    private fun createRanks(from: Game): List<GameRank> {
        val ranks = mutableListOf<GameRank>()
        from.statistics?.ranks?.mapTo(ranks) {
            GameRank(
                    it.id,
                    it.type,
                    it.name,
                    it.friendlyname,
                    it.value.toIntOrNull() ?: Int.MAX_VALUE,
                    it.bayesaverage.toDoubleOrNull() ?: 0.0)
        }
        return ranks
    }

    private fun createPolls(from: Game): List<GameEntity.Poll> {
        val polls = mutableListOf<GameEntity.Poll>()
        from.polls?.mapTo(polls) {
            GameEntity.Poll().apply {
                name = it.name ?: ""
                title = it.title ?: ""
                totalVotes = it.totalvotes
                it.results.forEach {
                    results.add(GameEntity.Results().apply {
                        numberOfPlayers = if (it.numplayers.isNullOrEmpty()) "X" else it.numplayers
                        it.result.forEach {
                            result.add(GameEntity.Result(it.level, it.value, it.numvotes))
                        }
                    })
                }
            }
        }
        return polls
    }
}
