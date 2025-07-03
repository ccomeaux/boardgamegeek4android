package com.boardgamegeek.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import com.boardgamegeek.R
import com.boardgamegeek.extensions.setActionBarCount
import com.boardgamegeek.model.Person
import com.boardgamegeek.ui.viewmodel.DesignersViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DesignersActivity : SimpleSinglePaneActivity() {
    private var numberOfDesigners = -1
    private var sortBy: Person.SortType? = null

    private val viewModel by viewModels<DesignersViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.designers.observe(this) {
            numberOfDesigners = it?.size ?: 0
            invalidateOptionsMenu()
        }
        viewModel.sort.observe(this) {
            sortBy = it
            invalidateOptionsMenu()
        }
    }

    override fun createPane() = DesignersFragment()

    override val optionsMenuId = R.menu.designers

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        val text = menu.findItem(
            when (sortBy) {
                Person.SortType.Name -> R.id.menu_sort_name
                Person.SortType.ItemCount -> R.id.menu_sort_item_count
                Person.SortType.WhitmoreScore -> R.id.menu_sort_whitmore_score
                else -> View.NO_ID
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
            R.id.menu_sort_name -> viewModel.sort(Person.SortType.Name)
            R.id.menu_sort_item_count -> viewModel.sort(Person.SortType.ItemCount)
            R.id.menu_sort_whitmore_score -> viewModel.sort(Person.SortType.WhitmoreScore)
            R.id.menu_refresh -> viewModel.reload()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}
