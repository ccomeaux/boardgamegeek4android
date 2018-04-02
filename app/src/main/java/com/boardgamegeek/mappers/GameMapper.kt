package com.boardgamegeek.mappers

import com.boardgamegeek.entities.GameEntity
import com.boardgamegeek.model.Constants
import com.boardgamegeek.model.Game
import com.boardgamegeek.util.StringUtils

class GameMapper {
    fun map(from: Game, updateTime: Long = System.currentTimeMillis()): GameEntity {
        val game = GameEntity()
        game.apply {
            id = from.id
            val (primaryName, sortIndex) = findPrimaryName(from)
            name = primaryName
            sortName = StringUtils.createSortName(primaryName, sortIndex)
            imageUrl = from.image
            thumbnailUrl = from.thumbnail
            description = StringUtils.replaceHtmlLineFeeds(from.description).trim()
            subtype = from.type
            yearPublished = from.yearpublished.toIntOrNull() ?: Constants.YEAR_UNKNOWN
            minPlayers = from.minplayers.toIntOrNull() ?: 0
            maxPlayers = from.maxplayers.toIntOrNull() ?: 0
            playingTime = from.playingtime.toIntOrNull() ?: 0
            maxPlayingTime = from.maxplaytime.toIntOrNull() ?: 0
            minPlayingTime = from.minplaytime.toIntOrNull() ?: 0
            minAge = from.minage.toIntOrNull() ?: 0
            if (from.statistics != null) {
                hasStatistics = true
                numberOfRatings = from.statistics.usersrated.toIntOrNull() ?: 0
                average = from.statistics.average.toDoubleOrNull() ?: 0.0
                bayesAverage = from.statistics.bayesaverage.toDoubleOrNull() ?: 0.0
                standardDeviation = from.statistics.stddev.toDoubleOrNull() ?: 0.0
                median = from.statistics.median.toDoubleOrNull() ?: 0.0
                numberOfUsersOwned = from.statistics.owned.toIntOrNull() ?: 0
                numberOfUsersTrading = from.statistics.trading.toIntOrNull() ?: 0
                numberOfUsersWanting = from.statistics.wanting.toIntOrNull() ?: 0
                numberOfUsersWishListing = from.statistics.wishing.toIntOrNull() ?: 0
                numberOfComments = from.statistics.numcomments.toIntOrNull() ?: 0
                numberOfUsersWeighting = from.statistics.numweights.toIntOrNull() ?: 0
                averageWeight = from.statistics.averageweight.toDoubleOrNull() ?: 0.0
                ranks.addAll(createRanks(from))
                overallRank = ranks.find { it.type == "subtype" }?.value ?: Int.MAX_VALUE
            }

            polls.addAll(createPolls(from))

            if (from.links != null && from.links.size > 0) {
                for (link in from.links) {
                    when (link.type) {
                        "boardgamedesigner" -> designers.add(link.id to link.value)
                        "boardgameartist" -> artists.add(link.id to link.value)
                        "boardgamepublisher" -> publishers.add(link.id to link.value)
                        "boardgamecategory" -> categories.add(link.id to link.value)
                        "boardgamemechanic" -> mechanics.add(link.id to link.value)
                        "boardgameexpansion" -> expansions.add(Triple(link.id, link.value, "true" == link.inbound))
                        "boardgamefamily" -> families.add(link.id to link.value)
                    // "boardgameimplementation"
                    }
                }
            }

            updated = updateTime
            updatedList = updateTime
        }
        return game
    }

    private fun findPrimaryName(from: Game): Pair<String, Int> {
        if (from.names != null) {
            for (name in from.names) {
                if ("primary" == name.type) {
                    return name.value to name.sortindex
                }
            }
        }
        return "" to 0
    }

    private fun createRanks(from: Game): List<GameEntity.Rank> {
        val ranks = mutableListOf<GameEntity.Rank>()
        from.statistics?.ranks?.mapTo(ranks) {
            GameEntity.Rank(
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
        for (poll in from.polls) {
            val p = GameEntity.Poll()
            p.name = poll.name ?: ""
            p.title = poll.title ?: ""
            p.totalVotes = poll.totalvotes
            for (results in poll.results) {
                val r = GameEntity.Results()
                r.numberOfPlayers = if (results.numplayers.isNullOrEmpty()) "X" else results.numplayers
                for (result in results.result) {
                    r.result.add(GameEntity.Result(result.level, result.value, result.numvotes))
                }
                p.results.add(r)
            }
            polls.add(p)
        }
        return polls
    }
}
