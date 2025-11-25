package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentLocationsBinding
import com.boardgamegeek.databinding.RowLocationBinding
import com.boardgamegeek.entities.LocationEntity
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.viewmodel.LocationsViewModel
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration.SectionCallback
import kotlin.properties.Delegates

class LocationsFragment : Fragment() {
    private var _binding: FragmentLocationsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LocationsViewModel by lazy {
        ViewModelProvider(requireActivity()).get(LocationsViewModel::class.java)
    }

    private val adapter: LocationsAdapter by lazy {
        LocationsAdapter(viewModel)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLocationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter
        val sectionItemDecoration = RecyclerSectionItemDecoration(
                resources.getDimensionPixelSize(R.dimen.recycler_section_header_height),
                adapter)
        binding.recyclerView.addItemDecoration(sectionItemDecoration)

        viewModel.locations.observe(this, Observer {
            adapter.locations = it
            binding.progressBar?.hide()
            if (adapter.itemCount == 0) {
                binding.recyclerView.fadeOut()
                binding.emptyContainer.fadeIn()
            } else {
                binding.emptyContainer.fadeOut()
                binding.recyclerView.fadeIn(binding.recyclerView.windowToken != null)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class LocationsAdapter(val viewModel: LocationsViewModel) : RecyclerView.Adapter<LocationsAdapter.LocationsViewHolder>(), AutoUpdatableAdapter, SectionCallback {
        var locations: List<LocationEntity> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
            autoNotify(oldValue, newValue) { old, new ->
                old.name == new.name
            }
        }

        override fun getItemCount() = locations.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationsViewHolder {
            val binding = RowLocationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return LocationsViewHolder(binding)
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

        inner class LocationsViewHolder(private val binding: RowLocationBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(location: LocationEntity?) {
                location?.let { l ->
                    if (l.name.isBlank()) {
                        binding.nameView.setText(R.string.no_location)
                    } else {
                        binding.nameView.text = l.name
                    }
                    binding.quantityView.text = binding.root.resources.getQuantityString(R.plurals.plays_suffix, l.playCount, l.playCount)
                    binding.root.setOnClickListener {
                        LocationActivity.start(binding.root.context, l.name)
                    }
                }
            }
        }
    }
}
