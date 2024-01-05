package com.boardgamegeek.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import com.boardgamegeek.extensions.setActionBarCount
import com.boardgamegeek.ui.viewmodel.PublishersViewModel
import com.boardgamegeek.ui.viewmodel.PublishersViewModel.SortType

class PublishersActivity : SimpleSinglePaneActivity() {
    private var numberOfPublishers = -1
    private var sortBy: SortType? = null

    private val viewModel by viewModels<PublishersViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.publishers.observe(this) {
            numberOfPublishers = it?.size ?: 0
            invalidateOptionsMenu()
        }
        viewModel.sort.observe(this) {
            sortBy = it
            invalidateOptionsMenu()
        }
    }

    override fun onCreatePane(intent: Intent): Fragment = PublishersFragment()

    override val optionsMenuId = R.menu.publishers

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        val text = menu.findItem(when (sortBy) {
            SortType.NAME -> R.id.menu_sort_name
            SortType.ITEM_COUNT -> R.id.menu_sort_item_count
            SortType.WHITMORE_SCORE -> R.id.menu_sort_whitmore_score
            else -> 0
        })?.let {
            it.isChecked = true
            getString(R.string.by_prefix, it.title)
        }
        menu.setActionBarCount(R.id.menu_list_count, numberOfPublishers, text)
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
