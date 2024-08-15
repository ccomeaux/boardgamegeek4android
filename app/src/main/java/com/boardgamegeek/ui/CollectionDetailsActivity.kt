package com.boardgamegeek.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import com.boardgamegeek.databinding.ActivityTabBinding
import com.boardgamegeek.ui.adapter.CollectionPagerAdapter
import com.boardgamegeek.ui.adapter.SyncPagerAdapter
import com.google.android.material.tabs.TabLayoutMediator

class CollectionDetailsActivity : TopLevelActivity() {
    override val navigationItemId: Int = R.id.collection_details

    private lateinit var binding: ActivityTabBinding

    private val adapter: CollectionPagerAdapter by lazy {
        CollectionPagerAdapter(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = adapter.getPageTitle(position)
        }.attach()
    }

    override fun bindLayout() {
        binding = ActivityTabBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
