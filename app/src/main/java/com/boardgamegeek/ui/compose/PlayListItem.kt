package com.boardgamegeek.ui.compose

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.core.content.res.ResourcesCompat
import com.boardgamegeek.R
import com.boardgamegeek.model.Play
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.util.XmlApiMarkupConverter

@Composable
fun PlayListItem(
    play: Play,
    showGameName: Boolean,
    modifier: Modifier = Modifier,
    padding: PaddingValues = ListItemDefaults.tallPaddingValues,
    minimumHeight: Dp = ListItemDefaults.threeLineHeight,
    markupConverter: XmlApiMarkupConverter? = null,
    isSelected: Boolean = false,
    onLongClick: () -> Unit = {},
    onClick: () -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = modifier
            .semantics(mergeDescendants = true) {
                selected = isSelected
            }
            .fillMaxWidth()
            .heightIn(min = minimumHeight)
            .background(
                if (isSelected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    Color.Transparent
            )
            .then(
                if (isSelected)
                    Modifier.clickable(onClick = onClick)
                else
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick,
                    )
            )
            .padding(padding)
    ) {
        if (showGameName) {
            ListItemThumbnail(
                play.thumbnailUrl,
            )
        }
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
            modifier = modifier
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ListItemPrimaryText(
                    if (showGameName) play.gameName else play.dateForDisplay(LocalContext.current).toString(), isSelected = isSelected
                )
                @StringRes val statusMessageId = when {
                    play.deleteTimestamp > 0 -> R.string.sync_pending_delete
                    play.updateTimestamp > 0 -> R.string.sync_pending_update
                    play.dirtyTimestamp > 0 -> if (play.isSynced) R.string.sync_editing else R.string.sync_draft
                    else -> ResourcesCompat.ID_NULL
                }
                if (statusMessageId != ResourcesCompat.ID_NULL) {
                    ListItemTertiaryText(stringResource(statusMessageId), isSelected = isSelected)
                }
            }
            ListItemSecondaryText(play.describe(LocalContext.current, showGameName), isSelected = isSelected)
            val comments = markupConverter?.strip(play.comments.replace("\n", " ")) ?: play.comments
            if (comments.isNotBlank()) {
                ListItemSecondaryText(comments, isSelected = isSelected)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlayListItemPreview(
    @PreviewParameter(PlayPreviewParameterProvider::class) play: Play,
) {
    BggAppTheme {
        PlayListItem(
            play = play,
            showGameName = true,
            markupConverter = XmlApiMarkupConverter(LocalContext.current),
            isSelected = false,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlayListItemWithoutGameNamePreview(
    @PreviewParameter(PlayPreviewParameterProvider::class) play: Play,
) {
    BggAppTheme {
        PlayListItem(
            play = play,
            showGameName = false,
            markupConverter = XmlApiMarkupConverter(LocalContext.current),
            isSelected = false,
        )
    }
}
private class PlayPreviewParameterProvider : PreviewParameterProvider<Play> {
    override val values = sequenceOf(
        Play(
            internalId = INVALID_ID.toLong(),
            playId = INVALID_ID,
            dateInMillis = System.currentTimeMillis(),
            gameId = 13,
            gameName = "Catan",
            quantity = 1,
            length = 92,
            location = "House",
//    incomplete = false,
//    val noWinStats = false,
            comments = "Lots of 5s and 9s got rolled!",
//    val syncTimestamp: Long = 0L,
//    private val initialPlayerCount: Int = 0,
            dirtyTimestamp = 1L,
            updateTimestamp = 1L,
            deleteTimestamp = 0L,
//    val startTime: Long = 0L,
//    val imageUrl: String = "",
//    val thumbnailUrl: String = "",
//    val heroImageUrl: String = "",
//    val updatedPlaysTimestamp: Long = 0L,
//    val gameIsCustomSorted: Boolean = false,
//    val subtypes: List<String> = emptyList(),
//    private val _players: List<PlayPlayer>? = null,
        ),
    )
}
