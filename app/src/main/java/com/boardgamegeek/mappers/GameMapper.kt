package com.boardgamegeek.mappers

import android.graphics.Color
import com.boardgamegeek.db.model.*
import com.boardgamegeek.db.model.GameRankLocal.Companion.RANK_UNKNOWN
import com.boardgamegeek.model.*
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.model.GameRemote
import com.boardgamegeek.provider.BggContract
import java.text.SimpleDateFormat
import java.util.Locale

@Suppress("SpellCheckingInspection")
fun GameRemote.mapToModel(): Game {
    val (primaryName, sortIndex) = findPrimaryName(this)
    val game = Game(
        id = id,
        name = primaryName,
        sortName = primaryName.sortName(sortIndex),
        imageUrl = image.orEmpty(),
        thumbnailUrl = thumbnail.orEmpty(),
        description = description.replaceHtmlLineFeeds().trim(),
        subtype = type.toThingSubtype().mapToEntitySubtype(),
        yearPublished = yearpublished?.toIntOrNull() ?: Game.YEAR_UNKNOWN,
        minPlayers = minplayers?.toIntOrNull() ?: 0,
        maxPlayers = maxplayers?.toIntOrNull() ?: 0,
        playingTime = playingtime?.toIntOrNull() ?: 0,
        maxPlayingTime = maxplaytime?.toIntOrNull() ?: 0,
        minPlayingTime = minplaytime?.toIntOrNull() ?: 0,
        minimumAge = minage?.toIntOrNull() ?: 0,
        designers = links.filter { it.type == "boardgamedesigner" }.map { it.id to it.value },
        artists = links.filter { it.type == "boardgameartist" }.map { it.id to it.value },
        publishers = links.filter { it.type == "boardgamepublisher" }.map { it.id to it.value },
        categories = links.filter { it.type == "boardgamecategory" }.map { it.id to it.value },
        mechanics = links.filter { it.type == "boardgamemechanic" }.map { it.id to it.value },
        expansions = links.filter { it.type == "boardgameexpansion" }.map { Triple(it.id, it.value, it.inbound == "true") },
        families = links.filter { it.type == "boardgamefamily" }.map { it.id to it.value },
        playerCountsBest = null,
        playerCountsRecommended = null,
        playerCountsNotRecommended = null,
        lastViewedTimestamp = 0L,
        lastPlayTimestamp = null,
        numberOfPlays = 0,
    )
    return if (this.statistics != null) {
        game.copy(
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
            overallRank = this.statistics.ranks.find { it.type == "subtype" }?.value?.toIntOrNull() ?: GameRank.RANK_UNKNOWN,
            ranks = createRanks(this),
            polls = createPolls(this),
            playerPoll = createPlayerPoll(this),
        )
    } else game
}

private fun findPrimaryName(from: GameRemote): Pair<String, Int> {
    return (from.names?.find { "primary" == it.type } ?: from.names?.firstOrNull())?.let { it.value to it.sortindex } ?: ("" to 0)
}

private fun createRanks(from: GameRemote) = from.statistics?.ranks?.map {
    GameRank(
        if (it.type == BggService.RANK_TYPE_FAMILY) GameRank.RankType.Family else GameRank.RankType.Subtype,
        it.name,
        it.friendlyname,
        it.value.toIntOrNull() ?: GameRank.RANK_UNKNOWN,
        it.bayesaverage.toDoubleOrNull() ?: 0.0
    )
}.orEmpty()

private fun BggService.ThingSubtype?.mapToEntitySubtype(): Game.Subtype? = when (this) {
    BggService.ThingSubtype.BOARDGAME -> Game.Subtype.BOARDGAME
    BggService.ThingSubtype.BOARDGAME_EXPANSION -> Game.Subtype.BOARDGAME_EXPANSION
    BggService.ThingSubtype.BOARDGAME_ACCESSORY -> Game.Subtype.BOARDGAME_ACCESSORY
    null -> null
}

