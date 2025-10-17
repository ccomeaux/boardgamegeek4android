package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.GameViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

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

        val viewModel by activityViewModels<GameViewModel>()
        viewModel.game.observe(viewLifecycleOwner) {
            it?.let { game ->
                binding.composeView.setContent {
                    GameLinks(
                        game.id,
                        game.name,
                        iconColor = game.iconColor,
                        modifier = Modifier.padding(
                            horizontal = dimensionResource(R.dimen.material_margin_horizontal),
                            vertical = 8.dp,
                        )
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
    Timber.i("CPC iconColor = ${iconColor}; tint = ${tint.toArgb()}")
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (gameId != BggContract.INVALID_ID) {
            LinkButton(
                textResId = R.string.link_geekbuddy_analysis,
                Icons.Filled.Person,
                tint = tint,
                onClick = { context.linkToBgg("geekbuddy/analyze/thing", gameId) }
            )
            LinkButton(
                textResId = R.string.link_bgg,
                Icons.Filled.OpenInBrowser,
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
                Icons.Filled.AttachMoney,
                tint = tint,
                onClick = { context.linkBgPrices(gameName) }
            )
            LinkButton(
                textResId = R.string.link_amazon,
                R.drawable.ic_action_amazon,
                tint = tint,
                onClick = { context.linkAmazon(gameName, LINK_AMAZON_COM) }
            )
            LinkButton(
                textResId = R.string.link_amazon_uk,
                R.drawable.ic_action_amazon,
                tint = tint,
                onClick = { context.linkAmazon(gameName, LINK_AMAZON_UK) }
            )
            LinkButton(
                textResId = R.string.link_amazon_de,
                R.drawable.ic_action_amazon,
                tint = tint,
                onClick = { context.linkAmazon(gameName, LINK_AMAZON_DE) }
            )
            LinkButton(
                textResId = R.string.link_ebay,
                Icons.Filled.Gavel,
                tint = tint,
                onClick = { context.linkEbay(gameName) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LinkButton(
    textResId: Int,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val size = ButtonDefaults.MediumContainerHeight
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(size),
        shape = ButtonDefaults.shapesFor(size).shape,
        contentPadding = ButtonDefaults.contentPaddingFor(size),
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LinkButton(
    textResId: Int,
    imageResId: Int,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val size = ButtonDefaults.MediumContainerHeight
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(size),
        shape = ButtonDefaults.shapesFor(size).shape,
        contentPadding = ButtonDefaults.contentPaddingFor(size),
    ) {
        Icon(
            painterResource(imageResId),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(ButtonDefaults.iconSizeFor(size))
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