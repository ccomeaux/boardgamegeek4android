package com.boardgamegeek.ui

import android.os.Bundle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.boardgamegeek.R
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.activity_tab.*

/**
 * A navigation drawer activity that displays a view pager.
 */
abstract class TabActivity : DrawerActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewPager.adapter = createAdapter()

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = getPageTitle(position)
        }.attach()
    }

    abstract fun getPageTitle(position: Int): CharSequence?

    protected abstract fun createAdapter(): FragmentStateAdapter

    override val layoutResId = R.layout.activity_tab

    protected fun safelySetTitle(title: String?) {
        if (!title.isNullOrBlank()) {
            supportActionBar?.title = title
        }
    }
}
