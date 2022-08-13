package com.boardgamegeek.mappers

import com.boardgamegeek.entities.*
import com.boardgamegeek.extensions.replaceHtmlLineFeeds
import com.boardgamegeek.extensions.sortName
import com.boardgamegeek.extensions.toSubtype
import com.boardgamegeek.extensions.toThingSubtype
import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.model.Game

fun Game.mapToEntity(): GameEntity {
    val (primaryName, sortIndex) = findPrimaryName(this)
    val game = GameEntity(
        id = this.id,
        name = primaryName,
        sortName = primaryName.sortName(sortIndex),
        imageUrl = this.image.orEmpty(),
        thumbnailUrl = this.thumbnail.orEmpty(),
        description = this.description.replaceHtmlLineFeeds().trim(),
        subtype = this.type.toThingSubtype().mapToEntitySubtype(),
        yearPublished = this.yearpublished?.toIntOrNull() ?: GameEntity.YEAR_UNKNOWN,
        minPlayers = this.minplayers?.toIntOrNull() ?: 0,
        maxPlayers = this.maxplayers?.toIntOrNull() ?: 0,
        playingTime = this.playingtime?.toIntOrNull() ?: 0,
        maxPlayingTime = this.maxplaytime?.toIntOrNull() ?: 0,
        minPlayingTime = this.minplaytime?.toIntOrNull() ?: 0,
        minimumAge = this.minage?.toIntOrNull() ?: 0,
        designers = this.links.filter { it.type == "boardgamedesigner" }.map { it.id to it.value },
        artists = this.links.filter { it.type == "boardgameartist" }.map { it.id to it.value },
        publishers = this.links.filter { it.type == "boardgamepublisher" }.map { it.id to it.value },
        categories = this.links.filter { it.type == "boardgamecategory" }.map { it.id to it.value },
        mechanics = this.links.filter { it.type == "boardgamemechanic" }.map { it.id to it.value },
        expansions = this.links.filter { it.type == "boardgameexpansion" }.map { Triple(it.id, it.value, it.inbound == "true") },
        families = this.links.filter { it.type == "boardgamefamily" }.map { it.id to it.value },
        // "boardgameimplementation"
    )
    return if (this.statistics != null) {
        game.copy(
            hasStatistics = true,
            numberOfRatings = this.statistics.usersrated?.toIntOrNull() ?: 0,
            rating = this.statistics.average?.toDoubleOrNull() ?: 0.0,
            bayesAverage = this.statistics.bayesaverage?.toDoubleOrNull() ?: 0.0,
            standardDeviation = this.statistics.stddev?.toDoubleOrNull() ?: 0.0,
            median = this.statistics.median?.toDoubleOrNull() ?: 0.0,
            numberOfUsersOwned = this.statistics.owned?.toIntOrNull() ?: 0,
            numberOfUsersTrading = this.statistics.trading?.toIntOrNull() ?: 0,
            numberOfUsersWanting = this.statistics.wanting?.toIntOrNull() ?: 0,
            numberOfUsersWishListing = this.statistics.wishing?.toIntOrNull() ?: 0,
            numberOfComments = this.statistics.numcomments?.toIntOrNull() ?: 0,
            numberOfUsersWeighting = this.statistics.numweights?.toIntOrNull() ?: 0,
            averageWeight = this.statistics.averageweight?.toDoubleOrNull() ?: 0.0,
            overallRank = this.statistics.ranks.find { it.type == "subtype" }?.value?.toIntOrNull() ?: GameRankEntity.RANK_UNKNOWN,
            ranks = createRanks(this),
            polls = createPolls(this),
            playerPoll = createPlayerPoll(this),
        )
    } else game
}

private fun findPrimaryName(from: Game): Pair<String, Int> {
    return (from.names?.find { "primary" == it.type } ?: from.names?.firstOrNull())?.let { it.value to it.sortindex } ?: ("" to 0)
}

private fun createRanks(from: Game): List<GameRankEntity> {
    val ranks = mutableListOf<GameRankEntity>()
    from.statistics?.ranks?.mapTo(ranks) {
        GameRankEntity(
            it.id,
            it.type,
            it.name,
            it.friendlyname,
            it.value.toIntOrNull() ?: GameRankEntity.RANK_UNKNOWN,
            it.bayesaverage.toDoubleOrNull() ?: 0.0
        )
    }
    return ranks
}

private fun BggService.ThingSubtype?.mapToEntitySubtype(): GameEntity.Subtype? = when (this){
    BggService.ThingSubtype.BOARDGAME -> GameEntity.Subtype.BOARDGAME
    BggService.ThingSubtype.BOARDGAME_EXPANSION -> GameEntity.Subtype.BOARDGAME_EXPANSION
    BggService.ThingSubtype.BOARDGAME_ACCESSORY -> GameEntity.Subtype.BOARDGAME_ACCESSORY
    null -> null
}

private const val playerPollName = "suggested_numplayers"

private fun createPolls(from: Game): List<GameEntity.Poll> {
    val polls = mutableListOf<GameEntity.Poll>()
    from.polls?.filter { it.name != playerPollName }?.mapTo(polls) { poll ->
        GameEntity.Poll().apply {
            name = poll.name ?: ""
            title = poll.title ?: ""
            totalVotes = poll.totalvotes
            poll.results.forEach {
                results += GameEntity.Results().apply {
                    numberOfPlayers = if (it.numplayers.isNullOrEmpty()) "X" else it.numplayers
                    it.result.forEach { gr ->
                        result.add(GamePollResultEntity(gr.level, gr.value, gr.numvotes))
                    }
                }
            }
        }
    }
    return polls
}

private fun createPlayerPoll(from: Game): GamePlayerPollEntity? {
    from.polls?.find { it.name == playerPollName }?.let { poll ->
        val results = mutableListOf<GamePlayerPollResultsEntity>()
        poll.results.forEach { playerCount ->
            results += GamePlayerPollResultsEntity(
                totalVotes = poll.totalvotes,
                playerCount = playerCount.numplayers,
                bestVoteCount = playerCount.result.find { it.value == "Best" }?.numvotes ?: 0,
                recommendedVoteCount = playerCount.result.find { it.value == "Recommended" }?.numvotes ?: 0,
                notRecommendedVoteCount = playerCount.result.find { it.value == "Not Recommended" }?.numvotes ?: 0,
            )
        }
        return GamePlayerPollEntity(results)
    }
    return null
}

fun Game.mapToRatingEntities(): GameCommentsEntity {
    val list = comments.comments.map {
        GameCommentEntity(
            username = it.username,
            rating = it.rating.toDoubleOrNull() ?: 0.0,
            comment = it.value,
        )
    }
    return GameCommentsEntity(this.comments.totalitems, list)
}
