package com.boardgamegeek.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.model.Game
import com.boardgamegeek.model.GameFamily
import com.boardgamegeek.model.GameSubtype
import com.boardgamegeek.ui.compose.Rating
import com.boardgamegeek.ui.compose.RatingDefaults
import com.boardgamegeek.ui.theme.BggAppTheme
import java.text.DecimalFormat

@Composable
fun GameRanksDialog(
    game: Game,
    subtypes: List<GameSubtype>?,
    families: List<GameFamily>?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState())

    ) {
        val (rankedSubtypes, unrankedSubtypes) = subtypes.orEmpty().partition { it.isRankValid() }
        rankedSubtypes.forEach { gameSubtype ->
            GameSubtypeListItem(gameSubtype)
        }
        unrankedSubtypes.forEach { gameSubtype ->
            Text(stringResource(R.string.unranked_prefix, gameSubtype.describeType(LocalContext.current)))
        }
        families?.filter { it.isRankValid() }?.forEach { gameFamily ->
            GameFamilyListItem(gameFamily)
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.primary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val voteCount = game?.numberOfRatings ?: 0
            val standardDeviation = game?.standardDeviation ?: 0.0
            Text(
                pluralStringResource(R.plurals.ratings_suffix, voteCount, voteCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (voteCount > 0) {
                Text(
                    stringResource(R.string.standard_deviation_prefix, standardDeviation),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun GameSubtypeListItem(
    gameSubtype: GameSubtype,
    modifier: Modifier = Modifier,
) {
    ListItem(
        rank = gameSubtype.rank,
        description = gameSubtype.describeType(LocalContext.current),
        rating = gameSubtype.bayesAverage,
        textStyle = MaterialTheme.typography.titleMedium,
        ratingStyle = RatingDefaults.textStyleLarge(),
        modifier = modifier,
    )
}

@Composable
private fun GameFamilyListItem(
    gameFamily: GameFamily,
    modifier: Modifier = Modifier,
) {
    ListItem(
        rank = gameFamily.rank,
        description = gameFamily.describeType(LocalContext.current),
        rating = gameFamily.bayesAverage,
        textStyle = MaterialTheme.typography.bodyMedium,
        ratingStyle = RatingDefaults.textStyleSmall(),
        modifier = modifier,
    )
}

@Composable
private fun ListItem(
    rank: Int,
    description: String,
    rating: Double,
    textStyle: TextStyle,
    ratingStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp)
    ) {
        if (rank != Game.RANK_UNKNOWN) {
            Text(
                text = stringResource(R.string.rank_prefix, rank),
                style = textStyle,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .width(40.dp),
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        } else {
            Spacer(modifier = Modifier.width(56.dp))
        }
        Text(
            text = description,
            style = textStyle,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Rating(
            rating,
            format = DecimalFormat("#0.000"),
            style = ratingStyle,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun GameRanksDialogPreview() {
    BggAppTheme {
        GameRanksDialog(
            Game(13, "Voidfall", numberOfRatings = 345, standardDeviation = 1.98),
            listOf(
                GameSubtype(
                    Game.Subtype.BoardGameExpansion,
                    42,
                    8.431,
                )
            ),
            listOf(
                GameFamily(
                    GameFamily.Family.Childrens,
                    42,
                    8.431,
                )
            ),
            modifier = Modifier.padding(
                horizontal = dimensionResource(R.dimen.material_margin_dialog),
                vertical = dimensionResource(R.dimen.material_margin_vertical),
            )
        )
    }
}
