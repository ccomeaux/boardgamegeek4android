package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import coil3.compose.AsyncImage
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentGeeklistItemsBinding
import com.boardgamegeek.model.GeekList
import com.boardgamegeek.model.GeekListItem
import com.boardgamegeek.model.Status
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.GeekListViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlin.properties.Delegates

@AndroidEntryPoint
class GeekListItemsFragment : Fragment() {
    private var _binding: FragmentGeeklistItemsBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<GeekListViewModel>()
    private val adapter: GeekListRecyclerViewAdapter by lazy { GeekListRecyclerViewAdapter() }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentGeeklistItemsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter

        viewModel.geekList.observe(viewLifecycleOwner) {
            it?.let { (status, data, message) ->
                if (status == Status.REFRESHING)
                    binding.progressView.show()
                else binding.progressView.hide()
                if (status == Status.ERROR)
                    setError(message)
                else {
                    val geekListItems = data?.items
                    if (geekListItems.isNullOrEmpty()) {
                        setError(getString(R.string.empty_geeklist))
                        binding.recyclerView.isVisible = false
                    } else {
                        setError(null)
                        adapter.geekList = data
                        adapter.geekListItems = geekListItems
                        binding.recyclerView.isVisible = true
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.adapter = null
        _binding = null
    }

    private fun setError(message: String?) {
        binding.emptyView.text = message
        binding.emptyView.isVisible = !message.isNullOrBlank()
    }

    class GeekListRecyclerViewAdapter :
        RecyclerView.Adapter<GeekListRecyclerViewAdapter.GeekListItemViewHolder>(), AutoUpdatableAdapter {
        var geekListItems: List<GeekListItem> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
            autoNotify(oldValue, newValue) { old, new ->
                old.objectId == new.objectId
            }
        }

        var geekList: GeekList? = null

        init {
            setHasStableIds(true)
        }

        override fun getItemCount(): Int = geekListItems.size

        override fun getItemId(position: Int) = geekListItems.getOrNull(position)?.id ?: RecyclerView.NO_ID

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GeekListItemViewHolder {
            return GeekListItemViewHolder(ComposeView(parent.context))
        }

        override fun onBindViewHolder(holder: GeekListItemViewHolder, position: Int) {
            geekListItems.getOrNull(position)?.let {
                holder.bind(it, position + 1)
            }
        }

        inner class GeekListItemViewHolder(private val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {
            fun bind(geekListItem: GeekListItem, order: Int) {
                composeView.setContent {
                    val context = LocalContext.current
                    BggAppTheme {
                        GeekListItemListItem(
                            order,
                            geekListItem,
                            geekList,
                            onClick = {
                                if (geekListItem.objectId != BggContract.INVALID_ID) {
                                    GeekListItemActivity.start(context, geekList!!, geekListItem, order)
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
    @PreviewParameter(GeekListPreviewParameterProvider::class) geekListItem: GeekListItem
) {
    BggAppTheme {
        GeekListItemListItem(
            100,
            geekListItem,
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

class GeekListPreviewParameterProvider : PreviewParameterProvider<GeekListItem> {
    override val values = sequenceOf(
        GeekListItem(
            id = 1,
            objectId = 31,
            objectName = "Gaia Project",
            username = "ccomeaux",
            thumbnailUrls = listOf("https://cf.geekdo-images.com/PyUol9QxBnZQCJqZI6bmSA__square/img/610c2mQNSggoh45dO3leJaLBruk=/75x75/filters:strip_icc()/pic8632666.png")
        ),
        GeekListItem(
            id = 12,
            objectId = 31,
            objectName = "Gaia Project",
            username = "author",
            thumbnailUrls = listOf("https://cf.geekdo-images.com/PyUol9QxBnZQCJqZI6bmSA__square/img/610c2mQNSggoh45dO3leJaLBruk=/75x75/filters:strip_icc()/pic8632666.png")
        ),
        GeekListItem(
            id = 13,
            objectId = 31,
            objectName = "No Image",
            username = "ccomeaux",
        ),
    )
}
