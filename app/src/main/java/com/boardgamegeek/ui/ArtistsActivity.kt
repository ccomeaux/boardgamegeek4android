package com.boardgamegeek.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.boardgamegeek.R
import com.boardgamegeek.extensions.setActionBarCount
import com.boardgamegeek.ui.viewmodel.ArtistsViewModel

class ArtistsActivity : TopLevelSinglePaneActivity() {
    private var numberOfArtists = -1
    private var sortBy = ArtistsViewModel.SortType.NAME

    private val viewModel: ArtistsViewModel by lazy {
        ViewModelProviders.of(this).get(ArtistsViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.artists.observe(this, Observer {
            numberOfArtists = it?.size ?: 0
            invalidateOptionsMenu()
        })
        viewModel.sort.observe(this, Observer {
            sortBy = it.sortType
            invalidateOptionsMenu()
        })
    }

    override fun onCreatePane(): Fragment = ArtistsFragment.newInstance()

    override val optionsMenuId = R.menu.artists

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        when (sortBy) {
            ArtistsViewModel.SortType.NAME -> menu.findItem(R.id.menu_sort_name)
            ArtistsViewModel.SortType.ITEM_COUNT -> menu.findItem(R.id.menu_sort_item_count)
        }.apply {
            isChecked = true
            menu.setActionBarCount(R.id.menu_list_count, numberOfArtists, getString(R.string.by_prefix, title))
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_sort_name -> {
                viewModel.sort(ArtistsViewModel.SortType.NAME)
                return true
            }
            R.id.menu_sort_item_count -> {
                viewModel.sort(ArtistsViewModel.SortType.ITEM_COUNT)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override val navigationItemId = R.id.artists
}
