package com.boardgamegeek.ui

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.ActivityTabBinding
import com.boardgamegeek.extensions.intentFor
import com.boardgamegeek.extensions.notifyLoggedPlay
import com.boardgamegeek.ui.adapter.CollectionPagerAdapter
import com.boardgamegeek.ui.viewmodel.CollectionDetailsViewModel
import com.google.android.material.tabs.TabLayoutMediator

class CollectionDetailsActivity : TopLevelActivity() {
    private lateinit var binding: ActivityTabBinding
    private val viewModel by viewModels<CollectionDetailsViewModel>()

    override val navigationItemId: Int = R.id.collection_details

    private val adapter: CollectionPagerAdapter by lazy {
        CollectionPagerAdapter(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel.loggedPlayResult.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                notifyLoggedPlay(it)
            }
        }

        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = adapter.getPageTitle(position)
        }.attach()

        binding.viewPager.isUserInputEnabled = false

        viewModel.refresh()
    }

    override fun bindLayout() {
        binding = ActivityTabBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override val optionsMenuId = R.menu.search

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.menu_search) {
            startActivity(intentFor<SearchResultsActivity>())
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }
}
