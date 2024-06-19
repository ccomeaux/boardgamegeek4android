package com.boardgamegeek.ui

import androidx.viewpager2.adapter.FragmentStateAdapter
import com.boardgamegeek.R
import com.boardgamegeek.ui.adapter.SyncPagerAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SyncActivity : TabActivity() {
    private val adapter: SyncPagerAdapter by lazy {
        SyncPagerAdapter(this)
    }

    override fun getPageTitle(position: Int): CharSequence = adapter.getPageTitle(position)

    override fun createAdapter(): FragmentStateAdapter = adapter

    override val navigationItemId: Int = R.id.sync
}
