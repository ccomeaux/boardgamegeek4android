package com.boardgamegeek.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.boardgamegeek.R
import com.boardgamegeek.ui.theme.BggAppTheme

object ListItemDefaults {
    val verticalTextPadding = 4.dp
    val horizontalPadding = 16.dp
    val paddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    val tallPaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    val oneLineHeight = 56.dp
    val twoLineHeight = 72.dp
    val threeLineHeight = 88.dp
    val imageSize = 56.dp
    val secondaryImageSize = 18.dp
    @Composable
    fun secondaryTextStyle() = MaterialTheme.typography.bodyMedium
    @Composable
    fun primaryTextStyle() = MaterialTheme.typography.titleMedium
}

@Composable
fun ListItemIndex(index: Int, modifier: Modifier = Modifier, isWide: Boolean = false, isSelected: Boolean = false) {
    Text(
        text = index.toString(),
        style = MaterialTheme.typography.headlineSmall,
        color = getOnColor(isSelected),
        modifier = modifier
            .padding(end = ListItemDefaults.horizontalPadding)
            .width(if (isWide) 44.dp else 32.dp)
            .wrapContentWidth(Alignment.End)
    )
}

@Composable
fun ListItemThumbnail(url: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .crossfade(true)
            .build(),
        contentDescription = null,
        placeholder = painterResource(id = R.drawable.thumbnail_image_empty),
        error = painterResource(id = R.drawable.thumbnail_image_empty),
        contentScale = ContentScale.Crop,
        modifier = modifier
            .padding(end = ListItemDefaults.horizontalPadding)
            .size(ListItemDefaults.imageSize)
            .clip(MaterialTheme.shapes.extraSmall)
    )
}

@Composable
fun ListItemAvatar(url: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .crossfade(true)
            .build(),
        contentDescription = null,
        placeholder = painterResource(R.drawable.person_image_empty),
        error = painterResource(R.drawable.person_image_empty),
        contentScale = ContentScale.Crop,
        modifier = modifier
            .padding(end = ListItemDefaults.horizontalPadding)
            .size(ListItemDefaults.imageSize)
            .clip(CircleShape)
    )
}


@Composable
fun ListItemPrimaryText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false
) {
    Text(
        text = text,
        color = getOnColor(isSelected),
        modifier = modifier,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun ListItemPrimaryText(
    text: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = ListItemDefaults.primaryTextStyle(),
    isSelected: Boolean = false
) {
    Text(
        text = text,
        style = textStyle,
        color = getOnColor(isSelected),
        modifier = modifier,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun ListItemSecondaryText(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    contentDescription: String? = null,
    textStyle: TextStyle = ListItemDefaults.secondaryTextStyle(),
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
                    .padding(end = 4.dp)
                    .size(ListItemDefaults.secondaryImageSize),
                tint = getOnVariantColor(isSelected),
            )
        }
        Text(
            text = text,
            style = textStyle,
            color = getOnVariantColor(isSelected),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
    }
}

@Composable
fun ListItemTertiaryText(
    text: String,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = getOnVariantColor(isSelected),
        overflow = TextOverflow.Clip,
        maxLines = 1,
    )
}

@Composable
fun ListItemVerticalDivider(modifier: Modifier = Modifier) {
    VerticalDivider(
        modifier
            .height(ListItemDefaults.secondaryImageSize)
            .padding(horizontal = 8.dp)
    )
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
            ListItemSecondaryText("Favorite", icon = Icons.Outlined.Star)
            Row(modifier = Modifier.heightIn(max = ListItemDefaults.oneLineHeight)) {
                ListItemSecondaryText("Description")
                VerticalDivider()
                ListItemSecondaryText("Favorite", icon = Icons.Outlined.Star)
            }
        }
    }
}