@Suppress("SpellCheckingInspection")
private const val PLAYER_POLL_NAME = "suggested_numplayers"

private fun createPolls(from: GameRemote): List<Game.Poll> {
    val polls = mutableListOf<Game.Poll>()
    from.polls?.filter { it.name != PLAYER_POLL_NAME }?.mapTo(polls) { poll ->
        Game.Poll().apply {
            name = poll.name ?: ""
            title = poll.title ?: ""
            totalVotes = poll.totalvotes
            poll.results.forEach {
                results += Game.Results().apply {
                    numberOfPlayers = if (it.numplayers.isNullOrEmpty()) "X" else it.numplayers
                    it.result.forEach { gr ->
                        result.add(GamePollResult(gr.level, gr.value, gr.numvotes))
                    }
                }
            }
        }
    }
    return polls
}

private fun createPlayerPoll(from: GameRemote): List<GamePlayerPollResults> {
    return from.polls?.find { it.name == PLAYER_POLL_NAME }?.let { poll ->
        poll.results.map { playerCount ->
            GamePlayerPollResults(
                totalVotes = poll.totalvotes,
                playerCount = playerCount.numplayers,
                bestVoteCount = playerCount.result.find { it.value == "Best" }?.numvotes ?: 0,
                recommendedVoteCount = playerCount.result.find { it.value == "Recommended" }?.numvotes ?: 0,
                notRecommendedVoteCount = playerCount.result.find { it.value == "Not Recommended" }?.numvotes ?: 0,
            )
        }
    } ?: emptyList()
}

fun GameRankEntity.mapToModel() = GameRank(
    if (gameRankType == BggService.RANK_TYPE_FAMILY) GameRank.RankType.Family else GameRank.RankType.Subtype,
    name = gameRankName,
    friendlyName = gameRankFriendlyName,
    value = gameRankValue,
    bayesAverage = gameRankBayesAverage,
)

fun GameRemote.mapToRatingModel(): GameComments {
    val list = comments.comments.map {
        GameComment(
            username = it.username,
            rating = it.rating.toDoubleOrNull() ?: 0.0,
            comment = it.value,
        )
    }
    return GameComments(this.comments.totalitems, list)
}

fun GameEntity.mapToModel(lastPlayDate: String?): Game {
    val playDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return Game(
        id = gameId,
        name = gameName,
        sortName = gameSortName,
        updated = updated ?: 0L,
        subtype = subtype.toSubtype(),
        thumbnailUrl = thumbnailUrl.orEmpty(),
        imageUrl = imageUrl.orEmpty(),
        heroImageUrl = heroImageUrl.orEmpty(),
        description = description.orEmpty(),
        yearPublished = yearPublished ?: Game.YEAR_UNKNOWN,
        minPlayers = minPlayers ?: 0,
        maxPlayers = maxPlayers ?: 0,
        playingTime = playingTime ?: 0,
        minPlayingTime = minPlayingTime ?: 0,
        maxPlayingTime = maxPlayingTime ?: 0,
        minimumAge = minimumAge ?: 0,
        numberOfRatings = numberOfRatings ?: 0,
        rating = average ?: Game.UNRATED,
        bayesAverage = bayesAverage ?: Game.UNRATED,
        standardDeviation = standardDeviation ?: 0.0,
        median = median ?: Game.UNRATED,
        numberOfUsersOwned = numberOfUsersOwned ?: 0,
        numberOfUsersTrading = numberOfUsersTrading ?: 0,
        numberOfUsersWanting = numberOfUsersWanting ?: 0,
        numberOfUsersWishListing = numberOfUsersWishListing ?: 0,
        numberOfComments = numberOfComments ?: 0,
        numberOfUsersWeighting = numberOfUsersWeighting ?: 0,
        averageWeight = averageWeight ?: 0.0,
        overallRank = gameRank ?: GameRank.RANK_UNKNOWN,
        updatedPlays = updatedPlays ?: 0L,
        customPlayerSort = customPlayerSort ?: false,
        isFavorite = isStarred ?: false,
        suggestedPlayerCountPollVoteTotal = suggestedPlayerCountPollVoteTotal ?: 0,
        iconColor = iconColor ?: Color.TRANSPARENT,
        darkColor = darkColor ?: Color.TRANSPARENT,
        winsColor = winsColor ?: Color.TRANSPARENT,
        winnablePlaysColor = winnablePlaysColor ?: Color.TRANSPARENT,
        allPlaysColor = allPlaysColor ?: Color.TRANSPARENT,
        playerCountsBest = playerCountsBest,
        playerCountsRecommended = playerCountsRecommended,
        playerCountsNotRecommended = playerCountsNotRecommended,
        lastViewedTimestamp = lastViewedTimestamp ?: 0L,
        lastPlayTimestamp = lastPlayDate.toMillis(playDateFormat),
        numberOfPlays = numberOfPlays,
    )
}

