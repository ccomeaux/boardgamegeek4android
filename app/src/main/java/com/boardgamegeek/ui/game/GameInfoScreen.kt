package com.boardgamegeek.ui.game

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.*
import com.boardgamegeek.ui.CommentsActivity
import com.boardgamegeek.ui.dialog.GameAgePollDialogFragment
import com.boardgamegeek.ui.dialog.GameLanguagePollDialogFragment
import com.boardgamegeek.ui.dialog.GameRanksDialogFragment
import com.boardgamegeek.ui.dialog.GameSuggestedPlayerCountPollDialogFragment
import com.boardgamegeek.ui.theme.BggAppTheme
import java.text.DecimalFormat

@Composable
fun GameInfoScreen(
    game: Game,
    subtypes: List<GameSubtype>,
    families: List<GameFamily>,
    playerPoll: List<GamePlayerPollResults>?,
    agePoll: GameAgePoll?,
    languagePoll: GameLanguagePoll?,
    modifier: Modifier = Modifier,
    host: Fragment? = null
) {
    val context = LocalContext.current
    val iconColor = Color(game.iconColor.addAlphaToColor())
    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        val windowSizeClass = currentWindowAdaptiveInfo(supportLargeAndXLargeWidth = true).windowSizeClass
        if (windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    RankRow(subtypes, families, iconColor) {
                        host?.let { GameRanksDialogFragment.launch(it) }
                    }
                    YearRow(game.yearPublished, iconColor)
                    PlayerCountRow(game.minPlayers, game.maxPlayers, playerPoll, iconColor) {
                        host?.let { GameSuggestedPlayerCountPollDialogFragment.launch(it) }
                    }
                    WeightRow(game.averageWeight, game.numberOfUsersWeighting, iconColor)
                }
                Column(modifier = Modifier.weight(1f)) {
                    RatingsRow(game.rating, game.numberOfRatings, game.numberOfComments, iconColor) {
                        CommentsActivity.startRating(context, game.id, game.name)
                    }
                    PlayingTimeRow(game.minPlayingTime, game.maxPlayingTime, iconColor)
                    PlayerAgesRow(game.minimumAge, agePoll, iconColor) {
                        host?.let { GameAgePollDialogFragment.launch(it) }
                    }
                    languagePoll?.let { poll ->
                        LanguageRow(poll, iconColor) {
                            host?.let { GameLanguagePollDialogFragment.launch(it) }
                        }
                    }
                }
            }
        } else {
            RankRow(subtypes, families, iconColor) {
                host?.let { GameRanksDialogFragment.launch(it) }
            }
            RatingsRow(game.rating, game.numberOfRatings, game.numberOfComments, iconColor) {
                CommentsActivity.startRating(context, game.id, game.name)
            }
            YearRow(game.yearPublished, iconColor)
            PlayingTimeRow(game.minPlayingTime, game.maxPlayingTime, iconColor)
            PlayerCountRow(game.minPlayers, game.maxPlayers, playerPoll, iconColor) {
                host?.let { GameSuggestedPlayerCountPollDialogFragment.launch(it) }
            }
            PlayerAgesRow(game.minimumAge, agePoll, iconColor) {
                host?.let { GameAgePollDialogFragment.launch(it) }
            }
            WeightRow(game.averageWeight, game.numberOfUsersWeighting, iconColor)
            languagePoll?.let { poll ->
                LanguageRow(poll, iconColor) {
                    host?.let { GameLanguagePollDialogFragment.launch(it) }
                }
            }
        }
        GameFooter(
            game.updated,
            game.id,
            Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun GameRow(
    @DrawableRes iconResId: Int,
    @StringRes contentDescriptionResId: Int,
    modifier: Modifier = Modifier,
    iconColor: Color = Color.Transparent,
    content: @Composable () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(dimensionResource(R.dimen.game_row_height))
    ) {
        Icon(
            painterResource(iconResId),
            contentDescription = stringResource(contentDescriptionResId),
            modifier = Modifier
                .padding(start = 4.dp, end = 28.dp, top = 12.dp)
                .size(24.dp),
            tint = iconColor,
        )
        Column(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .heightIn(dimensionResource(R.dimen.game_row_height)),
            verticalArrangement = Arrangement.Center,
        ) {
            content()
        }
    }
}

@Composable
private fun RankRow(
    gameSubtypes: List<GameSubtype>,
    gameFamilies: List<GameFamily>,
    iconColor: Color = Color.Transparent,
    onClick: () -> Unit = { },
) {
    val rankSeparator = "  ${HtmlCompat.fromHtml("&#9679;", HtmlCompat.FROM_HTML_MODE_LEGACY)}  "
    GameRow(
        R.drawable.rank_24px,
        R.string.rank,
        iconColor = iconColor,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Column {
            PrimaryRowText(
                text = gameSubtypes.map { it.describe(LocalContext.current) }.joinTo(rankSeparator).toString()
            )
            if (gameFamilies.isNotEmpty()) {
                SecondaryRowText(
                    text = gameFamilies.map { it.describe(LocalContext.current) }.joinTo(rankSeparator).toString()
                )
            }
        }
    }
}

