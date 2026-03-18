@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.boardgamegeek.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.extensions.LINK_AMAZON_COM
import com.boardgamegeek.extensions.LINK_AMAZON_DE
import com.boardgamegeek.extensions.LINK_AMAZON_UK
import com.boardgamegeek.extensions.linkAmazon
import com.boardgamegeek.extensions.linkBgPrices
import com.boardgamegeek.extensions.linkBgg
import com.boardgamegeek.extensions.linkEbay
import com.boardgamegeek.extensions.linkToBgg
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.theme.BggAppTheme

@Composable
fun GameLinksScreen(
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
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 24.dp, bottom = 4.dp),
                color = tint,
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
private fun GameLinksScreenPreview() {
    BggAppTheme {
        GameLinksScreen(13, "Terra Mystica", iconColor = -14667640)
    }
}