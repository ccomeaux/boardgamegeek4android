package com.boardgamegeek.mappers

import android.graphics.Color
import com.boardgamegeek.db.model.*
import com.boardgamegeek.model.*
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.model.GameRemote
import java.text.SimpleDateFormat
import java.util.Locale

private fun findPrimaryName(from: GameRemote): Pair<String, Int> {
    return (from.names?.find { "primary" == it.type } ?: from.names?.firstOrNull())?.let { it.value to it.sortindex } ?: ("" to 0)
}

@Suppress("SpellCheckingInspection")
private const val PLAYER_POLL_NAME = "suggested_numplayers"
private const val AGE_POLL_NAME = "suggested_playerage"
private const val LANGUAGE_POLL_NAME = "language_dependence"

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
            rating = it.rating.toDoubleOrNull() ?: Game.UNRATED,
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
    )
}

fun List<GameAgePollResultEntity>.mapToModel() = GameAgePoll(
    this.mapIndexed { i, it ->
        GameAgePoll.Result(
            value = if (i == size - 1) "${it.value}+" else it.value.toString(),
            numberOfVotes = it.votes,
        )
    }
)

fun List<GameLanguagePollResultEntity>.mapToModel() = GameLanguagePoll(
    this.map { entity ->
        GameLanguagePoll.Result(
            level = GameLanguagePoll.Level.values().find { it.value == (entity.level - 1) % 5 + 1 },
            numberOfVotes = entity.votes,
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
fun GameRemote.mapForUpsert(internalId: Long, updated: Long): GameForUpsert {
    val (primaryName, sortIndex) = findPrimaryName(this)
    val ranks = statistics?.ranks?.map {
        GameRankEntity(
            internalId = 0L,
            gameId = this.id,
            gameRankId = it.id,
            gameRankType = it.type,
            gameRankName = it.name,
            gameRankFriendlyName = it.friendlyname,
            gameRankValue = it.value.toIntOrNull() ?: GameRankEntity.RANK_UNKNOWN,
            gameRankBayesAverage = it.bayesaverage.toDoubleOrNull() ?: 0.0,
        )
    }.orEmpty()
    val playerPoll = polls?.find { it.name == PLAYER_POLL_NAME }?.results?.mapIndexed { index, playerCount ->
        val bestVoteCount = playerCount.result.find { it.value == "Best" }?.numvotes ?: 0
        val recommendedVoteCount = playerCount.result.find { it.value == "Recommended" }?.numvotes ?: 0
        val notRecommendedVoteCount = playerCount.result.find { it.value == "Not Recommended" }?.numvotes ?: 0
        val halfTotalVoteCount = ((bestVoteCount + recommendedVoteCount + notRecommendedVoteCount) / 2) + 1
        val recommendation = when {
            halfTotalVoteCount == 0 -> GameSuggestedPlayerCountPollResultsEntity.UNKNOWN
            bestVoteCount >= halfTotalVoteCount -> GameSuggestedPlayerCountPollResultsEntity.BEST
            bestVoteCount + recommendedVoteCount >= halfTotalVoteCount -> GameSuggestedPlayerCountPollResultsEntity.RECOMMENDED
            notRecommendedVoteCount >= halfTotalVoteCount -> GameSuggestedPlayerCountPollResultsEntity.NOT_RECOMMENDED
            else -> GameSuggestedPlayerCountPollResultsEntity.UNKNOWN
        }
        GameSuggestedPlayerCountPollResultsEntity(
            internalId = 0L,
            gameId = this.id,
            playerCount = playerCount.numplayers,
            sortIndex = index + 1,
            bestVoteCount = playerCount.result.find { it.value == "Best" }?.numvotes ?: 0,
            recommendedVoteCount = playerCount.result.find { it.value == "Recommended" }?.numvotes ?: 0,
            notRecommendedVoteCount = playerCount.result.find { it.value == "Not Recommended" }?.numvotes ?: 0,
            recommendation = recommendation,
        )
    }.orEmpty()
    val agePoll = polls?.find { it.name == AGE_POLL_NAME }?.results?.firstOrNull()?.result?.map {
        GameAgePollResultEntity(
            gameId = this.id,
            value = it.value.split(" ").firstOrNull()?.toInt() ?: 21,
            votes = it.numvotes,
        )
    }
    val languagePoll = polls?.find { it.name == LANGUAGE_POLL_NAME }?.results?.firstOrNull()?.result?.map {
        GameLanguagePollResultEntity(
            gameId = this.id,
            level = it.level,
            votes = it.numvotes,
        )
    }
    val header = GameForUpsertHeader(
        internalId = internalId,
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
        gameRank = statistics?.ranks?.find { it.type == "subtype" }?.value?.toIntOrNull() ?: GameRank.RANK_UNKNOWN,
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
        playerCountsBest = playerPoll.filter { it.recommendation == GameSuggestedPlayerCountPollResultsEntity.BEST }.map { it.playerCount }.toSet().forDatabase(),
        playerCountsRecommended = playerPoll.filter { it.recommendation == GameSuggestedPlayerCountPollResultsEntity.RECOMMENDED }.map { it.playerCount }.toSet().forDatabase(),
        playerCountsNotRecommended = playerPoll.filter { it.recommendation == GameSuggestedPlayerCountPollResultsEntity.NOT_RECOMMENDED }.map { it.playerCount }.toSet().forDatabase(),
    )
    return GameForUpsert(
        header = header,
        ranks = ranks,
        agePollResults = agePoll.orEmpty(),
        languagePollResults = languagePoll.orEmpty(),
        playerPoll = playerPoll,
        designers = links?.filter { it.type == "boardgamedesigner" }?.map { GameDesignerEntity(0L, this.id, it.id) }.orEmpty(),
        artists = links?.filter { it.type == "boardgameartist" }?.map { GameArtistEntity(0L, this.id, it.id) }.orEmpty(),
        publishers = links?.filter { it.type == "boardgamepublisher" }?.map { GamePublisherEntity(0L, this.id, it.id) }.orEmpty(),
        categories = links?.filter { it.type == "boardgamecategory" }?.map { GameCategoryEntity(0L, this.id, it.id) }.orEmpty(),
        mechanics = links?.filter { it.type == "boardgamemechanic" }?.map { GameMechanicEntity(0L, this.id, it.id) }.orEmpty(),
        expansions = links?.filter { it.type == "boardgameexpansion" }?.map { GameExpansionEntity(0L, this.id, it.id, it.value, it.inbound == "true") }.orEmpty(),
    )
}

fun GameRemote.mapToDesigners() = links.filter {
    it.type == "boardgamedesigner"
}.map {
    DesignerBriefForUpsert(0L, it.id, it.value)
}

fun GameRemote.mapToArtists() = links.filter {
    it.type == "boardgameartist"
}.map {
    ArtistBriefForUpsert(0L, it.id, it.value)
}

fun GameRemote.mapToPublishers() = links.filter {
    it.type == "boardgamepublisher"
}.map {
    PublisherBriefForUpsert(0L, it.id, it.value)
}

fun GameRemote.mapToCategories() = links.filter {
    it.type == "boardgamecategory"
}.map {
    CategoryEntity(0L, it.id, it.value)
}

fun GameRemote.mapToMechanics() = links.filter {
    it.type == "boardgamemechanic"
}.map {
    MechanicEntity(0L, it.id, it.value)
}

fun String?.toThingSubtype() = BggService.ThingSubtype.values().find { this == it.code }

fun <T> Iterable<T>.forDatabase(delimiter: String = "|") = this.joinToString(delimiter, prefix = delimiter, postfix = delimiter)
