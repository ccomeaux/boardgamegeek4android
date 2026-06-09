package com.boardgamegeek.ui.collectionshelf

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.boardgamegeek.extensions.BggColors
import com.boardgamegeek.extensions.asWishListPriority
import com.boardgamegeek.extensions.toColor

@Composable
fun SmallBadge(text: String, backgroundColor: Color = MaterialTheme.colorScheme.surface) {
    val shape = RoundedCornerShape(4.dp)
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier
            .widthIn(min = 48.dp, max = 90.dp)
            .background(backgroundColor, shape)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .padding(horizontal = 4.dp),
        overflow = TextOverflow.Clip,
        maxLines = 1,
    )
}

@Preview
@Composable
private fun SmallBadgePreview() {
    SmallBadge("Sep 1, 1973")
}

@Preview
@Composable
private fun SmallBadgeWishlistPreview() {
    val level = 1
    SmallBadge(
        level.asWishListPriority(LocalContext.current),
        Color(level.toDouble().toColor(BggColors.fiveStageColors)),
    )
}
