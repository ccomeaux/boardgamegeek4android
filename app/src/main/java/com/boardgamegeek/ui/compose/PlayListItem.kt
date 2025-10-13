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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
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
    markupConverter: XmlApiMarkupConverter,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onLongClick: () -> Unit = {},
    onClick: () -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ListItemDefaults.threeLineHeight)
            .background(
                if (isSelected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surface
            )
            .then(
                if (isSelected)
                    Modifier.Companion.clickable(onClick = onClick)
                else
                    Modifier.Companion.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick,
                    )
            )
            .padding(ListItemDefaults.tallPaddingValues)
    ) {
        ListItemThumbnail(
            play.thumbnailUrl,
        )
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Companion.Start,
            modifier = modifier
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.Companion.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Companion.CenterVertically
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
            val comments = markupConverter.strip(play.comments.replace("\n", " "))
            if (comments.isNotBlank()) {
                ListItemSecondaryText(comments, isSelected = isSelected)
            }
        }
    }
}

@Preview
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

private class PlayPreviewParameterProvider : PreviewParameterProvider<Play> {
    override val values = sequenceOf(
        Play(
            internalId = INVALID_ID.toLong(),
            playId = INVALID_ID,
            dateInMillis = System.currentTimeMillis(),
            gameId = 13,
            gameName = "CATAN",
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
