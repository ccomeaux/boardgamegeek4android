package com.boardgamegeek.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.extensions.toggle
import com.boardgamegeek.model.CollectionStatus
import com.boardgamegeek.ui.theme.BggAppTheme

@Composable
fun AddCollectionItemDialog(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit = {},
    onConfirmation: (selectedStatuses: Set<CollectionStatus>, wishlistPriority: Int) -> Unit = { _, _ -> },
) {
    val statuses = remember { mutableStateSetOf<CollectionStatus>() }
    var wishlistPriority by remember { mutableIntStateOf(0) }

    @Composable
    fun CheckboxRow(
        text: String,
        status: CollectionStatus,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(40.dp)) {
            Checkbox(
                checked = statuses.contains(status),
                onCheckedChange = {
                    statuses.toggle(status, it)
                },
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(text)
        }
    }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = { onDismissRequest() },
        confirmButton = {
            TextButton(onClick = {
                onConfirmation(statuses, wishlistPriority)
            }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismissRequest() }) { Text(stringResource(R.string.cancel)) }
        },
        title = { Text(text = stringResource(R.string.title_add_a_copy)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                CheckboxRow(
                    stringResource(R.string.collection_status_own),
                    CollectionStatus.Own,
                )
                CheckboxRow(
                    stringResource(R.string.collection_status_prev_owned),
                    CollectionStatus.PreviouslyOwned,

                    )
                CheckboxRow(
                    stringResource(R.string.collection_status_for_trade),
                    CollectionStatus.ForTrade,
                )
                CheckboxRow(
                    stringResource(R.string.collection_status_want_to_play),
                    CollectionStatus.WantToPlay,
                )
                CheckboxRow(
                    stringResource(R.string.collection_status_want_in_trade),
                    CollectionStatus.WantInTrade,
                )
                CheckboxRow(
                    stringResource(R.string.collection_status_want_to_buy),
                    CollectionStatus.WantToBuy,
                )
                CheckboxRow(
                    stringResource(R.string.collection_status_preordered),
                    CollectionStatus.Preordered,
                )
                CheckboxRow(
                    stringResource(R.string.collection_status_wishlist),
                    CollectionStatus.Wishlist,
                )
                AnimatedVisibility(statuses.contains(CollectionStatus.Wishlist)) {
                    Column(modifier = Modifier.selectableGroup()) {
                        val wishlistPriorities = stringArrayResource(R.array.wishlist_priority_finite)
                        wishlistPriorities.forEachIndexed { index, description ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(40.dp)) {
                                RadioButton(
                                    selected = ((index + 1) == wishlistPriority),
                                    onClick = { wishlistPriority = (index + 1) },
                                    modifier = Modifier.padding(start = 40.dp, end = 8.dp),
                                )
                                Text(description)
                            }
                        }
                    }
                }
            }
        }
    )
}

@Preview
@Composable
private fun AddCollectionItemDialogPreview() {
    BggAppTheme {
        AddCollectionItemDialog()
    }
}
