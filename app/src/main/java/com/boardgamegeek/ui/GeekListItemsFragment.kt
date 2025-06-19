package com.boardgamegeek.ui

import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import coil3.compose.AsyncImage
import com.boardgamegeek.R
import com.boardgamegeek.model.GeekList
import com.boardgamegeek.model.GeekListItem
import com.boardgamegeek.model.RefreshableResource
import com.boardgamegeek.model.Status
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.compose.LoadingIndicator
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.GeekListViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GeekListItemsFragment : Fragment(R.layout.fragment_geeklist_description) {
    private val viewModel by activityViewModels<GeekListViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ComposeView>(R.id.composeView)?.apply {
            setContent {
                val context = LocalContext.current
                val geekListState = viewModel.geekList.observeAsState(RefreshableResource.refreshing(null))
                BggAppTheme {
                    if (geekListState.value.status == Status.ERROR) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = geekListState.value.message,
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                    } else {
                        val geekList = geekListState.value.data
                        val geekListItems = geekList?.items
                        if (geekListItems.isNullOrEmpty()) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Text(
                                    text = stringResource(R.string.empty_geeklist),
                                    modifier = Modifier.align(Alignment.Center),
                                )
                            }
                        } else {
                            LazyColumn {
                                itemsIndexed(geekListItems) { index, geekListItem ->
                                    GeekListItemListItem(
                                        index + 1,
                                        geekListItem,
                                        geekList,
                                        onClick = {
                                            if (geekListItem.objectId != BggContract.INVALID_ID) {
                                                GeekListItemActivity.start(context, geekList, geekListItem, index + 1)
                                            }
                                        },
                                        modifier = Modifier
                                            .padding(
                                                horizontal = dimensionResource(R.dimen.material_margin_horizontal),
                                                vertical = dimensionResource(R.dimen.material_margin_vertical),
                                            )
                                    )
                                }
                            }
                        }
                        if (geekListState.value.status == Status.REFRESHING) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                LoadingIndicator(
                                    Modifier
                                        .align(Alignment.Center)
                                        .padding(dimensionResource(R.dimen.padding_extra))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GeekListItemListItem(order: Int, geekListItem: GeekListItem, geekList: GeekList?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val iconModifier = Modifier
        .size(18.dp)
        .padding(end = 8.dp)
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        onClick = onClick
    ) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = order.toString(),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .widthIn(min = 40.dp)
                    .wrapContentWidth(Alignment.End)
            )
            AsyncImage(
                model = geekListItem.thumbnailUrls?.first(), // TODO iterate through thumbnails?
                contentDescription = null,
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.thumbnail_image_empty),
                error = painterResource(id = R.drawable.thumbnail_image_empty),
                modifier = Modifier.size(56.dp)
            )
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(
                    text = geekListItem.objectName,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (geekListItem.username != geekList?.username) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.AccountCircle,
                            contentDescription = null,
                            modifier = iconModifier,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = geekListItem.username,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun GeekListItemListItemPreview(
    @PreviewParameter(GeekListPreviewParameterProvider::class) geekListItem: Pair<GeekListItem, Int>,
) {
    BggAppTheme {
        GeekListItemListItem(
            geekListItem.second,
            geekListItem.first,
            GeekList(
                id = 123,
                title = "My GeekList",
                username = "ccomeaux",
                description = "This is a description",
            ),
            {},
            Modifier.padding(
                horizontal = 16.dp,
                vertical = 8.dp,
            )
        )
    }
}

class GeekListPreviewParameterProvider : PreviewParameterProvider<Pair<GeekListItem, Int>> {
    override val values = sequenceOf(
        GeekListItem(
            id = 1,
            objectId = 31,
            objectName = "Gaia Project",
            username = "ccomeaux",
            thumbnailUrls = listOf("https://cf.geekdo-images.com/PyUol9QxBnZQCJqZI6bmSA__square/img/610c2mQNSggoh45dO3leJaLBruk=/75x75/filters:strip_icc()/pic8632666.png")
        ) to 1,
        GeekListItem(
            id = 12,
            objectId = 31,
            objectName = "Gaia Project",
            username = "author",
            thumbnailUrls = listOf("https://cf.geekdo-images.com/PyUol9QxBnZQCJqZI6bmSA__square/img/610c2mQNSggoh45dO3leJaLBruk=/75x75/filters:strip_icc()/pic8632666.png")
        ) to 99,
        GeekListItem(
            id = 13,
            objectId = 31,
            objectName = "No Image",
            username = "ccomeaux",
        ) to 100,
    )
}
