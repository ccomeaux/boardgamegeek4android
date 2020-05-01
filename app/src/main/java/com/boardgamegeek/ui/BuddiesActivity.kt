package com.boardgamegeek.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.boardgamegeek.R
import com.boardgamegeek.extensions.setActionBarCount
import com.boardgamegeek.ui.viewmodel.BuddiesViewModel

class BuddiesActivity : TopLevelSinglePaneActivity() {
    private var numberOfBuddies = -1
    private var sortBy = BuddiesViewModel.SortType.USERNAME

    private val viewModel by viewModels<BuddiesViewModel>()

    override val answersContentType = "Buddies"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.buddies.observe(this, Observer {
            numberOfBuddies = it?.data?.size ?: 0
            invalidateOptionsMenu()
        })
        viewModel.sort.observe(this, Observer {
            sortBy = it.sortType
            invalidateOptionsMenu()
        })
    }

    override fun onCreatePane(): Fragment = BuddiesFragment()

    override val optionsMenuId = R.menu.buddies

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        when (sortBy) {
            BuddiesViewModel.SortType.USERNAME -> menu.findItem(R.id.menu_sort_username)
            BuddiesViewModel.SortType.FIRST_NAME -> menu.findItem(R.id.menu_sort_first_name)
            BuddiesViewModel.SortType.LAST_NAME -> menu.findItem(R.id.menu_sort_last_name)
        }.apply {
            isChecked = true
            menu.setActionBarCount(R.id.menu_list_count, numberOfBuddies, title.toString())
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_sort_username -> {
                viewModel.sort(BuddiesViewModel.SortType.USERNAME)
                return true
            }
            R.id.menu_sort_first_name -> {
                viewModel.sort(BuddiesViewModel.SortType.FIRST_NAME)
                return true
            }
            R.id.menu_sort_last_name -> {
                viewModel.sort(BuddiesViewModel.SortType.LAST_NAME)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override val navigationItemId = R.id.geek_buddies
}
