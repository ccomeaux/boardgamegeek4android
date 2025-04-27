package com.boardgamegeek.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import com.boardgamegeek.R
import com.boardgamegeek.extensions.setActionBarCount
import com.boardgamegeek.model.Category
import com.boardgamegeek.ui.viewmodel.CategoriesViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CategoriesActivity : SimpleSinglePaneActivity() {
    private var numberOfCategories = -1
    private var sortBy: Category.SortType? = null
    private val viewModel by viewModels<CategoriesViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.categories.observe(this) {
            numberOfCategories = it?.size ?: 0
            invalidateOptionsMenu()
        }
        viewModel.sort.observe(this) {
            sortBy = it
            invalidateOptionsMenu()
        }
    }

    override fun createPane() = CategoriesFragment()

    override val optionsMenuId = R.menu.categories

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        val text = menu.findItem(when (sortBy) {
            Category.SortType.NAME -> R.id.menu_sort_name
            Category.SortType.ITEM_COUNT -> R.id.menu_sort_item_count
            else -> View.NO_ID
        })?.let {
            it.isChecked = true
            getString(R.string.by_prefix, it.title)
        }
        menu.setActionBarCount(R.id.menu_list_count, numberOfCategories, text)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_sort_name -> viewModel.sort(Category.SortType.NAME)
            R.id.menu_sort_item_count -> viewModel.sort(Category.SortType.ITEM_COUNT)
            R.id.menu_refresh -> viewModel.reload()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}