@Composable
private fun RatingsRow(
    rating: Double,
    numberOfRatings: Int,
    numberOfComments: Int,
    iconColor: Color = Color.Transparent,
    onClick: () -> Unit = { },
) {
    GameRow(
        R.drawable.star_rate_24px,
        R.string.rating,
        Modifier.clickable(onClick = onClick),
        iconColor,
    ) {
        val color = rating.toColor(BggColors.ratingColors)
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .background(Color(color), MaterialTheme.shapes.extraSmall)
                .border(width = 1.dp, color = Color(color.darkenColor()), shape = MaterialTheme.shapes.extraSmall)
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = rating.asBoundedRating(LocalContext.current, DecimalFormat("#0.0"), R.string.unrated),
                maxLines = 1,
                style = MaterialTheme.typography.titleMedium,
                color = Color(color.getTextColor()),
            )
        }
        SecondaryRowText(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(numberOfRatings.toFormattedString())
                }
                append(" ")
                append(pluralStringResource(R.plurals.ratings, numberOfRatings))
                append(" & ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(
                        numberOfComments.toFormattedString()
                    )
                }
                append(" ")
                append(pluralStringResource(R.plurals.comments, numberOfComments))
            },
        )
    }
}

@Composable
private fun YearRow(
    yearPublished: Int,
    iconColor: Color = Color.Transparent,
) {
    GameRow(
        R.drawable.year_24px,
        R.string.year_published,
        iconColor = iconColor
    ) {
        PrimaryRowText(
            yearPublished.asYear(LocalContext.current),
        )
    }
}

@Composable
private fun PlayingTimeRow(
    minPlayingTime: Int,
    maxPlayingTime: Int,
    iconColor: Color = Color.Transparent,
) {
    GameRow(
        R.drawable.play_time_24px,
        R.string.year_published,
        iconColor = iconColor
    ) {
        PrimaryRowText(
            pluralStringResource(R.plurals.mins_suffix, minPlayingTime, (minPlayingTime to maxPlayingTime).asRange()),
        )
    }
}

@Composable
private fun PlayerCountRow(
    minPLayers: Int,
    maxPlayers: Int,
    pollResults: List<GamePlayerPollResults>?,
    iconColor: Color = Color.Transparent,
    onClick: () -> Unit = {},
) {
    GameRow(
        R.drawable.player_count_24px,
        R.string.player_range,
        modifier = Modifier.clickable(onClick = onClick),
        iconColor = iconColor,
    ) {
        PrimaryRowText(
            text = pluralStringResource(R.plurals.player_range_suffix, minPLayers, (minPLayers to maxPlayers).asRange()),
        )
        pollResults?.let {
            val bestCounts = pollResults.filter { it.calculatedRecommendation == GamePlayerPollResults.BEST }.toSet()
            val goodCounts =
                pollResults.filter { it.calculatedRecommendation == GamePlayerPollResults.BEST || it.calculatedRecommendation == GamePlayerPollResults.RECOMMENDED }
                    .toSet()

            val best = stringResource(R.string.best_prefix, bestCounts.toList().asRange())
            val good = stringResource(R.string.recommended_prefix, goodCounts.toList().asRange())
            val communityText = when {
                bestCounts.isNotEmpty() && goodCounts.isNotEmpty() && bestCounts != goodCounts -> stringResource(R.string.ampersand, best, good)
                bestCounts.isNotEmpty() -> best
                goodCounts.isNotEmpty() -> good
                else -> ""
            }
            SecondaryRowText(
                text = communityText,
            )
        }
    }
}

private fun List<GamePlayerPollResults>.asRange(comma: String = ", ", dash: String = " - "): String {
    return this.sortedBy { it.playerNumber }.fold(mutableListOf<MutableList<GamePlayerPollResults>>()) { accumulator, element ->
        val current = element.playerNumber
        val last = accumulator.lastOrNull()?.lastOrNull()?.playerNumber ?: Int.MAX_VALUE
        if (accumulator.isEmpty() || last != current - 1) {
            accumulator += mutableListOf(element)
        } else accumulator.last() += element
        accumulator
    }.joinToString(comma) {
        if (it.size == 1)
            it.first().playerCount
        else if (it.last().playerCount.endsWith('+'))
            it.first().playerCount + "+"
        else
            it.first().playerCount + dash + it.last().playerCount
    }
}

