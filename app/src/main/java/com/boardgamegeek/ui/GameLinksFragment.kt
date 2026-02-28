package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentNestedComposeViewBinding
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.compose.EmptyContent
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.GameViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GameLinksFragment : Fragment() {
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
            val paddingValues = PaddingValues(dimensionResource(R.dimen.material_margin_horizontal), 8.dp)
            if (game == null) {
                EmptyContent(
                    stringResource(R.string.empty_game),
                    painterResource(R.drawable.link_24px),
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
            } else {
                game?.let {
                    GameLinks(
                        it.id,
                        it.name,
                        iconColor = it.iconColor,
                        modifier = Modifier.padding(paddingValues),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GameLinks(
    gameId: Int,
    gameName: String,
    modifier: Modifier = Modifier,
    iconColor: Int = android.graphics.Color.TRANSPARENT,
) {
    val context = LocalContext.current
    val tint = if (iconColor == android.graphics.Color.TRANSPARENT)
        LocalContentColor.current
    else
        Color(iconColor)
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (gameId != BggContract.INVALID_ID) {
            LinkButton(
                textResId = R.string.link_geekbuddy_analysis,
                painterResource(R.drawable.geekbuddy_24px),
                tint = tint,
                onClick = { context.linkToBgg("geekbuddy/analyze/thing", gameId) }
            )
            LinkButton(
                textResId = R.string.link_bgg,
                painterResource(R.drawable.open_in_browser_24px),
                tint = tint,
                onClick = { context.linkBgg(gameId) }
            )
        }
        if (gameName.isNotBlank()) {
            Text(
                stringResource(R.string.title_acquire),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)
            )
            LinkButton(
                textResId = R.string.link_bg_prices,
                painterResource(R.drawable.shopping_cart_24px),
                tint = tint,
                onClick = { context.linkBgPrices(gameName) }
            )
            LinkButton(
                textResId = R.string.link_amazon,
                painterResource(R.drawable.amazon_24px),
                tint = tint,
                onClick = { context.linkAmazon(gameName, LINK_AMAZON_COM) }
            )
            LinkButton(
                textResId = R.string.link_amazon_uk,
                painterResource(R.drawable.amazon_24px),
                tint = tint,
                onClick = { context.linkAmazon(gameName, LINK_AMAZON_UK) }
            )
            LinkButton(
                textResId = R.string.link_amazon_de,
                painterResource(R.drawable.amazon_24px),
                tint = tint,
                onClick = { context.linkAmazon(gameName, LINK_AMAZON_DE) }
            )
            LinkButton(
                textResId = R.string.link_ebay,
                painterResource(R.drawable.ebay_24px),
                tint = tint,
                onClick = { context.linkEbay(gameName) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LinkButton(
    textResId: Int,
    icon: Painter,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val size = ButtonDefaults.MediumContainerHeight
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(size),
        shape = ButtonDefaults.shapesFor(size).shape,
        contentPadding = ButtonDefaults.contentPaddingFor(size, true),
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.iconSizeFor(size)),
            tint = tint,
        )
        Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(size)))
        Text(
            stringResource(textResId),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GameLinksPreview() {
    BggAppTheme {
        GameLinks(13, "Terra Mystica", iconColor = -14667640)
    }
}