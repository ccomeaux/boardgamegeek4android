package com.boardgamegeek.ui.collectionshelf

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.boardgamegeek.R
import com.boardgamegeek.extensions.asYear
import com.boardgamegeek.mappers.mapToResId
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.CollectionStatus
import com.boardgamegeek.model.Game
import com.boardgamegeek.ui.compose.SmallRating
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.CollectionDetailsViewModel

enum class DialogType {
    AddRating,
    AddComment,
    RemoveStatus,
    Acquire,
    AddTradeCondition,
    Trade,
}

@Composable
fun CollectionItemCard(
    item: CollectionItem,
    modifier: Modifier = Modifier,
    rating: Double = Game.UNRATED,
    comment: String = "",
    acquiredFromList: List<String> = emptyList(),
    onClick: () -> Unit = {},
    onAcquire: ((CollectionDetailsViewModel.AcquisitionInfo) -> Unit)? = null,
    onLogPlay: (() -> Unit)? = null,
    onRateClick: ((Double) -> Unit)? = null,
    onCommentClick: ((String) -> Unit)? = null,
    onRemoveStatusClick: ((CollectionStatus) -> Unit)? = null,
    onOfferForTradeClick: (() -> Unit)? = null,
    onTradeClick: (() -> Unit)? = null,
    onAddTradeConditionClick: ((String) -> Unit)? = null,
    statusToRemove: CollectionStatus? = null,
    badge: @Composable () -> Unit = {},
) {
    var showOverflowMenu by remember { mutableStateOf(false) }
    var dialogType by remember { mutableStateOf<DialogType?>(null) }
    val hasOverflowOptions by remember {
        derivedStateOf {
            onAcquire != null ||
                    onLogPlay != null ||
                    onRateClick != null ||
                    onCommentClick != null ||
                    onRemoveStatusClick != null ||
                    onOfferForTradeClick != null ||
                    onTradeClick != null
        }
    }
    Card(
        modifier = modifier
            .padding(2.dp)
            .width(96.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    ) {
        Column {
            Box {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.robustThumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = stringResource(id = R.string.thumbnail),
                    placeholder = painterResource(id = R.drawable.thumbnail_image_empty),
                    error = painterResource(id = R.drawable.thumbnail_image_empty),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(96.dp)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(1.dp),
                ) {
                    badge()
                }
            }

            Text(
                text = item.robustName,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .padding(top = 4.dp),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.yearPublished.asYear(LocalContext.current),
                    modifier = Modifier
                        .padding(start = 4.dp, bottom = 4.dp)
                        .weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (hasOverflowOptions) {
                    Box {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.overflow_menu_24px),
                                contentDescription = stringResource(id = R.string.more),
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                        ) {
                            onAcquire?.let {
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.title_acquire)) },
                                    onClick = {
                                        showOverflowMenu = false
                                        dialogType = DialogType.Acquire
                                    },
                                )
                            }
                            onLogPlay?.let {
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.menu_log_play)) },
                                    onClick = {
                                        showOverflowMenu = false
                                        // TODO show "are you sure" for quick plays
                                        onLogPlay()
                                    },
                                )
                            }
                            onRateClick?.let {
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.menu_rate_item)) },
                                    onClick = {
                                        showOverflowMenu = false
                                        dialogType = DialogType.AddRating
                                    },
                                )
                            }
                            onCommentClick?.let {
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.menu_comment_item)) },
                                    onClick = {
                                        showOverflowMenu = false
                                        dialogType = DialogType.AddComment
                                    },
                                )
                            }
                            onRemoveStatusClick?.let {
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.menu_remove_status)) },
                                    onClick = {
                                        showOverflowMenu = false
                                        dialogType = DialogType.RemoveStatus
                                    },
                                )
                            }
                            onOfferForTradeClick?.let {
                                DropdownMenuItem(
                                    text = { Text("Add status") },
                                    onClick = {
                                        showOverflowMenu = false
                                        onOfferForTradeClick()
                                    },
                                )
                            }
                            onTradeClick?.let {
                                dialogType = DialogType.Trade
                            }
                            onAddTradeConditionClick?.let {
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.menu_add_condition_text)) },
                                    onClick = {
                                        showOverflowMenu = false
                                        dialogType = DialogType.AddTradeCondition
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    dialogType?.let {
        when (it) {
            DialogType.AddRating -> {
                AddRatingDialog(
                    gameName = item.robustName,
                    rating = rating,
                    onDismiss = { dialogType = null },
                    onConfirmation = { rating ->
                        dialogType = null
                        onRateClick?.let { lambda -> lambda(rating) }
                    }
                )
            }
            DialogType.AddComment -> {
                AddCommentDialog(
                    gameName = item.robustName,
                    comment = comment,
                    onDismiss = { dialogType = null },
                    onConfirmation = { comment ->
                        dialogType = null
                        onCommentClick?.let { lambda -> lambda(comment) }
                    }
                )
            }
            DialogType.AddTradeCondition -> {
                AddTextDialog(
                    label = stringResource(R.string.trade_condition),
                    gameName = item.robustName,
                    initialText = comment,
                    onDismiss = { dialogType = null },
                    onConfirmation = { text ->
                        dialogType = null
                        onAddTradeConditionClick?.let { lambda -> lambda(text) }
                    }
                )
            }
            DialogType.Acquire -> {
                AcquireDialog(
                    item = item,
                    acquiredFromList = acquiredFromList,
                    onDismiss = { dialogType = null },
                    onConfirmation = { info ->
                        dialogType = null
                        onAcquire?.let { lambda -> lambda(info) }
                    }
                )
            }
            DialogType.Trade -> {
                AlertDialog(
                    onDismissRequest = { dialogType = null },
                    title = { Text(item.robustName) },
                    text = { Text(stringResource(R.string.msg_confirm_trade)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onTradeClick?.let { lambda -> lambda() }
                            }
                        ) {
                            Text(stringResource(R.string.yes))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { dialogType = null }) {
                            Text(stringResource(R.string.no))
                        }
                    }
                )
            }
            DialogType.RemoveStatus -> {
                val statusString = statusToRemove?.let { status ->
                    stringResource(status.mapToResId())
                } ?: ""
                AlertDialog(
                    onDismissRequest = { dialogType = null },
                    title = { Text(item.robustName) },
                    text = { Text(stringResource(R.string.msg_remove_status, statusString)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (statusToRemove != null)
                                    onRemoveStatusClick?.let { lambda -> lambda(statusToRemove) }
                            }
                        ) {
                            Text(stringResource(R.string.yes))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { dialogType = null }) {
                            Text(stringResource(R.string.no))
                        }
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CollectionItemCardRatingPreview() {
    BggAppTheme {
        CollectionItemCard(
            item = collectionItem(),
            badge = {
                SmallRating(8.5)
            }
        )
    }
}


@Preview(showBackground = true)
@Composable
private fun CollectionItemCardDatePreview() {
    BggAppTheme {
        CollectionItemCard(
            item = collectionItem(),
            badge = {
                val shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                Text(
                    text = "Apr 13, 2025",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .widthIn(min = 48.dp, max = 90.dp)
                        .background(MaterialTheme.colorScheme.surface, shape)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                        .padding(horizontal = 4.dp),
                    overflow = TextOverflow.Clip,
                    maxLines = 1,
                )
            }
        )
    }
}

@Composable
private fun collectionItem(): CollectionItem = CollectionItem(
    collectionId = 123,
    collectionName = "Gaia Project",
    thumbnailUrl = "",
    collectionYearPublished = 2017,
)