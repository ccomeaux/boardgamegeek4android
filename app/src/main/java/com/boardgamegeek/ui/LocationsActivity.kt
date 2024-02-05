package com.boardgamegeek.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import com.boardgamegeek.extensions.setActionBarCount
import com.boardgamegeek.model.Location
import com.boardgamegeek.ui.viewmodel.LocationsViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LocationsActivity : SimpleSinglePaneActivity() {
    private val viewModel by viewModels<LocationsViewModel>()

    private var locationCount = -1
    private var sortType: Location.SortType? = null

    override val optionsMenuId: Int
        get() = R.menu.locations

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Locations")
            }
        }

        viewModel.locations.observe(this) {
            locationCount = it?.size ?: 0
            invalidateOptionsMenu()
        }
        viewModel.sortType.observe(this) {
            it?.let {
                sortType = it
                invalidateOptionsMenu()
            }
        }
    }

    override fun onCreatePane(intent: Intent): Fragment {
        return LocationsFragment()
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(
            when (sortType) {
                Location.SortType.NAME -> R.id.menu_sort_name
                Location.SortType.PLAY_COUNT -> R.id.menu_sort_quantity
                else -> View.NO_ID
            }
        )?.apply { isChecked = true }
        menu.setActionBarCount(R.id.menu_list_count, locationCount, title.toString())
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_sort_name -> viewModel.sort(Location.SortType.NAME)
            R.id.menu_sort_quantity -> viewModel.sort(Location.SortType.PLAY_COUNT)
            R.id.menu_refresh -> viewModel.refresh()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}
