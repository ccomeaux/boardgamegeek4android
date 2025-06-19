package com.boardgamegeek.ui

import android.os.Bundle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.boardgamegeek.databinding.ActivityTabBinding
import com.google.android.material.tabs.TabLayoutMediator

/**
 * A navigation drawer activity that displays a view pager.
 */
abstract class TabActivity : DrawerActivity() {
    private lateinit var binding: ActivityTabBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.viewPager.adapter = createAdapter()

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = getPageTitle(position)
        }.attach()
    }

    override fun bindLayout() {
        binding = ActivityTabBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    abstract fun getPageTitle(position: Int): CharSequence

    protected abstract fun createAdapter(): FragmentStateAdapter
}
