package com.boardgamegeek.ui

import android.os.Bundle
import androidx.fragment.app.FragmentPagerAdapter
import com.boardgamegeek.R
import kotlinx.android.synthetic.main.activity_tab.*

/**
 * A navigation drawer activity that displays a view pager.
 */
abstract class TabActivity : DrawerActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewPager.adapter = createAdapter()
        tabLayout.setupWithViewPager(viewPager)
    }

    protected abstract fun createAdapter(): FragmentPagerAdapter

    override val layoutResId = R.layout.activity_tab

    protected fun safelySetTitle(title: String?) {
        if (!title.isNullOrBlank()) {
            supportActionBar?.title = title
        }
    }
}
