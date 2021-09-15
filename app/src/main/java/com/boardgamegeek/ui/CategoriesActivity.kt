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
import com.boardgamegeek.ui.viewmodel.CategoriesViewModel

class CategoriesActivity : SimpleSinglePaneActivity() {
    private var numberOfCategories = -1
    private var sortBy = CategoriesViewModel.SortType.ITEM_COUNT

    private val viewModel by viewModels<CategoriesViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.categories.observe(this, Observer {
            numberOfCategories = it?.size ?: 0
            invalidateOptionsMenu()
        })
        viewModel.sort.observe(this, Observer {
            sortBy = it.sortType
            invalidateOptionsMenu()
        })
    }

    override fun onCreatePane(intent: Intent): Fragment = CategoriesFragment()

    override val optionsMenuId = R.menu.categories

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        when (sortBy) {
            CategoriesViewModel.SortType.NAME -> menu.findItem(R.id.menu_sort_name)
            CategoriesViewModel.SortType.ITEM_COUNT -> menu.findItem(R.id.menu_sort_item_count)
        }.apply {
            isChecked = true
            menu.setActionBarCount(R.id.menu_list_count, numberOfCategories, getString(R.string.by_prefix, title))
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_sort_name -> {
                viewModel.sort(CategoriesViewModel.SortType.NAME)
                return true
            }
            R.id.menu_sort_item_count -> {
                viewModel.sort(CategoriesViewModel.SortType.ITEM_COUNT)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
