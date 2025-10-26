package com.boardgamegeek.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.boardgamegeek.R
import com.boardgamegeek.model.GameDetail
import com.boardgamegeek.ui.theme.BggAppTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GameDetailsFlowRow(
    list: List<GameDetail>,
    headerText: String,
    moreButtonIconId: Int,
    modifier: Modifier = Modifier,
    limit: Int = 4,
    onItemClick: (GameDetail) -> Unit = { },
    onMoreClick: () -> Unit = { },
) {
    Column(modifier = modifier) {
        Text(
            headerText,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.Companion
                .heightIn(48.dp)
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .wrapContentHeight(Alignment.Companion.Bottom),
        )
        FlowRow(
            modifier = Modifier.Companion.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            val size = ButtonDefaults.MinHeight
            list.take(limit).forEach {
                OutlinedButton(
                    onClick = { onItemClick(it) },
                    modifier = Modifier.Companion.heightIn(min = size),
                    shape = ButtonDefaults.shapesFor(size).shape,
                    contentPadding = ButtonDefaults.contentPaddingFor(size),
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(it.thumbnailUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        placeholder = painterResource(id = R.drawable.thumbnail_image_empty),
                        error = painterResource(id = R.drawable.thumbnail_image_empty),
                        contentScale = ContentScale.Companion.Crop,
                        modifier = Modifier.Companion
                            .padding(end = ButtonDefaults.iconSpacingFor(size))
                            .size(ButtonDefaults.iconSizeFor(size))
                            .clip(MaterialTheme.shapes.extraSmall)
                    )
                    Text(
                        text = it.name,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            if (list.size > limit) {
                OutlinedButton(
                    onClick = onMoreClick,
                    modifier = Modifier.Companion.heightIn(min = size),
                    shape = ButtonDefaults.shapesFor(size).shape,
                    contentPadding = ButtonDefaults.contentPaddingFor(size),
                ) {
                    Icon(
                        painter = painterResource(id = moreButtonIconId),
                        contentDescription = null,
                        modifier = Modifier.Companion
                            .size(ButtonDefaults.iconSizeFor(size))
                    )
                    Spacer(Modifier.Companion.size(ButtonDefaults.iconSpacingFor(size)))
                    Text(
                        text = stringResource(R.string.more_suffix, list.size - limit),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 480)
@Composable
private fun GameLinkedItemsPreview() {
    BggAppTheme {
        GameDetailsFlowRow(
            listOf(
                GameDetail(13, "CATAN"),
                GameDetail(14, "Terra Mystica"),
                GameDetail(123, "Elder Scrolls: Betrayal of the Second Era"),
                GameDetail(9, "Root"),
                GameDetail(999, "You won't see this one")
            ),
            "Expansions",
            moreButtonIconId = R.drawable.ic_baseline_face_24,
            Modifier.fillMaxWidth(),
        )
    }
}