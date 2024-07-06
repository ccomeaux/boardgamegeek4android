package com.boardgamegeek.ui

import android.os.Bundle
import com.boardgamegeek.R
import com.boardgamegeek.databinding.ActivityTabBinding
import com.boardgamegeek.ui.adapter.SyncPagerAdapter
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SyncActivity : TopLevelActivity() {
    override val navigationItemId: Int = R.id.sync

    private lateinit var binding: ActivityTabBinding

    private val adapter: SyncPagerAdapter by lazy {
        SyncPagerAdapter(this)
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
