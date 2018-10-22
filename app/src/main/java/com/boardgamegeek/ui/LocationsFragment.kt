package com.boardgamegeek.ui

import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.events.LocationSortChangedEvent
import com.boardgamegeek.events.LocationsCountChangedEvent
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.provider.BggContract.Plays
import com.boardgamegeek.sorter.LocationsSorter
import com.boardgamegeek.sorter.LocationsSorterFactory
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.model.Location
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration.SectionCallback
import com.boardgamegeek.util.AnimationUtils
import com.boardgamegeek.util.fabric.SortEvent
import kotlinx.android.synthetic.main.fragment_locations.*
import kotlinx.android.synthetic.main.row_location.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.jetbrains.anko.support.v4.ctx
import kotlin.properties.Delegates

class LocationsFragment : Fragment(), LoaderManager.LoaderCallbacks<Cursor> {
    private var sorter: LocationsSorter? = null

    private val adapter: LocationsAdapter by lazy {
        LocationsAdapter(requireContext())
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setSort(savedInstanceState?.getInt(STATE_SORT_TYPE) ?: LocationsSorterFactory.TYPE_DEFAULT)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_locations, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter
        val sectionItemDecoration = RecyclerSectionItemDecoration(
                resources.getDimensionPixelSize(R.dimen.recycler_section_header_height),
                adapter)
        recyclerView.addItemDecoration(sectionItemDecoration)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_SORT_TYPE, sorter?.type ?: LocationsSorterFactory.TYPE_DEFAULT)
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe
    fun onEvent(event: LocationSortChangedEvent) {
        setSort(event.sortType)
    }

    private fun setSort(sortType: Int) {
        if ((sorter?.type ?: LocationsSorterFactory.TYPE_UNKNOWN) != sortType) {
            SortEvent.log("Locations", sortType.toString())
            sorter = LocationsSorterFactory.create(ctx, sortType)
            LoaderManager.getInstance(this).restartLoader(0, arguments, this)
        }
    }

    override fun onCreateLoader(id: Int, data: Bundle?): Loader<Cursor> {
        return CursorLoader(ctx, Plays.buildLocationsUri(), Location.PROJECTION, null, null, sorter?.orderByClause)
    }

    override fun onLoadFinished(loader: Loader<Cursor>, cursor: Cursor) {
        if (!isAdded) return

        val locations = arrayListOf<Location>()
        if (cursor.moveToFirst()) {
            do {
                locations.add(Location.fromCursor(cursor))
            } while (cursor.moveToNext())
        }

        adapter.locations = locations
        adapter.sorter = sorter ?: LocationsSorterFactory.create(ctx, LocationsSorterFactory.TYPE_DEFAULT)

        EventBus.getDefault().postSticky(LocationsCountChangedEvent(cursor.count))

        progressBar?.hide()
        setListShown(recyclerView.windowToken != null)
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        adapter.clear()
    }

    private fun setListShown(animate: Boolean) {
        if (adapter.itemCount == 0) {
            AnimationUtils.fadeOut(recyclerView)
            AnimationUtils.fadeIn(emptyContainer)
        } else {
            AnimationUtils.fadeOut(emptyContainer)
            AnimationUtils.fadeIn(recyclerView, animate)
        }
    }

    private class LocationsAdapter(context: Context) : RecyclerView.Adapter<LocationsAdapter.LocationsViewHolder>(), AutoUpdatableAdapter, SectionCallback {
        var locations: List<Location> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
            autoNotify(oldValue, newValue) { old, new ->
                old.name == new.name
            }
        }

        var sorter: LocationsSorter by Delegates.observable(LocationsSorterFactory.create(context, LocationsSorterFactory.TYPE_DEFAULT)) { _, oldValue, newValue ->
            if (oldValue.type != newValue.type) notifyDataSetChanged()
        }

        fun clear() {
            locations = emptyList()
        }

        override fun getItemCount() = locations.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationsViewHolder {
            return LocationsViewHolder(parent.inflate(R.layout.row_location))
        }

        override fun onBindViewHolder(holder: LocationsViewHolder, position: Int) {
            holder.bind(locations.getOrNull(position))
        }

        override fun isSection(position: Int): Boolean {
            if (locations.isEmpty()) return false
            if (position == 0) return true
            val thisLetter = sorter.getSectionText(locations.getOrNull(position))
            val lastLetter = sorter.getSectionText(locations.getOrNull(position - 1))
            return thisLetter != lastLetter
        }

        override fun getSectionHeader(position: Int): CharSequence {
            return if (locations.isEmpty()) "-" else sorter.getSectionText(locations[position])
        }

        inner class LocationsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            fun bind(location: Location?) {
                location?.let { l ->
                    if (l.name.isNullOrBlank()) {
                        itemView.nameView.setText(R.string.no_location)
                    } else {
                        itemView.nameView.text = l.name
                    }
                    itemView.quantityView.text = itemView.resources.getQuantityString(R.plurals.plays_suffix, l.playCount, l.playCount)
                    itemView.setOnClickListener {
                        LocationActivity.start(itemView.context, l.name)
                    }
                }
            }
        }
    }

    companion object {
        private const val STATE_SORT_TYPE = "sortType"
    }
}
