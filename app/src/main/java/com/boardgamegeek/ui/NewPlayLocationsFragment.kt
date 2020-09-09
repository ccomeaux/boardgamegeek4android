package com.boardgamegeek.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.entities.LocationEntity
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.viewmodel.NewPlayViewModel
import kotlinx.android.synthetic.main.fragment_forums.recyclerView
import kotlinx.android.synthetic.main.fragment_new_play_locations.*
import kotlinx.android.synthetic.main.row_new_play_location.view.*
import kotlin.properties.Delegates

class NewPlayLocationsFragment : Fragment(R.layout.fragment_new_play_locations) {
    private val viewModel by activityViewModels<NewPlayViewModel>()

    private val adapter: LocationsAdapter by lazy {
        LocationsAdapter(viewModel)
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.setSubtitle(R.string.title_location)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter

        viewModel.locations.observe(viewLifecycleOwner, {
            adapter.locations = it
            recyclerView.fadeIn()
            if (it.isEmpty()) {
                if (filterView.text.isNullOrBlank()) {
                    emptyView.setText(R.string.empty_new_play_locations)
                } else {
                    emptyView.setText(R.string.empty_new_play_locations_filter)
                }
                emptyView.fadeIn()
            } else {
                emptyView.fadeOut()
            }
        })

        filterView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.filterLocations(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

        next.setOnClickListener {
            viewModel.setLocation(filterView.text.toString())
        }
    }

    private class LocationsAdapter(private val viewModel: NewPlayViewModel) : RecyclerView.Adapter<LocationsAdapter.LocationsViewHolder>(), AutoUpdatableAdapter {
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

        inner class LocationsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            fun bind(location: LocationEntity?) {
                location?.let { l ->
                    itemView.nameView.text = l.name
                    itemView.setOnClickListener { viewModel.setLocation(l.name) }
                }
            }
        }
    }
}