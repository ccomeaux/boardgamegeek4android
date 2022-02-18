package com.boardgamegeek.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import com.boardgamegeek.extensions.setActionBarCount
import com.boardgamegeek.ui.viewmodel.MechanicsViewModel
import com.boardgamegeek.ui.viewmodel.MechanicsViewModel.SortType

class MechanicsActivity : SimpleSinglePaneActivity() {
    private var numberOfMechanics = -1
    private var sortBy = SortType.ITEM_COUNT

    private val viewModel by viewModels<MechanicsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.mechanics.observe(this) {
            numberOfMechanics = it?.size ?: 0
            invalidateOptionsMenu()
        }
        viewModel.sort.observe(this) {
            sortBy = it.sortType
            invalidateOptionsMenu()
        }
    }

    override fun onCreatePane(intent: Intent): Fragment = MechanicsFragment()

    override val optionsMenuId = R.menu.mechanics

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(when (sortBy) {
            SortType.NAME -> R.id.menu_sort_name
            SortType.ITEM_COUNT -> R.id.menu_sort_item_count
        })?.isChecked = true
        menu.setActionBarCount(R.id.menu_list_count, numberOfMechanics, getString(R.string.by_prefix, title))
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_sort_name -> viewModel.sort(SortType.NAME)
            R.id.menu_sort_item_count -> viewModel.sort(SortType.ITEM_COUNT)
            R.id.menu_refresh -> viewModel.refresh()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}
