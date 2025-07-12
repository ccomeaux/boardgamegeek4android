package com.boardgamegeek.ui.adapter

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.extensions.asYear
import com.boardgamegeek.model.TopGame
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.GameActivity
import com.boardgamegeek.ui.compose.*
import com.boardgamegeek.ui.theme.BggAppTheme
import kotlin.properties.Delegates

class TopGamesAdapter : RecyclerView.Adapter<TopGamesAdapter.ViewHolder>(), AutoUpdatableAdapter {
    init {
        setHasStableIds(true)
    }

    var results: List<TopGame> by Delegates.observable(emptyList()) { _, old, new ->
        autoNotify(old, new) { o, n ->
            o.id == n.id
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ComposeView(parent.context))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        results.getOrNull(position)?.let { holder.bind(it) }
    }

    override fun getItemCount() = results.size

    override fun getItemId(position: Int) = (results.getOrNull(position)?.id ?: BggContract.INVALID_ID).toLong()

    inner class ViewHolder(private val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {
        fun bind(game: TopGame) {
            composeView.setContent {
                BggAppTheme {
                    TopGameListItem(
                        game,
                        onClick = {
                            GameActivity.start(
                                composeView.context,
                                game.id,
                                game.name,
                                game.thumbnailUrl,
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TopGameListItem(
    topGame: TopGame,
    modifier: Modifier = Modifier,
    onClick: (topGame: TopGame) -> Unit = {},
) {
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = { onClick(topGame) })
            .padding(ListItemTokens.tallPaddingValues)
            .then(modifier)
    ) {
        ListItemIndex(topGame.rank, isWide = true)
        ListItemThumbnail(topGame.thumbnailUrl)
        Column {
            ListItemPrimaryText(topGame.name)
            ListItemSecondaryText(
                topGame.yearPublished.asYear(LocalContext.current),
                modifier = modifier.padding(bottom = ListItemTokens.verticalTextPadding),
                icon = Icons.Outlined.CalendarToday,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun TopGameListItemPreview(
    @PreviewParameter(TopGamePreviewParameterProvider::class) topGame: TopGame,
) {
    BggAppTheme {
        TopGameListItem(topGame, Modifier)
    }
}

private class TopGamePreviewParameterProvider : PreviewParameterProvider<TopGame> {
    override val values = sequenceOf(
        TopGame(
            rank = 1,
            id = 99,
            name = "Spirit Island",
            thumbnailUrl = "",
            yearPublished = 2019,
        ),
        TopGame(
            rank = 22,
            id = 99,
            name = "Star Wars: the Deck Building Game",
            thumbnailUrl = "",
            yearPublished = 2023,
        ),
        TopGame(
            rank = 50,
            id = 99,
            name = "Sky Team",
            thumbnailUrl = "",
            yearPublished = 2022,
        )
    )
}
