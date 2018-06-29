package com.boardgamegeek.mappers

import com.boardgamegeek.entities.*
import com.boardgamegeek.io.model.Game
import com.boardgamegeek.replaceHtmlLineFeeds
import com.boardgamegeek.sortName

class GameMapper {
    fun map(from: Game): GameEntity {
        val (primaryName, sortIndex) = findPrimaryName(from)
        val game = GameEntity(
                id = from.id,
                name = primaryName,
                sortName = primaryName.sortName(sortIndex),
                imageUrl = from.image ?: "",
                thumbnailUrl = from.thumbnail ?: "",
                description = from.description.replaceHtmlLineFeeds().trim(),
                subtype = from.type ?: "",
                yearPublished = from.yearpublished?.toIntOrNull() ?: YEAR_UNKNOWN,
                minPlayers = from.minplayers?.toIntOrNull() ?: 0,
                maxPlayers = from.maxplayers?.toIntOrNull() ?: 0,
                playingTime = from.playingtime?.toIntOrNull() ?: 0,
                maxPlayingTime = from.maxplaytime?.toIntOrNull() ?: 0,
                minPlayingTime = from.minplaytime?.toIntOrNull() ?: 0,
                minimumAge = from.minage?.toIntOrNull() ?: 0
        )
        game.apply {
            if (from.statistics != null) {
                hasStatistics = true
                numberOfRatings = from.statistics.usersrated?.toIntOrNull() ?: 0
                rating = from.statistics.average?.toDoubleOrNull() ?: 0.0
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
                overallRank = ranks.find { it.type == "subtype" }?.value ?: RANK_UNKNOWN
            }

            polls.addAll(createPolls(from))
            playerPoll = createPlayerPoll(from)

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

    private fun createRanks(from: Game): List<GameRankEntity> {
        val ranks = mutableListOf<GameRankEntity>()
        from.statistics?.ranks?.mapTo(ranks) {
            GameRankEntity(
                    it.id,
                    it.type,
                    it.name,
                    it.friendlyname,
                    it.value.toIntOrNull() ?: RANK_UNKNOWN,
                    it.bayesaverage.toDoubleOrNull() ?: 0.0)
        }
        return ranks
    }

    private val playerPollName = "suggested_numplayers"

    private fun createPolls(from: Game): List<GameEntity.Poll> {
        val polls = mutableListOf<GameEntity.Poll>()
        from.polls?.filter { it.name != playerPollName }?.mapTo(polls) {
            GameEntity.Poll().apply {
                name = it.name ?: ""
                title = it.title ?: ""
                totalVotes = it.totalvotes
                it.results.forEach {
                    results.add(GameEntity.Results().apply {
                        numberOfPlayers = if (it.numplayers.isNullOrEmpty()) "X" else it.numplayers
                        it.result.forEach {
                            result.add(GamePollResultEntity(it.level, it.value, it.numvotes))
                        }
                    })
                }
            }
        }
        return polls
    }

    private fun createPlayerPoll(from: Game): GamePlayerPollEntity? {
        from.polls?.find { it.name == playerPollName }?.let { poll ->
            val results = mutableListOf<GamePlayerPollResultsEntity>()
            poll.results.forEach { playerCount ->
                results.add(GamePlayerPollResultsEntity(
                        totalVotes = poll.totalvotes,
                        playerCount = playerCount.numplayers,
                        bestVoteCount = playerCount.result.find { it.value == "Best" }?.numvotes ?: 0,
                        recommendedVoteCount = playerCount.result.find { it.value == "Recommended" }?.numvotes ?: 0,
                        notRecommendedVoteCount = playerCount.result.find { it.value == "Not Recommended" }?.numvotes ?: 0,
                        recommendation = GamePlayerPollResultsEntity.UNKNOWN
                ))
            }
            return GamePlayerPollEntity(results)
        }
        return null
    }
}
