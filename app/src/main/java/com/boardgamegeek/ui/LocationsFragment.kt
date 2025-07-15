package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentLocationsBinding
import com.boardgamegeek.model.Location
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.compose.ListItemPrimaryText
import com.boardgamegeek.ui.compose.ListItemSecondaryText
import com.boardgamegeek.ui.compose.ListItemTokens
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.LocationsViewModel
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration.SectionCallback
import dagger.hilt.android.AndroidEntryPoint
import kotlin.properties.Delegates

@AndroidEntryPoint
class LocationsFragment : Fragment() {
    private var _binding: FragmentLocationsBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<LocationsViewModel>()
    private val adapter: LocationsAdapter by lazy { LocationsAdapter(viewModel) }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentLocationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addItemDecoration(
            RecyclerSectionItemDecoration(
                resources.getDimensionPixelSize(R.dimen.recycler_section_header_height),
                adapter
            )
        )

        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }

        viewModel.locations.observe(viewLifecycleOwner) {
            adapter.locations = it
            binding.recyclerView.isVisible = it.isNotEmpty()
            binding.emptyContainer.isVisible = it.isEmpty()
            binding.progressBar.hide()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.adapter = null
        _binding = null
    }

    private class LocationsAdapter(val viewModel: LocationsViewModel) :
        RecyclerView.Adapter<LocationsAdapter.LocationsViewHolder>(), AutoUpdatableAdapter, SectionCallback {
        var locations: List<Location> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
            autoNotify(oldValue, newValue) { old, new ->
                old.name == new.name
            }
        }

        override fun getItemCount() = locations.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationsViewHolder {
            return LocationsViewHolder(ComposeView(parent.context))
        }

        override fun onBindViewHolder(holder: LocationsViewHolder, position: Int) {
            locations.getOrNull(position)?.let { holder.bind(it) }
        }

        override fun isSection(position: Int): Boolean {
            if (position == RecyclerView.NO_POSITION) return false
            if (locations.isEmpty()) return false
            if (position == 0) return true
            val thisLetter = viewModel.getSectionHeader(locations.getOrNull(position))
            val lastLetter = viewModel.getSectionHeader(locations.getOrNull(position - 1))
            return thisLetter != lastLetter
        }

        override fun getSectionHeader(position: Int): CharSequence {
            return when {
                position == RecyclerView.NO_POSITION -> "-"
                locations.isEmpty() -> "-"
                else -> viewModel.getSectionHeader(locations.getOrNull(position))
            }
        }

        inner class LocationsViewHolder(private val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {
            fun bind(location: Location) {
                composeView.setContent {
                    LocationListItem(
                        location,
                        onClick = {
                            LocationActivity.start(itemView.context, location.name)
                        }
                    )
                }
            }
        }
    }
}


@Composable
private fun LocationListItem(
    location: Location,
    modifier: Modifier = Modifier,
    onClick: (Location) -> Unit = {},
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = { onClick(location) })
            .padding(ListItemTokens.paddingValues)
            .then(modifier)
    ) {
        ListItemPrimaryText(location.name)
        ListItemSecondaryText(pluralStringResource(R.plurals.plays_suffix, location.playCount, location.playCount))
    }
}

@Preview
@Composable
private fun LocationListItemPreview(
    @PreviewParameter(LocationPreviewParameterProvider::class) location: Location
) {
    BggAppTheme {
        LocationListItem(location)
    }
}

private class LocationPreviewParameterProvider : PreviewParameterProvider<Location> {
    override val values = sequenceOf(
        Location(
            name = "House",
            playCount = 256,
        ),
        Location(
            name = "Gulf Games",
            playCount = 0,
        ),
        Location(
            name = "Library",
            playCount = 1,
        )
    )
}
