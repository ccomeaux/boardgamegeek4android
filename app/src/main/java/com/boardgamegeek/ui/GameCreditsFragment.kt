package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentNestedComposeViewBinding
import com.boardgamegeek.model.GameDetail
import com.boardgamegeek.ui.compose.BggLoadingIndicatorBox
import com.boardgamegeek.ui.compose.EmptyContent
import com.boardgamegeek.ui.compose.GameFooter
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.GameViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GameCreditsFragment : Fragment() {
    private var _binding: FragmentNestedComposeViewBinding? = null
    private val binding get() = _binding!!

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentNestedComposeViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val limit = 4
        binding.composeView.setContent {
            val viewModel by activityViewModels<GameViewModel>()
            val isRefreshing by viewModel.gameIsRefreshing.observeAsState(true)
            val game by viewModel.game.observeAsState()
            val designers by viewModel.designers.observeAsState(emptyList())
            val artists by viewModel.artists.observeAsState(emptyList())
            val publishers by viewModel.publishers.observeAsState(emptyList())
            val mechanics by viewModel.mechanics.observeAsState(emptyList())
            val categories by viewModel.categories.observeAsState(emptyList())

            viewModel.refreshDesignerImages(limit)
            viewModel.refreshArtistImages(limit)
            viewModel.refreshPublisherImages(limit)

            if (isRefreshing)
                BggLoadingIndicatorBox()

            game?.let { game ->
                val color = Color(game.iconColor)
                Column(Modifier.padding(horizontal = dimensionResource(R.dimen.material_margin_horizontal), vertical = 8.dp)) {
                    GameCreditsFlowRow(
                        designers,
                        stringResource(R.string.designers),
                        R.drawable.designer_24px,
                        headerColor = color,
                        onItemClick = { designer ->
                            PersonActivity.startForDesigner(requireContext(), designer.id, designer.name)
                        },
                        onMoreClick = {
                            GameDetailActivity.start(
                                requireContext(),
                                getString(R.string.designers),
                                game.id,
                                game.name,
                                GameViewModel.ProducerType.DESIGNER,
                            )
                        }
                    )
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    GameCreditsFlowRow(
                        artists,
                        stringResource(R.string.artists),
                        R.drawable.artist_24px,
                        headerColor = color,
                        onItemClick = { artist ->
                            PersonActivity.startForArtist(requireContext(), artist.id, artist.name)
                        },
                        onMoreClick = {
                            GameDetailActivity.start(
                                requireContext(),
                                getString(R.string.artists),
                                game.id,
                                game.name,
                                GameViewModel.ProducerType.ARTIST,
                            )
                        }
                    )
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    GameCreditsFlowRow(
                        publishers,
                        stringResource(R.string.publishers),
                        R.drawable.publisher_24px,
                        headerColor = color,
                        onItemClick = { publisher ->
                            PersonActivity.startForPublisher(requireContext(), publisher.id, publisher.name)
                        },
                        onMoreClick = {
                            GameDetailActivity.start(
                                requireContext(),
                                getString(R.string.publishers),
                                game.id,
                                game.name,
                                GameViewModel.ProducerType.PUBLISHER,
                            )
                        }
                    )
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    GameCreditsFlowRow(
                        mechanics,
                        stringResource(R.string.mechanics),
                        R.drawable.mechanic_24px,
                        headerColor = color,
                        onItemClick = { mechanics ->
                            MechanicActivity.start(requireContext(), mechanics.id, mechanics.name)
                        },
                        onMoreClick = {
                            GameDetailActivity.start(
                                requireContext(),
                                getString(R.string.mechanics),
                                game.id,
                                game.name,
                                GameViewModel.ProducerType.MECHANIC,
                            )
                        }
                    )
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    GameCreditsFlowRow(
                        categories,
                        stringResource(R.string.categories),
                        R.drawable.category_24px,
                        headerColor = color,
                        onItemClick = { category ->
                            CategoryActivity.start(requireContext(), category.id, category.name)
                        },
                        onMoreClick = {
                            GameDetailActivity.start(
                                requireContext(),
                                getString(R.string.categories),
                                game.id,
                                game.name,
                                GameViewModel.ProducerType.CATEGORY,
                            )
                        }
                    )
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    GameFooter(
                        game.updated,
                        game.id,
                        Modifier.fillMaxSize()
                    )
                }
            } ?: EmptyContent(
                stringResource(R.string.empty_game),
                painterResource(R.drawable.game_24px),
                Modifier.fillMaxSize()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GameCreditsFlowRow(
    list: List<GameDetail>,
    headerText: String,
    moreButtonIconId: Int,
    modifier: Modifier = Modifier,
    limit: Int = 4,
    headerColor: Color = MaterialTheme.colorScheme.onSurface,
    @DrawableRes emptyIconResId: Int = R.drawable.person_image_empty,
    onItemClick: (GameDetail) -> Unit = { },
    onMoreClick: () -> Unit = { },
) {
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
            list.take(limit).forEach {
                OutlinedButton(
                    onClick = { onItemClick(it) },
                    modifier = Modifier.heightIn(min = buttonHeight),
                    shape = ButtonDefaults.shapesFor(buttonHeight).shape,
                    contentPadding = ButtonDefaults.contentPaddingFor(
                        buttonHeight,
                        hasStartIcon,
                    ), // TODO use PaddingValues(bottom = 6.dp, top = 6.dp, start = 12.dp, end = 12.dp) instead?
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
                        text = stringResource(R.string.more_suffix, list.size - limit),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 480)
@Composable
private fun GameLinkedItemsPreview() {
    BggAppTheme {
        GameCreditsFlowRow(
            listOf(
                GameDetail(13, "Reiner Knizia"),
                GameDetail(14, "Stephan Feld"),
                GameDetail(123, "Alexander Pfister"),
                GameDetail(9, "Uwe Rosenberg"),
                GameDetail(999, "You won't see this one")
            ),
            "Designers",
            moreButtonIconId = R.drawable.designer_24px,
            Modifier.fillMaxWidth(),
            // emptyIconResId = ResourcesCompat.ID_NULL,
        )
    }
}
