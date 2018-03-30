package com.boardgamegeek.mappers

import android.text.TextUtils
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
            image = from.image
            thumbnail = from.thumbnail
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
                usersRated = from.statistics.usersRated.toIntOrNull() ?: 0
                average = from.statistics.average.toDoubleOrNull() ?: 0.0
                bayesAverage = from.statistics.bayesAverage.toDoubleOrNull() ?: 0.0
                standardDeviation = from.statistics.standardDeviation.toDoubleOrNull() ?: 0.0
                median = from.statistics.median.toDoubleOrNull() ?: 0.0
                owned = from.statistics.owned.toIntOrNull() ?: 0
                trading = from.statistics.trading.toIntOrNull() ?: 0
                wanting = from.statistics.wanting.toIntOrNull() ?: 0
                wishing = from.statistics.wishing.toIntOrNull() ?: 0
                commenting = from.statistics.commenting.toIntOrNull() ?: 0
                weighting = from.statistics.weighting.toIntOrNull() ?: 0
                averageWeight = from.statistics.averageWeight.toDoubleOrNull() ?: 0.0
                rank = getRank(from)
                ranks.addAll(createRanks(from))
            }

            polls.addAll(createPolls(from))

            designers.addAll(getLinks(from, "boardgamedesigner"))
            artists.addAll(getLinks(from, "boardgameartist"))
            publishers.addAll(getLinks(from, "boardgamepublisher"))
            categories.addAll(getLinks(from, "boardgamecategory"))
            mechanics.addAll(getLinks(from, "boardgamemechanic"))
            expansions.addAll(getExpansions(from, "boardgameexpansion"))
            families.addAll(getLinks(from, "boardgamefamily"))
            // boardgameimplementation

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


    private fun getRank(from: Game): Int {
        if (from.statistics != null && from.statistics.ranks != null) {
            for (rank in from.statistics.ranks) {
                if ("subtype" == rank.type) {
                    return rank.value.toIntOrNull() ?: Int.MAX_VALUE
                }
            }
        }
        return Integer.MAX_VALUE
    }

    private fun createRanks(from: Game): List<GameEntity.Rank> {
        val ranks = mutableListOf<GameEntity.Rank>()
        if (from.statistics != null && from.statistics.ranks != null) {
            for (rank in from.statistics.ranks) {
                val r = GameEntity.Rank()
                r.id = rank.id
                r.type = rank.type
                r.name = rank.name
                r.value = rank.value.toIntOrNull() ?: Int.MAX_VALUE
                r.friendlyName = rank.friendlyName
                r.bayesAverage = rank.bayesaverage.toDoubleOrNull() ?: 0.0
                ranks.add(r)
            }
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
                r.numplayers = if (results.numplayers.isNullOrEmpty()) "X" else results.numplayers
                for (result in results.result) {
                    r.result.add(GameEntity.Result(result.level, result.value, result.numvotes))
                }
                p.results.add(r)
            }
            polls.add(p)
        }
        return polls
    }

    private fun getLinks(from: Game, type: String): List<Pair<Int, String>> {
        val list = mutableListOf<Pair<Int, String>>()
        if (!TextUtils.isEmpty(type) && from.links != null && from.links.size > 0) {
            for (link in from.links) {
                if (type == link.type) {
                    list.add(link.id to link.value)
                }
            }
        }
        return list
    }

    private fun getExpansions(from: Game, type: String): List<Triple<Int, String, Boolean>> {
        val list = mutableListOf<Triple<Int, String, Boolean>>()
        if (!TextUtils.isEmpty(type) && from.links != null && from.links.size > 0) {
            for (link in from.links) {
                if (type == link.type) {
                    list.add(Triple(link.id, link.value, "true" == link.inbound))
                }
            }
        }
        return list
    }
}