fun GameLocal.mapToModel(): Game {
    val playDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return Game(
        id = gameId,
        name = gameName,
        sortName = gameSortName,
        updated = updated ?: 0L,
        subtype = subtype.toSubtype(),
        thumbnailUrl = thumbnailUrl.orEmpty(),
        imageUrl = imageUrl.orEmpty(),
        heroImageUrl = heroImageUrl.orEmpty(),
        description = description.orEmpty(),
        yearPublished = yearPublished ?: Game.YEAR_UNKNOWN,
        minPlayers = minPlayers ?: 0,
        maxPlayers = maxPlayers ?: 0,
        playingTime = playingTime ?: 0,
        minPlayingTime = minPlayingTime ?: 0,
        maxPlayingTime = maxPlayingTime ?: 0,
        minimumAge = minimumAge ?: 0,
        numberOfRatings = numberOfRatings ?: 0,
        rating = average ?: Game.UNRATED,
        bayesAverage = bayesAverage ?: Game.UNRATED,
        standardDeviation = standardDeviation ?: 0.0,
        median = median ?: Game.UNRATED,
        numberOfUsersOwned = numberOfUsersOwned ?: 0,
        numberOfUsersTrading = numberOfUsersTrading ?: 0,
        numberOfUsersWanting = numberOfUsersWanting ?: 0,
        numberOfUsersWishListing = numberOfUsersWishListing ?: 0,
        numberOfComments = numberOfComments ?: 0,
        numberOfUsersWeighting = numberOfUsersWeighting ?: 0,
        averageWeight = averageWeight ?: 0.0,
        overallRank = gameRank ?: GameRank.RANK_UNKNOWN,
        updatedPlays = updatedPlays ?: 0L,
        customPlayerSort = customPlayerSort ?: false,
        isFavorite = isStarred ?: false,
        suggestedPlayerCountPollVoteTotal = suggestedPlayerCountPollVoteTotal ?: 0,
        iconColor = iconColor ?: Color.TRANSPARENT,
        darkColor = darkColor ?: Color.TRANSPARENT,
        winsColor = winsColor ?: Color.TRANSPARENT,
        winnablePlaysColor = winnablePlaysColor ?: Color.TRANSPARENT,
        allPlaysColor = allPlaysColor ?: Color.TRANSPARENT,
        playerCountsBest = playerCountsBest,
        playerCountsRecommended = playerCountsRecommended,
        playerCountsNotRecommended = playerCountsNotRecommended,
        lastViewedTimestamp = lastViewedTimestamp ?: 0L,
        lastPlayTimestamp = lastPlayDate.toMillis(playDateFormat),
        numberOfPlays = numberOfPlays ?: 0,
    )
}

fun List<GamePollResultsWithPoll>.mapToModel() = GamePoll(
    this.map {
        GamePollResult(
            level = it.results.pollResultsResultLevel ?: BggContract.INVALID_ID,
            value = it.results.pollResultsResultValue,
            numberOfVotes = it.results.pollResultsResultVotes,
        )
    }
)

