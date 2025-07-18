package com.boardgamegeek.ui.adapter

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.extensions.firstChar
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.ui.GameActivity
import com.boardgamegeek.ui.adapter.BuddyCollectionAdapter.BuddyGameViewHolder
import com.boardgamegeek.ui.compose.ListItemDefaults
import com.boardgamegeek.ui.compose.ListItemPrimaryText
import com.boardgamegeek.ui.compose.ListItemSecondaryText
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration.SectionCallback
import kotlin.properties.Delegates

class BuddyCollectionAdapter : RecyclerView.Adapter<BuddyGameViewHolder>(), AutoUpdatableAdapter, SectionCallback {
    var items: List<CollectionItem> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
        autoNotify(oldValue, newValue) { old, new ->
            old.collectionId == new.collectionId
        }
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BuddyGameViewHolder {
        return BuddyGameViewHolder(ComposeView(parent.context))
    }

    override fun onBindViewHolder(holder: BuddyGameViewHolder, position: Int) {
        items.getOrNull(position)?.let { holder.bind(it) }
    }

    inner class BuddyGameViewHolder(private val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {
        fun bind(item: CollectionItem) {
            composeView.setContent {
                BggAppTheme {
                    CollectionRow(item) {
                        GameActivity.start(itemView.context, item.gameId, item.gameName)
                    }
                }
            }
        }
    }

    override fun isSection(position: Int): Boolean {
        if (position == RecyclerView.NO_POSITION) return false
        if (items.isEmpty()) return false
        if (position == 0) return true
        val thisLetter = items.getOrNull(position)?.sortName.firstChar()
        val lastLetter = items.getOrNull(position - 1)?.sortName.firstChar()
        return thisLetter != lastLetter
    }

    override fun getSectionHeader(position: Int): CharSequence {
        return when {
            position == RecyclerView.NO_POSITION -> return "-"
            items.isEmpty() -> return "-"
            position < 0 || position >= items.size -> "-"
            else -> items[position].sortName.firstChar()
        }
    }
}

@Composable
private fun CollectionRow(
    collectionItem: CollectionItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ListItemDefaults.twoLineHeight)
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(ListItemDefaults.paddingValues)
    ) {
        ListItemPrimaryText(collectionItem.gameName)
        ListItemSecondaryText(collectionItem.gameId.toString())
    }
}

@PreviewLightDark
@Composable
private fun CollectionRowPreview(
    @PreviewParameter(CollectionItemPreviewParameterProvider::class) collectionItem: CollectionItem,
) {
    BggAppTheme {
        CollectionRow(collectionItem, Modifier)
    }
}

private class CollectionItemPreviewParameterProvider : PreviewParameterProvider<CollectionItem> {
    override val values = sequenceOf(
        CollectionItem(
            gameId = 13,
            gameName = "CATAN",
            rank = 1,
        ),
    )
}

