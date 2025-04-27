package com.boardgamegeek.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import com.boardgamegeek.R
import com.boardgamegeek.extensions.setActionBarCount
import com.boardgamegeek.model.Company
import com.boardgamegeek.ui.viewmodel.PublishersViewModel

class PublishersActivity : SimpleSinglePaneActivity() {
    private var numberOfPublishers = -1
    private var sortBy: Company.SortType? = null

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

    override fun createPane() = PublishersFragment()

    override val optionsMenuId = R.menu.publishers

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        val text = menu.findItem(when (sortBy) {
            Company.SortType.NAME -> R.id.menu_sort_name
            Company.SortType.ITEM_COUNT -> R.id.menu_sort_item_count
            Company.SortType.WHITMORE_SCORE -> R.id.menu_sort_whitmore_score
            else -> View.NO_ID
        })?.let {
            it.isChecked = true
            getString(R.string.by_prefix, it.title)
        }
        menu.setActionBarCount(R.id.menu_list_count, numberOfPublishers, text)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_sort_name -> viewModel.sort(Company.SortType.NAME)
            R.id.menu_sort_item_count -> viewModel.sort(Company.SortType.ITEM_COUNT)
            R.id.menu_sort_whitmore_score -> viewModel.sort(Company.SortType.WHITMORE_SCORE)
            R.id.menu_refresh -> viewModel.reload()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}
