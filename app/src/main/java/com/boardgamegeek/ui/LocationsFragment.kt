package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentLocationsBinding
import com.boardgamegeek.databinding.RowLocationBinding
import com.boardgamegeek.entities.LocationEntity
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
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
        _binding = null
    }

    private class LocationsAdapter(val viewModel: LocationsViewModel) :
        RecyclerView.Adapter<LocationsAdapter.LocationsViewHolder>(), AutoUpdatableAdapter, SectionCallback {
        var locations: List<LocationEntity> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
            autoNotify(oldValue, newValue) { old, new ->
                old.name == new.name
            }
        }

        override fun getItemCount() = locations.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationsViewHolder {
            return LocationsViewHolder(parent.inflate(R.layout.row_location))
        }

        override fun onBindViewHolder(holder: LocationsViewHolder, position: Int) {
            holder.bind(locations.getOrNull(position))
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

        inner class LocationsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val binding = RowLocationBinding.bind(itemView)

            fun bind(location: LocationEntity?) {
                location?.let { l ->
                    binding.nameView.text = l.name.ifBlank { itemView.context.getString(R.string.no_location) }
                    binding.quantityView.text = itemView.resources.getQuantityString(R.plurals.plays_suffix, l.playCount, l.playCount)
                    itemView.setOnClickListener {
                        LocationActivity.start(itemView.context, l.name)
                    }
                }
            }
        }
    }
}
