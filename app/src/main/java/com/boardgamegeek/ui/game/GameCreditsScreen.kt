@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.boardgamegeek.ui.game

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.boardgamegeek.R
import com.boardgamegeek.model.Game
import com.boardgamegeek.model.GameDetail
import com.boardgamegeek.ui.theme.BggAppTheme

@Composable
fun GameCreditsFlowRow(
    list: List<GameDetail>,
    headerText: String,
    moreButtonIconId: Int,
    modifier: Modifier = Modifier,
    limit: Int = 4,
    headerColor: Color = MaterialTheme.colorScheme.onSurface,
    @DrawableRes emptyIconResId: Int = R.drawable.person_image_empty,
    @StringRes emptyStringResId: Int = R.string.no_information,
    onItemClick: (GameDetail) -> Unit = { },
    onMoreClick: () -> Unit = { },
) {
    if (list.isEmpty()) {
        Text(
            stringResource(emptyStringResId),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    } else {
        Column(modifier = modifier) {
            Text(
                headerText,
                style = MaterialTheme.typography.titleLarge,
                color = headerColor,
                modifier = Modifier
                    .heightIn(48.dp)
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .wrapContentHeight(Alignment.Bottom),
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                val buttonHeight = ButtonDefaults.MinHeight
                val hasStartIcon = (emptyIconResId != ResourcesCompat.ID_NULL)
                val partialList = if (list.size > limit) list.take(limit - 1) else list
                partialList.forEach {
                    OutlinedButton(
                        onClick = { onItemClick(it) },
                        modifier = Modifier.heightIn(min = buttonHeight),
                        shape = ButtonDefaults.shapesFor(buttonHeight).shape,
                        contentPadding = PaddingValues(bottom = 6.dp, top = 6.dp, start = 12.dp, end = 12.dp)
//                        contentPadding = ButtonDefaults.contentPaddingFor(
//                            buttonHeight,
//                            hasStartIcon,
//                        ),
                    ) {
                        if (hasStartIcon) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(it.thumbnailUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                placeholder = painterResource(id = emptyIconResId),
                                error = painterResource(id = emptyIconResId),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .padding(end = ButtonDefaults.iconSpacingFor(buttonHeight))
                                    .size(ButtonDefaults.iconSizeFor(buttonHeight))
                                    .clip(CircleShape)
                            )
                        }
                        Text(
                            text = it.name,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                if (list.size > limit) {
                    OutlinedButton(
                        onClick = onMoreClick,
                        modifier = Modifier.heightIn(min = buttonHeight),
                        shape = ButtonDefaults.shapesFor(buttonHeight).shape,
                        contentPadding = ButtonDefaults.contentPaddingFor(buttonHeight, true),
                    ) {
                        Icon(
                            painter = painterResource(id = moreButtonIconId),
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.iconSizeFor(buttonHeight))
                        )
                        Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(buttonHeight)))
                        Text(
                            text = stringResource(R.string.more_suffix, list.size - limit + 1),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GameCreditsScreen(
    game: Game,
    designers: List<GameDetail>,
    artists: List<GameDetail>,
    publishers: List<GameDetail>,
    mechanics: List<GameDetail>,
    categories: List<GameDetail>,
    modifier: Modifier = Modifier,
    onDesignerClick: (GameDetail) -> Unit = {},
    onDesignersClick: () -> Unit = {},
    onArtistClick: (GameDetail) -> Unit = {},
    onArtistsClick: () -> Unit = {},
    onPublisherClick: (GameDetail) -> Unit = {},
    onPublishersClick: () -> Unit = {},
    onMechanicClick: (GameDetail) -> Unit = {},
    onMechanicsClick: () -> Unit = {},
    onCategoryClick: (GameDetail) -> Unit = {},
    onCategoriesClick: () -> Unit = {},
) {
    val iconColor = game.iconColor
    val color = if (iconColor != android.graphics.Color.TRANSPARENT) Color(iconColor) else MaterialTheme.colorScheme.onSurface
    Column(
        modifier.verticalScroll(rememberScrollState())
    ) {
        GameCreditsFlowRow(
            designers,
            stringResource(R.string.designers),
            R.drawable.designer_24px,
            headerColor = color,
            emptyStringResId = R.string.empty_designers,
            onItemClick = { designer -> onDesignerClick(designer) },
            onMoreClick = onDesignersClick,
        )
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        GameCreditsFlowRow(
            artists,
            stringResource(R.string.artists),
            R.drawable.artist_24px,
            headerColor = color,
            emptyStringResId = R.string.empty_artists,
            onItemClick = { artist -> onArtistClick(artist) },
            onMoreClick = onArtistsClick,
        )
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        GameCreditsFlowRow(
            publishers,
            stringResource(R.string.publishers),
            R.drawable.publisher_24px,
            headerColor = color,
            emptyStringResId = R.string.empty_publishers,
            onItemClick = { publisher -> onPublisherClick(publisher) },
            onMoreClick = onPublishersClick,
        )
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        GameCreditsFlowRow(
            mechanics,
            stringResource(R.string.mechanics),
            R.drawable.mechanic_24px,
            headerColor = color,
            emptyIconResId = ResourcesCompat.ID_NULL,
            emptyStringResId = R.string.empty_mechanics,
            onItemClick = { mechanic -> onMechanicClick(mechanic) },
            onMoreClick = onMechanicsClick,
        )
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        GameCreditsFlowRow(
            categories,
            stringResource(R.string.categories),
            R.drawable.category_24px,
            headerColor = color,
            emptyIconResId = ResourcesCompat.ID_NULL,
            emptyStringResId = R.string.empty_categories,
            onItemClick = { category -> onCategoryClick(category) },
            onMoreClick = onCategoriesClick,
        )
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        ThingFooter(
            game.updated,
            game.id,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GameLinkedItemsPreview() {
    BggAppTheme {
        Column {
            GameCreditsScreen(
                Game(1, "Game 1"),
                listOf(
                    GameDetail(13, "Reiner Knizia"),
                    GameDetail(14, "Stephan Feld"),
                    GameDetail(123, "Alexander Pfister"),
                    GameDetail(9, "Uwe Rosenberg"),
                    GameDetail(999, "You won't see this one")
                ),
                listOf(
                    GameDetail(13, "Franz Vohwinkel"),
                    GameDetail(14, "Ian O'Toole"),
                ),
                emptyList(),
                listOf(
                    GameDetail(1, "Mechanic 1"),
                    GameDetail(2, "Mechanic 2"),
                    GameDetail(3, "Mechanic 3"),
                    GameDetail(4, "Mechanic 4"),
                ),
                emptyList(),
            )
        }
    }
}
