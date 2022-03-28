package com.boardgamegeek.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import com.boardgamegeek.extensions.setActionBarCount
import com.boardgamegeek.ui.viewmodel.BuddiesViewModel
import com.boardgamegeek.ui.viewmodel.BuddiesViewModel.SortType

class BuddiesActivity : TopLevelSinglePaneActivity() {
    private var numberOfBuddies = -1
    private var sortBy = SortType.USERNAME

    private val viewModel by viewModels<BuddiesViewModel>()

    override val firebaseContentType = "Buddies"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.buddies.observe(this) {
            numberOfBuddies = it?.data?.size ?: 0
            invalidateOptionsMenu()
        }
        viewModel.sort.observe(this) {
            sortBy = it.sortType
            invalidateOptionsMenu()
        }
    }

    override fun onCreatePane(): Fragment = BuddiesFragment()

    override val optionsMenuId = R.menu.buddies

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(
            when (sortBy) {
                SortType.USERNAME -> R.id.menu_sort_username
                SortType.FIRST_NAME -> R.id.menu_sort_first_name
                SortType.LAST_NAME -> R.id.menu_sort_last_name
            }
        )?.isChecked = true
        menu.setActionBarCount(R.id.menu_list_count, numberOfBuddies, title.toString())
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_sort_username -> viewModel.sort(SortType.USERNAME)
            R.id.menu_sort_first_name -> viewModel.sort(SortType.FIRST_NAME)
            R.id.menu_sort_last_name -> viewModel.sort(SortType.LAST_NAME)
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override val navigationItemId = R.id.geek_buddies
}
