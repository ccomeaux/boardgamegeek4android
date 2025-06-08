package com.boardgamegeek.ui.adapter

import android.content.res.Configuration
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.model.GeekList
import com.boardgamegeek.ui.GeekListActivity
import com.boardgamegeek.ui.theme.BggAppTheme

class GeekListsPagedListAdapter : PagingDataAdapter<GeekList, GeekListsPagedListAdapter.GeekListsViewHolder>(diffCallback) {
    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<GeekList>() {
            override fun areItemsTheSame(oldItem: GeekList, newItem: GeekList): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: GeekList, newItem: GeekList): Boolean = oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GeekListsViewHolder {
        return GeekListsViewHolder(ComposeView(parent.context))
    }

    override fun onBindViewHolder(holder: GeekListsViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }

    inner class GeekListsViewHolder(private val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {
        fun bind(geekList: GeekList) {
            composeView.setContent {
                BggAppTheme {
                    GeekListRow(
                        geekList,
                        Modifier
                            .padding(
                                horizontal = dimensionResource(R.dimen.material_margin_horizontal),
                                vertical = dimensionResource(R.dimen.material_margin_vertical),
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun GeekListRow(geekList: GeekList, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dividerModifier = Modifier
        .size(18.dp)
        .padding(horizontal = 8.dp)
    val iconModifier = Modifier
        .size(18.dp)
        .padding(end = 8.dp)
    Surface(
        color = MaterialTheme.colorScheme.surface,
        onClick = {
            GeekListActivity.start(context, geekList.id, geekList.title)
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)

    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = modifier.padding(vertical = 8.dp)
        ) {
            Text(
                text = geekList.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.AccountCircle,
                    contentDescription = null,
                    modifier = iconModifier
                )
                Text(
                    text = geekList.username,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                VerticalDivider(dividerModifier)
                Icon(
                    Icons.AutoMirrored.Outlined.List,
                    contentDescription = null,
                    modifier = iconModifier
                )
                Text(
                    text = geekList.numberOfItems.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                VerticalDivider(dividerModifier)
                Icon(
                    Icons.Outlined.ThumbUp,
                    contentDescription = null,
                    modifier = iconModifier
                )
                Text(
                    text = geekList.numberOfThumbs.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(backgroundColor = 0xFF00FF00, showBackground = true)
@Composable
private fun GeekListRowPreview() {
    BggAppTheme {
        GeekListRow(
            GeekList(
                id = 123,
                title = "Top 10 Games",
                username = "ccomeaux",
                numberOfItems = 42,
                numberOfThumbs = 11,
            ),
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Preview(backgroundColor = 0xFF00FF00, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GeekListRowPreviewDarkMode() {
    BggAppTheme {
        GeekListRow(
            GeekList(
                id = 123,
                title = "Top 10 Games",
                username = "ccomeaux",
                numberOfItems = 42,
                numberOfThumbs = 11,
            ),
            Modifier.padding(0.dp)
        )
    }
}

@Preview
@Composable
private fun GeekListRowPreviewLongTitle() {
    BggAppTheme {
        GeekListRow(
            GeekList(
                id = 123,
                title = "These are my Top 10 Games for the year of something, I don't know...",
                username = "ccomeaux",
                numberOfItems = 42,
                numberOfThumbs = 11,
            ),
            Modifier.padding(0.dp)
        )
    }
}
