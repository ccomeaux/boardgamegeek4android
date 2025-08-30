package com.boardgamegeek.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.boardgamegeek.extensions.asColorRgb
import com.boardgamegeek.extensions.darkenColor
import com.boardgamegeek.extensions.isColorDark
import com.boardgamegeek.ui.theme.BggAppTheme

object ColorBoxDefaults {
    val sizeSmall = 24.dp
    val sizeMedium = 40.dp
    val sizeLarge = 64.dp
}

@Composable
fun ColorBox(
    colorName: String,
    modifier: Modifier = Modifier,
    size: Dp = ColorBoxDefaults.sizeMedium,
    content: @Composable BoxScope.(Color) -> Unit,
) {
    val colorRgb = colorName.asColorRgb()
    Box(
        modifier = modifier
            .padding(4.dp)
            .size(size)
            .clip(CircleShape)
            .background(Color(colorRgb))
            .border(1.dp, Color(colorRgb.darkenColor(0.75)), CircleShape)
    ) {
        content(if (colorRgb.isColorDark()) Color.White else Color.Black)
    }
}

@Preview
@Composable
private fun ColoBoxPreview() {
    BggAppTheme {
        ColorBox("red", size = ColorBoxDefaults.sizeMedium) {
            Text("X", color = it, modifier = Modifier.align(Alignment.Center))
        }
    }
}