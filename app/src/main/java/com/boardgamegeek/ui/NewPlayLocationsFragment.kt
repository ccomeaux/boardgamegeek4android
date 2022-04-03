package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentNewPlayLocationsBinding
import com.boardgamegeek.databinding.RowNewPlayLocationBinding
import com.boardgamegeek.entities.LocationEntity
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.viewmodel.NewPlayViewModel
import kotlin.properties.Delegates

class NewPlayLocationsFragment : Fragment() {
    private var _binding: FragmentNewPlayLocationsBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<NewPlayViewModel>()
    private val adapter: LocationsAdapter by lazy { LocationsAdapter(viewModel) }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentNewPlayLocationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter

        viewModel.locations.observe(viewLifecycleOwner) {
            adapter.locations = it
            binding.recyclerView.fadeIn()
            if (it.isEmpty()) {
                if (binding.filterEditText.text.isNullOrBlank()) {
                    binding.emptyView.setText(R.string.empty_new_play_locations)
                } else {
                    binding.emptyView.setText(R.string.empty_new_play_locations_filter)
                }
                binding.emptyView.fadeIn()
            } else {
                binding.emptyView.fadeOut()
            }
        }

        binding.filterEditText.doAfterTextChanged { viewModel.filterLocations(it.toString()) }

        binding.next.setOnClickListener {
            viewModel.setLocation(binding.filterEditText.text.toString())
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.setSubtitle(R.string.title_location)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class LocationsAdapter(private val viewModel: NewPlayViewModel) : RecyclerView.Adapter<LocationsAdapter.LocationsViewHolder>(),
        AutoUpdatableAdapter {
        var locations: List<LocationEntity> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
            autoNotify(oldValue, newValue) { old, new ->
                old.name == new.name
            }
        }

        override fun getItemCount() = locations.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationsViewHolder {
            return LocationsViewHolder(parent.inflate(R.layout.row_new_play_location))
        }

        override fun onBindViewHolder(holder: LocationsViewHolder, position: Int) {
            holder.bind(locations.getOrNull(position))
        }

        inner class LocationsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val binding = RowNewPlayLocationBinding.bind(itemView)

            fun bind(location: LocationEntity?) {
                location?.let { l ->
                    binding.nameView.text = l.name
                    itemView.setOnClickListener { viewModel.setLocation(l.name) }
                }
            }
        }
    }
}
