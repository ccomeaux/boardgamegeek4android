package com.boardgamegeek.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import com.boardgamegeek.extensions.setActionBarCount
import com.boardgamegeek.model.Mechanic
import com.boardgamegeek.ui.viewmodel.MechanicsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MechanicsActivity : SimpleSinglePaneActivity() {
    private var numberOfMechanics = -1
    private var sortBy: Mechanic.SortType? = null

    private val viewModel by viewModels<MechanicsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.mechanics.observe(this) {
            numberOfMechanics = it?.size ?: 0
            invalidateOptionsMenu()
        }
        viewModel.sort.observe(this) {
            sortBy = it
            invalidateOptionsMenu()
        }
    }

    override fun onCreatePane(intent: Intent): Fragment = MechanicsFragment()

    override val optionsMenuId = R.menu.mechanics

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        val text = menu.findItem(when (sortBy) {
            Mechanic.SortType.NAME -> R.id.menu_sort_name
            Mechanic.SortType.ITEM_COUNT -> R.id.menu_sort_item_count
            else -> View.NO_ID
        })?.let {
            it.isChecked = true
            getString(R.string.by_prefix, it.title)
        }
        menu.setActionBarCount(R.id.menu_list_count, numberOfMechanics, text)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_sort_name -> viewModel.sort(Mechanic.SortType.NAME)
            R.id.menu_sort_item_count -> viewModel.sort(Mechanic.SortType.ITEM_COUNT)
            R.id.menu_refresh -> viewModel.reload()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}
