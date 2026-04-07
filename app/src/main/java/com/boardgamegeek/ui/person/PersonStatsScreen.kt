@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.boardgamegeek.ui.person

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.model.GIndex
import com.boardgamegeek.model.HIndex
import com.boardgamegeek.model.PersonStats
import com.boardgamegeek.model.Person.Type
import com.boardgamegeek.ui.compose.EmptyContent
import com.boardgamegeek.ui.compose.Rating
import com.boardgamegeek.ui.theme.BggAppTheme
import java.text.DecimalFormat

@Composable
fun PersonStatsScreen(
    stats: PersonStats?,
    type: Type,
    modifier: Modifier = Modifier,
    onModifyCollectionStatus: () -> Unit = {},
    showCollectionStatusButton: Boolean = true,
) {
    Box(modifier = modifier) {
        when (stats) {
            null -> {
                EmptyContent(
                    text = stringResource(R.string.empty_person_stats, type.mapToDescription().lowercase(LocalLocale.current.platformLocale)),
                    iconPainter = painterResource(R.drawable.play_stats_24px),
                    modifier = modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                )
            }
            else -> {
                PersonStatsContent(
                    stats = stats,
                    type = type,
                    showCollectionStatusButton = showCollectionStatusButton,
                    onModifyCollectionStatus = onModifyCollectionStatus,
                )
            }
        }
    }
}

@Composable
private fun PersonStatsContent(
    stats: PersonStats,
    type: Type,
    showCollectionStatusButton: Boolean,
    modifier: Modifier = Modifier,
    onModifyCollectionStatus: () -> Unit = {},
) {
    var showModifyDialog by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        RatingStatRow(
            label = stringResource(R.string.average_rating),
            value = stats.averageRating,
        )

        Spacer(modifier = Modifier.height(24.dp))

        TextStatRow(
            label = stringResource(R.string.whitmore_score),
            value = stats.whitmoreScore.toString(),
            tooltipText = stringResource(R.string.whitmore_score_info, type.mapToDescription().lowercase(LocalLocale.current.platformLocale)),
        )

        if (stats.whitmoreScore != stats.whitmoreScoreWithExpansions) {
            TextStatRow(
                label = stringResource(R.string.whitmore_score_with_expansions),
                value = stats.whitmoreScoreWithExpansions.toString()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextStatRow(
            label = stringResource(R.string.play_count),
            value = stats.playCount.toString()
        )

        TextStatRow(
            label = stringResource(R.string.h_index),
            value = stats.hIndex.description,
            tooltipText = pluralStringResource(
                R.plurals.person_game_h_index_info,
                stats.hIndex.h,
                stats.hIndex.h,
                stats.hIndex.n,
            ),
        )

        TextStatRow(
            label = stringResource(R.string.g_index),
            value = stats.gIndex.description,
            tooltipText = pluralStringResource(
                R.plurals.person_game_g_index_info,
                stats.gIndex.g,
                stats.gIndex.g,
                (stats.gIndex.g * stats.gIndex.g),
            ),
        )

        TextStatRow(
            label = stringResource(R.string.pearson),
            value = DecimalFormat("0.00").format(stats.pearson),
            tooltipText = stringResource(R.string.pearson_info),
        )

        if (showCollectionStatusButton) {
            Spacer(modifier = Modifier.size(24.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.person_stat_collection_status),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.size(8.dp))
                val size = ButtonDefaults.MinHeight
                FilledTonalButton(
                    onClick = { showModifyDialog = true },
                    modifier = modifier.heightIn(size),
                    shape = ButtonDefaults.shapesFor(size).shape,
                    contentPadding = ButtonDefaults.contentPaddingFor(size)
                ) {
                    Text(text = stringResource(R.string.modify))
                }
            }
        }
    }
    if (showModifyDialog) {
        AlertDialog(
            onDismissRequest = { showModifyDialog = false },
            title = { Text(stringResource(R.string.title_modify_collection_status)) },
            text = { Text(stringResource(R.string.msg_modify_collection_status)) },
            dismissButton = {
                TextButton(onClick = {
                    showModifyDialog = false
                }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onModifyCollectionStatus()
                    showModifyDialog = false
                }) {
                    Text(stringResource(R.string.modify))
                }
            },
        )
    }
}

@Composable
private fun RatingStatRow(
    label: String,
    value: Double,
    modifier: Modifier = Modifier,
    tooltipText: String? = null,
) {
    StatRow(
        label = label,
        modifier = modifier,
        tooltipText = tooltipText,
    ) {
        Rating(rating = value)
    }
}

@Composable
private fun TextStatRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    tooltipText: String? = null,
) {
    StatRow(
        label = label,
        modifier = modifier,
        tooltipText = tooltipText,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(end = 16.dp),
        )
    }
}

@Composable
private fun StatRow(
    label: String,
    modifier: Modifier = Modifier,
    tooltipText: String? = null,
    valueContent: @Composable RowScope.() -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (tooltipText != null) {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.End),
                tooltip = {
                    PlainTooltip { Text(tooltipText) }
                },
                state = rememberTooltipState(isPersistent = true)
            ) {
                Icon(
                    painter = painterResource(R.drawable.help_24px),
                    contentDescription = stringResource(R.string.title_info),
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(16.dp),
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        valueContent.invoke(this)
    }
}

@Preview(backgroundColor = 0xFFFFFFFF, showBackground = true, widthDp = 320, heightDp = 320)
@Composable
private fun PersonStatsScreenPreview() {
    BggAppTheme {
        PersonStatsScreen(
            stats = PersonStats(
                averageRating = 7.79,
                whitmoreScore = 42,
                whitmoreScoreWithExpansions = 73,
                playCount = 7,
                hIndex = HIndex(6, 7),
                gIndex = GIndex(8),
                pearson = 0.767,
            ),
            type = Type.Designer,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(backgroundColor = 0xFFFFFFFF, showBackground = true, widthDp = 320, heightDp = 320)
@Composable
private fun PersonStatsScreenEmptyPreview() {
    BggAppTheme {
        PersonStatsScreen(
            null,
            type = Type.Designer,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
