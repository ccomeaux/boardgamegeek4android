package com.boardgamegeek.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import com.boardgamegeek.events.LocationSortChangedEvent
import com.boardgamegeek.events.LocationsCountChangedEvent
import com.boardgamegeek.extensions.setActionBarCount
import com.boardgamegeek.sorter.LocationsSorterFactory
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.ContentViewEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class LocationsActivity : SimpleSinglePaneActivity() {
    private var locationCount = -1
    private var sortType = LocationsSorterFactory.TYPE_DEFAULT

    override val optionsMenuId: Int
        get() = R.menu.locations

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            Answers.getInstance().logContentView(ContentViewEvent().putContentType("Locations"))
        }
    }

    override fun onCreatePane(intent: Intent): Fragment {
        return LocationsFragment()
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        if (sortType == LocationsSorterFactory.TYPE_QUANTITY) {
            menu.findItem(R.id.menu_sort_quantity)?.isChecked = true
        } else {
            menu.findItem(R.id.menu_sort_name)?.isChecked = true
        }
        menu.setActionBarCount(R.id.menu_list_count, locationCount)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_sort_name -> {
                EventBus.getDefault().postSticky(LocationSortChangedEvent(LocationsSorterFactory.TYPE_NAME))
                return true
            }
            R.id.menu_sort_quantity -> {
                EventBus.getDefault().postSticky(LocationSortChangedEvent(LocationsSorterFactory.TYPE_QUANTITY))
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @Subscribe(sticky = true)
    fun onEvent(event: LocationsCountChangedEvent) {
        locationCount = event.count
        invalidateOptionsMenu()
    }

    @Subscribe(sticky = true)
    fun onEvent(event: LocationSortChangedEvent) {
        sortType = event.sortType
    }
}
