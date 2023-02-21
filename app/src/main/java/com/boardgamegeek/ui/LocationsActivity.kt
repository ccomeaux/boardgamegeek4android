package com.boardgamegeek.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import com.boardgamegeek.extensions.setActionBarCount
import com.boardgamegeek.ui.viewmodel.LocationsViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LocationsActivity : SimpleSinglePaneActivity() {
    private val viewModel by viewModels<LocationsViewModel>()

    private var locationCount = -1
    private var sortType = LocationsViewModel.SortType.NAME

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
        viewModel.sort.observe(this) {
            sortType = it?.sortType ?: LocationsViewModel.SortType.NAME
            invalidateOptionsMenu()
        }
    }

    override fun onCreatePane(intent: Intent): Fragment {
        return LocationsFragment()
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(
            when (sortType) {
                LocationsViewModel.SortType.NAME -> R.id.menu_sort_name
                LocationsViewModel.SortType.PLAY_COUNT -> R.id.menu_sort_quantity
            }
        )?.apply { isChecked = true }
        menu.setActionBarCount(R.id.menu_list_count, locationCount, title.toString())
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_sort_name -> viewModel.sort(LocationsViewModel.SortType.NAME)
            R.id.menu_sort_quantity -> viewModel.sort(LocationsViewModel.SortType.PLAY_COUNT)
            R.id.menu_refresh -> viewModel.refresh()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}
