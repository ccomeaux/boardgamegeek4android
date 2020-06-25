package com.boardgamegeek.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.boardgamegeek.R
import com.boardgamegeek.extensions.setActionBarCount
import com.boardgamegeek.ui.viewmodel.PublishersViewModel

class PublishersActivity : SimpleSinglePaneActivity() {
    private var numberOfPublishers = -1
    private var sortBy = PublishersViewModel.SortType.ITEM_COUNT

    private val viewModel by viewModels<PublishersViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.publishers.observe(this, Observer {
            numberOfPublishers = it?.size ?: 0
            invalidateOptionsMenu()
        })
        viewModel.sort.observe(this, Observer {
            sortBy = it.sortType
            invalidateOptionsMenu()
        })
    }

    override fun onCreatePane(intent: Intent): Fragment = PublishersFragment()

    override val optionsMenuId = R.menu.publishers

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        when (sortBy) {
            PublishersViewModel.SortType.NAME -> menu.findItem(R.id.menu_sort_name)
            PublishersViewModel.SortType.ITEM_COUNT -> menu.findItem(R.id.menu_sort_item_count)
            PublishersViewModel.SortType.WHITMORE_SCORE -> menu.findItem(R.id.menu_sort_whitmore_score)
        }.apply {
            isChecked = true
            menu.setActionBarCount(R.id.menu_list_count, numberOfPublishers, getString(R.string.by_prefix, title))
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_sort_name -> {
                viewModel.sort(PublishersViewModel.SortType.NAME)
                return true
            }
            R.id.menu_sort_item_count -> {
                viewModel.sort(PublishersViewModel.SortType.ITEM_COUNT)
                return true
            }
            R.id.menu_sort_whitmore_score -> {
                viewModel.sort(PublishersViewModel.SortType.WHITMORE_SCORE)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
