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
import com.boardgamegeek.model.Person
import com.boardgamegeek.ui.viewmodel.ArtistsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ArtistsActivity : SimpleSinglePaneActivity() {
    private var numberOfArtists = -1
    private var sortBy: Person.SortType? = null

    private val viewModel by viewModels<ArtistsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.artists.observe(this) {
            numberOfArtists = it?.size ?: 0
            invalidateOptionsMenu()
        }
        viewModel.sort.observe(this) {
            sortBy = it
            invalidateOptionsMenu()
        }
    }

    override fun onCreatePane(intent: Intent): Fragment = ArtistsFragment()

    override val optionsMenuId = R.menu.artists

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        val text = menu.findItem(
            when (sortBy) {
                Person.SortType.NAME -> R.id.menu_sort_name
                Person.SortType.ITEM_COUNT -> R.id.menu_sort_item_count
                Person.SortType.WHITMORE_SCORE -> R.id.menu_sort_whitmore_score
                else -> View.NO_ID
            }
        )?.let {
            it.isChecked = true
            getString(R.string.by_prefix, it.title)
        }
        menu.setActionBarCount(R.id.menu_list_count, numberOfArtists, text)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_sort_name -> viewModel.sort(Person.SortType.NAME)
            R.id.menu_sort_item_count -> viewModel.sort(Person.SortType.ITEM_COUNT)
            R.id.menu_sort_whitmore_score -> viewModel.sort(Person.SortType.WHITMORE_SCORE)
            R.id.menu_refresh -> viewModel.reload()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}
