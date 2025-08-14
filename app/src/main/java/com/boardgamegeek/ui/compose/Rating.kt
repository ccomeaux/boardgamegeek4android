package com.boardgamegeek.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.theme.BggAppTheme
import java.text.DecimalFormat

object RatingDefaults {
    val widthLarge = 56.dp
    val widthSmall = 48.dp
    @Composable
    fun textStyleLarge() = MaterialTheme.typography.labelLarge
    @Composable
    fun textStyleSmall() = MaterialTheme.typography.titleMedium

}

@Composable
fun Rating(
    rating: Double,
    modifier: Modifier = Modifier,
    format: DecimalFormat = DecimalFormat("#0.#"),
    style: TextStyle = RatingDefaults.textStyleLarge(),
    width: Dp = RatingDefaults.widthLarge,
) {
    val colorRgb = rating.toColor(BggColors.ratingColors)
    val shape = RoundedCornerShape(4.dp)
    Text(
        text = rating.asBoundedRating(LocalContext.current, format, R.string.unrated_abbr),
        color = Color(colorRgb.getTextColor()),
        textAlign = TextAlign.Center,
        modifier = modifier
            .widthIn(min = width)
            .background(Color(colorRgb), shape)
            .border(1.dp, Color(colorRgb.darkenColor(0.75)), shape),
        style = style,
        overflow = TextOverflow.Clip,
        maxLines = 1,
    )
}

@PreviewLightDark
@Composable
private fun RatingPreview(
    @PreviewParameter(RatingPreviewParameterProvider::class) rating: Double
) {
    BggAppTheme {
        Rating(
            rating = rating,
            modifier = Modifier.background(MaterialTheme.colorScheme.background)
        )
    }
}

private class RatingPreviewParameterProvider : PreviewParameterProvider<Double> {
    override val values = sequenceOf(
        0.0,
        1.0,
        2.0,
        3.0,
        4.0,
        5.0,
        6.0,
        7.0,
        8.0,
        9.0,
        10.0,
        8.5,
        6.49,
    )
}