@Composable
private fun PlayerAgesRow(
    minimumAge: Int,
    poll: GameAgePoll?,
    iconColor: Color = Color.Transparent,
    onClick: () -> Unit = {},
) {
    GameRow(
        R.drawable.player_age_24px,
        R.string.player_ages,
        modifier = Modifier.clickable(onClick = onClick),
        iconColor = iconColor,
    ) {
        PrimaryRowText(
            text = minimumAge.asAge(LocalContext.current).toString()
        )
        poll?.let {
            val message = if (it.modalValue.isBlank()) ""
            else stringResource(R.string.age_community, poll.modalValue)
            Text(
                text = message,
                maxLines = 1,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            VotesRowText(it.totalVotes)
        }
    }
}

@Composable
private fun WeightRow(
    averageWeight: Double,
    voteCount: Int,
    iconColor: Color = Color.Transparent,
) {
    GameRow(
        R.drawable.weight_24px,
        R.string.weight,
        iconColor = iconColor,
    ) {
        val weightColor = averageWeight.toColor(BggColors.fiveStageColors)
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .background(Color(weightColor), MaterialTheme.shapes.extraSmall)
                .border(width = 1.dp, color = Color(weightColor.darkenColor()), shape = MaterialTheme.shapes.extraSmall)
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = averageWeight.toDescription(LocalContext.current, R.array.game_weight, R.string.unknown_weight).toString(),
                maxLines = 1,
                style = MaterialTheme.typography.titleMedium,
                color = Color(weightColor.getTextColor()),
                modifier = Modifier.alignByBaseline(),
            )
            if (averageWeight != Game.UNWEIGHTED) {
                Text(
                    text = averageWeight.asScore(LocalContext.current, format = DecimalFormat("0.00")),
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(weightColor.getTextColor()),
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .alignByBaseline()
                )
            }
        }
        VotesRowText(voteCount)
    }
}

@Composable
private fun LanguageRow(
    poll: GameLanguagePoll,
    iconColor: Color = Color.Transparent,
    onClick: () -> Unit = { },
) {
    val score by remember { derivedStateOf { poll.calculateScore() } }
    GameRow(
        R.drawable.language_24px,
        R.string.language_dependence,
        Modifier.clickable(onClick = onClick),
        iconColor,
    ) {
        val levelColor = score.toColor(BggColors.fiveStageColors)
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .background(Color(levelColor), MaterialTheme.shapes.extraSmall)
                .border(width = 1.dp, color = Color(levelColor.darkenColor()), shape = MaterialTheme.shapes.extraSmall)
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = score.toDescription(LocalContext.current, R.array.language_poll, R.string.unknown_language).toString(),
                maxLines = 1,
                style = MaterialTheme.typography.titleMedium,
                color = Color(levelColor.getTextColor()),
                modifier = Modifier.alignByBaseline(),
            )
            if (score != 0.0) {
                Text(
                    text = score.asScore(LocalContext.current, format = DecimalFormat("0.00")),
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(levelColor.getTextColor()),
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .alignByBaseline()
                )
            }
        }
        VotesRowText(poll.totalVotes)
    }
}

@Composable
private fun VotesRowText(voteCount: Int) {
    SecondaryRowText(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(voteCount.toFormattedString())
            }
            append(" ")
            append(pluralStringResource(R.plurals.votes, voteCount))
        },
    )
}

@Preview(backgroundColor = 0xFFF, showBackground = true)
@Composable
private fun GameInfoPreview() {
    val game = Game(
        id = 13,
        name = "Planet Unknown",
        iconColor = 0x0088FF,
        rating = 7.4,
        numberOfRatings = 456,
        numberOfComments = 654,
        yearPublished = 1991,
        minPlayingTime = 30,
        maxPlayingTime = 45,
        minPlayers = 3,
        maxPlayers = 5,
        minimumAge = 13,
        averageWeight = 3.2,
        numberOfUsersWeighting = 42_987,
        updated = 0L,
    )
    BggAppTheme {
        val gameSubtypes = listOf(GameSubtype(Game.Subtype.BoardGameExpansion, 42, 8.24))
        val gameFamilies = listOf(GameFamily(GameFamily.Family.Strategy, 7, 8.45))
        val playerPollResults = listOf(
            GamePlayerPollResults(7, "2", 1, 3, 2),
            GamePlayerPollResults(7, "3", 6, 1, 0),
        )
        val agesPoll = GameAgePoll(
            listOf(
                GameAgePoll.Result("10", 15)
            )
        )
        val languagePoll = GameLanguagePoll(
            listOf(
                GameLanguagePoll.Result(GameLanguagePoll.Level.MODERATE, 7),
                GameLanguagePoll.Result(GameLanguagePoll.Level.SOME, 23),
            )
        )
        GameInfoScreen(
            game,
            subtypes = gameSubtypes,
            families = gameFamilies,
            playerPoll = playerPollResults,
            agePoll = agesPoll,
            languagePoll = languagePoll,
        )
    }
}
