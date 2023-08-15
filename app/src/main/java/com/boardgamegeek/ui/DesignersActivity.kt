package com.boardgamegeek.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import com.boardgamegeek.extensions.setActionBarCount
import com.boardgamegeek.ui.viewmodel.DesignersViewModel
import com.boardgamegeek.ui.viewmodel.DesignersViewModel.SortType
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DesignersActivity : SimpleSinglePaneActivity() {
    private var numberOfDesigners = -1
    private var sortBy = SortType.ITEM_COUNT

    private val viewModel by viewModels<DesignersViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.designers.observe(this) {
            numberOfDesigners = it?.size ?: 0
            invalidateOptionsMenu()
        }
        viewModel.sort.observe(this) {
            sortBy = it.sortType
            invalidateOptionsMenu()
        }
    }

    override fun onCreatePane(intent: Intent): Fragment = DesignersFragment()

    override val optionsMenuId = R.menu.designers

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        val text = menu.findItem(
            when (sortBy) {
                SortType.NAME -> R.id.menu_sort_name
                SortType.ITEM_COUNT -> R.id.menu_sort_item_count
                SortType.WHITMORE_SCORE -> R.id.menu_sort_whitmore_score
            }
        )?.let {
            it.isChecked = true
            getString(R.string.by_prefix, it.title)
        }
        menu.setActionBarCount(R.id.menu_list_count, numberOfDesigners, text)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_sort_name -> viewModel.sort(SortType.NAME)
            R.id.menu_sort_item_count -> viewModel.sort(SortType.ITEM_COUNT)
            R.id.menu_sort_whitmore_score -> viewModel.sort(SortType.WHITMORE_SCORE)
            R.id.menu_refresh -> viewModel.refresh()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}
