package com.boardgamegeek.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.boardgamegeek.R
import com.boardgamegeek.ui.theme.BggAppTheme

object ListItemTokens {
    val verticalTextPadding = 4.dp
    val paddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    val tallPaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
}

@Composable
fun ListItemIndex(index: Int, modifier: Modifier = Modifier, isWide: Boolean = false, isSelected: Boolean = false) {
    Text(
        text = index.toString(),
        style = MaterialTheme.typography.headlineSmall,
        color = getOnColor(isSelected),
        modifier = modifier
            .padding(end = 16.dp)
            .width(if (isWide) 44.dp else 32.dp)
            .wrapContentWidth(Alignment.End)
    )
}

@Composable
fun ListItemThumbnail(url: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        placeholder = painterResource(id = R.drawable.thumbnail_image_empty),
        error = painterResource(id = R.drawable.thumbnail_image_empty),
        modifier = modifier.padding(end = 16.dp).size(56.dp)
    )
}

@Composable
fun ListItemPrimaryText(text: String, modifier: Modifier = Modifier, isSelected: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = getOnColor(isSelected),
        modifier = modifier,
        maxLines = 2,
    )
}

@Composable
fun ListItemSecondaryText(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    contentDescription: String? = null,
    isSelected: Boolean = false,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = contentDescription,
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 8.dp),
                tint = getOnVariantColor(isSelected),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = getOnVariantColor(isSelected),
        )
    }
}

@Composable
private fun getOnColor(isSelected: Boolean) =
    if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

@Composable
private fun getOnVariantColor(isSelected: Boolean) =
    if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun ListItemPreviews() {
    BggAppTheme {
        Column {
            ListItemIndex(88, isWide = false)
            ListItemIndex(100, isWide = true)
            ListItemThumbnail("")
            ListItemPrimaryText("Title")
            ListItemSecondaryText("Description")
            ListItemSecondaryText("Description", icon = Icons.Outlined.Star)
        }
    }
}
