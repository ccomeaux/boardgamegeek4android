package com.boardgamegeek.ui

import android.os.Bundle
import android.view.Menu
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.boardgamegeek.R
import com.boardgamegeek.extensions.setActionBarCount
import com.boardgamegeek.ui.viewmodel.BuddiesViewModel

class BuddiesActivity : TopLevelSinglePaneActivity() {
    private var numberOfBuddies = 0

    private val viewModel: BuddiesViewModel by lazy {
        ViewModelProviders.of(this).get(BuddiesViewModel::class.java)
    }

    override val answersContentType = "Buddies"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.buddies.observe(this, Observer {
            numberOfBuddies = it?.data?.size ?: 0
            invalidateOptionsMenu()
        })
    }

    override fun onCreatePane(): Fragment = BuddiesFragment()

    override val optionsMenuId = R.menu.buddies

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.setActionBarCount(R.id.menu_list_count, numberOfBuddies)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun getDrawerResId() = R.string.title_buddies
}
