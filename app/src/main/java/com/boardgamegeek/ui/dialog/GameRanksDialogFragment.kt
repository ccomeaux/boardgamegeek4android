package com.boardgamegeek.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
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
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.DialogGameRanksBinding
import com.boardgamegeek.extensions.createThemedBuilder
import com.boardgamegeek.extensions.showAndSurvive
import com.boardgamegeek.model.Game
import com.boardgamegeek.model.GameFamily
import com.boardgamegeek.model.GameSubtype
import com.boardgamegeek.ui.compose.Rating
import com.boardgamegeek.ui.compose.RatingDefaults
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.GameViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.DecimalFormat

@AndroidEntryPoint
class GameRanksDialogFragment : DialogFragment() {
    private var _binding: DialogGameRanksBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<GameViewModel>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogGameRanksBinding.inflate(layoutInflater)
        val builder = requireContext().createThemedBuilder()
            .setView(binding.root)
            .setTitle(R.string.title_ranks_ratings)
        return builder.create()
    }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setTitle(R.string.title_ranks_ratings)

        binding.composeView.setContent {
            BggAppTheme {
                val subtypes by viewModel.subtypes.observeAsState()
                val families by viewModel.families.observeAsState()
                val game by viewModel.game.observeAsState()
                Column(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .verticalScroll(rememberScrollState())
                        .padding(
                            horizontal = dimensionResource(R.dimen.material_margin_dialog),
                            vertical = dimensionResource(R.dimen.material_margin_vertical),
                        )
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
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun launch(host: Fragment) {
            host.showAndSurvive(GameRanksDialogFragment())
        }
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
        labelStyle = RatingDefaults.textStyleLarge(),
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
        labelStyle = RatingDefaults.textStyleSmall(),
        modifier = modifier,
    )
}

@Composable
private fun ListItem(
    rank: Int,
    description: String,
    rating: Double,
    textStyle: TextStyle,
    labelStyle: TextStyle,
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
            style = labelStyle,
        )
    }
}

@Preview
@Composable
private fun GameSubtypeListItemPreview() {
    BggAppTheme {
        GameSubtypeListItem(
            GameSubtype(
                Game.Subtype.BoardGameExpansion,
                42,
                8.431,
            )
        )
    }
}

@Preview
@Composable
private fun GameFamilyListItemPreview() {
    BggAppTheme {
        GameFamilyListItem(
            GameFamily(
                GameFamily.Family.Childrens,
                42,
                8.431,
            )
        )
    }
}