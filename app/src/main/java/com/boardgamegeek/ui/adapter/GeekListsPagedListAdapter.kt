package com.boardgamegeek.ui.adapter

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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
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
                val context = LocalContext.current
                BggAppTheme {
                    GeekListRow(
                        geekList,
                        {
                            GeekListActivity.start(context, geekList.id, geekList.title)
                        },
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
private fun GeekListRow(geekList: GeekList, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val dividerModifier = Modifier
        .size(18.dp)
        .padding(horizontal = 8.dp)
    val iconModifier = Modifier
        .size(18.dp)
        .padding(end = 8.dp)
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
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

@PreviewLightDark
@Composable
private fun GeekListRowPreview(
    @PreviewParameter(GeekListPreviewParameterProvider::class) geekList: GeekList
) {
    BggAppTheme {
        GeekListRow(
            geekList,
            {},
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

class GeekListPreviewParameterProvider : PreviewParameterProvider<GeekList> {
    override val values = sequenceOf(
        GeekList(
            id = 1,
            title = "Top 10 Games",
            username = "ccomeaux",
            numberOfItems = 42,
            numberOfThumbs = 11,
        ),
        GeekList(
            id = 2,
            title = "Short",
            username = "me2",
            numberOfItems = 0,
            numberOfThumbs = 0,
        ),
        GeekList(
            id = 3,
            title = "These are my Top 10 Games for the year of something, I don't know...",
            username = "ccomeaux",
            numberOfItems = 42,
            numberOfThumbs = 11,
        ),
    )
}
