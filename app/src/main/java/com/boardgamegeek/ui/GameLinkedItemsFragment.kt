package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentNestedComposeViewBinding
import com.boardgamegeek.model.Game
import com.boardgamegeek.model.GameDetail
import com.boardgamegeek.ui.compose.BggLoadingIndicatorBox
import com.boardgamegeek.ui.compose.EmptyContent
import com.boardgamegeek.ui.compose.GameDetailsFlowRow
import com.boardgamegeek.ui.compose.GameFooter
import com.boardgamegeek.ui.viewmodel.GameViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GameLinkedItemsFragment : Fragment() {
    private var _binding: FragmentNestedComposeViewBinding? = null
    private val binding get() = _binding!!

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentNestedComposeViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.composeView.setContent {
            val viewModel by activityViewModels<GameViewModel>()
            val game by viewModel.game.observeAsState()
            val baseGames by viewModel.baseGames.observeAsState()
            val expansions by viewModel.expansions.observeAsState()
            GameLinkedItemsScreen(
                game,
                baseGames,
                expansions,
                PaddingValues(
                    horizontal = dimensionResource(R.dimen.material_margin_horizontal),
                    vertical = dimensionResource(R.dimen.material_margin_vertical),
                )
            )
        }
    }
}

@Composable
private fun GameLinkedItemsScreen(
    game: Game?,
    baseGames: List<GameDetail>?,
    expansions: List<GameDetail>?,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val count = (baseGames?.size ?: 0) + (expansions?.size ?: 0)
    when {
        (game == null || count == 0) -> {
            EmptyContent(
                stringResource(R.string.empty_game),
                painterResource(R.drawable.link_24px),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
        }
        else -> {
            Column(Modifier.padding(contentPadding)) {
                val context = LocalContext.current
                if (!baseGames.isNullOrEmpty()) {
                    val headerText = stringResource(R.string.base_games)
                    GameDetailsFlowRow(
                        list = baseGames,
                        headerText = headerText,
                        moreButtonIconId = R.drawable.ic_baseline_flip_to_front_24,
                        onItemClick = { GameActivity.start(context, it.id, it.name, it.thumbnailUrl) },
                        onMoreClick = {
                            GameDetailActivity.start(context, headerText, game.id, game.name, GameViewModel.ProducerType.BASE_GAME)
                        }
                    )
                }
                if (!expansions.isNullOrEmpty()) {
                    val headerText = stringResource(R.string.expansions)
                    GameDetailsFlowRow(
                        list = expansions,
                        headerText = headerText,
                        moreButtonIconId = R.drawable.ic_baseline_flip_to_front_24,
                        onItemClick = { GameActivity.start(context, it.id, it.name, it.thumbnailUrl) },
                        onMoreClick = {
                            GameDetailActivity.start(context, headerText, game.id, game.name, GameViewModel.ProducerType.EXPANSION)
                        }
                    )
                }
                Spacer(Modifier.heightIn(8.dp))
                HorizontalDivider()
                GameFooter(game.updated, game.id, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