fun GameSuggestedPlayerCountPollResultsEntity.mapToModel() = GamePlayerPollResults(
    totalVotes = 0,
    playerCount = playerCount,
    bestVoteCount = bestVoteCount ?: 0,
    recommendedVoteCount = recommendedVoteCount ?: 0,
    notRecommendedVoteCount = notRecommendedVoteCount ?: 0,
    recommendation = recommendation ?: GamePlayerPollResults.NOT_RECOMMENDED,
)

fun GameExpansionWithGame.mapToModel(items: List<CollectionItem>) = GameExpansion(
    id = gameExpansionEntity.expansionId,
    name = gameExpansionEntity.expansionName,
    thumbnailUrl = items.firstOrNull()?.gameThumbnailUrl.orEmpty().ifBlank { thumbnailUrl.orEmpty() },
    own = items.any { it.own },
    previouslyOwned = items.any { it.previouslyOwned },
    preOrdered = items.any { it.preOrdered },
    forTrade = items.any { it.forTrade },
    wantInTrade = items.any { it.wantInTrade },
    wantToPlay = items.any { it.wantToPlay },
    wantToBuy = items.any { it.wantToBuy },
    wishList = items.any { it.wishList },
    wishListPriority = if (items.isEmpty()) GameExpansion.WISHLIST_PRIORITY_UNKNOWN else items.minOf { it.wishListPriority },
    numberOfPlays = items.firstOrNull()?.numberOfPlays ?: 0,
    rating = if (items.isEmpty()) GameExpansion.UNRATED else items.maxOf { it.rating },
    comment = items.firstOrNull()?.comment.orEmpty(),
)

@Suppress("SpellCheckingInspection")
fun GameRemote.mapForUpsert(updated: Long): GameForUpsert {
    val (primaryName, sortIndex) = findPrimaryName(this)
    val ranks = statistics?.ranks?.map {
        GameRankLocal(
            internalId = BggContract.INVALID_ID.toLong(),
            gameId = this.id,
            gameRankId = it.id,
            gameRankType = it.type,
            gameRankName = it.name,
            gameFriendlyRankName = it.friendlyname,
            gameRankValue = it.value.toIntOrNull() ?: RANK_UNKNOWN,
            gameRankBayesAverage = it.bayesaverage.toDoubleOrNull() ?: 0.0,
        )
    }
    val playerPoll = polls?.find { it.name == PLAYER_POLL_NAME }?.results?.map { playerCount ->
        GameSuggestedPlayerCountPollResultsLocal(
            internalId = BggContract.INVALID_ID.toLong(),
            gameId = this.id,
            playerCount = playerCount.numplayers,
            sortIndex = 0,
            bestVoteCount = playerCount.result.find { it.value == "Best" }?.numvotes ?: 0,
            recommendedVoteCount = playerCount.result.find { it.value == "Recommended" }?.numvotes ?: 0,
            notRecommendedVoteCount = playerCount.result.find { it.value == "Not Recommended" }?.numvotes ?: 0,
        )
    }
    val polls = polls?.filterNot { it.name == PLAYER_POLL_NAME }?.map { poll ->
        GamePollLocal(
            internalId = BggContract.INVALID_ID.toLong(),
            gameId = this.id,
            pollName = poll.name,
            pollTitle = poll.title,
            pollTotalVotes = poll.totalvotes,
            results = poll.results.mapIndexed { sortIndex, r ->
                GamePollResultsLocal(
                    internalId = BggContract.INVALID_ID.toLong(),
                    pollId = BggContract.INVALID_ID,
                    pollResultsKey = if (r.numplayers.isNullOrEmpty()) "X" else r.numplayers,
                    pollResultsPlayers = if (r.numplayers.isNullOrEmpty()) "X" else r.numplayers,
                    pollResultsSortIndex = sortIndex,
                    pollResultsResult = r.result.mapIndexed { index, result ->
                        GamePollResultsResultLocal(
                            internalId = BggContract.INVALID_ID.toLong(),
                            pollResultsId = BggContract.INVALID_ID,
                            pollResultsResultLevel = if (result.level == 0) null else result.level,
                            pollResultsResultValue = result.value,
                            pollResultsResultVotes = result.numvotes,
                            pollResultsResulSortIndex = index + 1,
                        )
                    }
                )
            }
        )
    }
    return GameForUpsert(
        updated = updated,
        updatedList = updated,
        gameId = id,
        gameName = primaryName,
        gameSortName = primaryName.sortName(sortIndex),
        imageUrl = image,
        thumbnailUrl = thumbnail,
        description = description.replaceHtmlLineFeeds().trim(),
        subtype = type.toThingSubtype()?.code,
        yearPublished = yearpublished?.toIntOrNull() ?: Game.YEAR_UNKNOWN,
        minPlayers = minplayers?.toIntOrNull(),
        maxPlayers = maxplayers?.toIntOrNull(),
        playingTime = playingtime?.toIntOrNull(),
        maxPlayingTime = maxplaytime?.toIntOrNull(),
        minPlayingTime = minplaytime?.toIntOrNull(),
        minimumAge = minage?.toIntOrNull(),
        overallRank = statistics?.ranks?.find { it.type == "subtype" }?.value?.toIntOrNull() ?: GameRank.RANK_UNKNOWN,
        numberOfRatings = statistics?.usersrated?.toIntOrNull(),
        average = statistics?.average?.toDoubleOrNull(),
        bayesAverage = statistics?.bayesaverage?.toDoubleOrNull(),
        standardDeviation = statistics?.stddev?.toDoubleOrNull(),
        median = statistics?.median?.toDoubleOrNull(),
        numberOfUsersOwned = statistics?.owned?.toIntOrNull(),
        numberOfUsersTrading = statistics?.trading?.toIntOrNull(),
        numberOfUsersWanting = statistics?.wanting?.toIntOrNull(),
        numberOfUsersWishListing = statistics?.wishing?.toIntOrNull(),
        numberOfComments = statistics?.numcomments?.toIntOrNull(),
        numberOfUsersWeighting = statistics?.numweights?.toIntOrNull(),
        averageWeight = statistics?.averageweight?.toDoubleOrNull(),
        suggestedPlayerCountPollVoteTotal = this.polls.find { it.name == PLAYER_POLL_NAME }?.totalvotes,
        playerCountsBest = playerPoll?.filter { it.recommendation == GameSuggestedPlayerCountPollResultsLocal.BEST }?.map { it.playerCount }.orEmpty().toSet().forDatabase(),
        playerCountsRecommended = playerPoll?.filter { it.recommendation == GameSuggestedPlayerCountPollResultsLocal.RECOMMENDED }?.map { it.playerCount }.orEmpty().toSet().forDatabase(),
        playerCountsNotRecommended = playerPoll?.filter { it.recommendation == GameSuggestedPlayerCountPollResultsLocal.NOT_RECOMMENDED }?.map { it.playerCount }.orEmpty().toSet().forDatabase(),
        ranks = ranks,
        polls = polls,
        playerPoll = playerPoll,
        designers = links.filter { it.type == "boardgamedesigner" }.map { it.id to it.value },
        artists = links.filter { it.type == "boardgameartist" }.map { it.id to it.value },
        publishers = links.filter { it.type == "boardgamepublisher" }.map { it.id to it.value },
        categories = links.filter { it.type == "boardgamecategory" }.map { it.id to it.value },
        mechanics = links.filter { it.type == "boardgamemechanic" }.map { it.id to it.value },
        expansions = links.filter { it.type == "boardgameexpansion" }.map { Triple(it.id, it.value, it.inbound == "true") },
    )
}

fun String?.toThingSubtype() = BggService.ThingSubtype.values().find { this == it.code }

fun <T> Iterable<T>.forDatabase(delimiter: String = "|") = this.joinToString(delimiter, prefix = delimiter, postfix = delimiter)
