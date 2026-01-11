package com.boardgamegeek.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentPagerAdapter
import com.boardgamegeek.R
import com.boardgamegeek.databinding.ActivityTabBinding

/**
 * A navigation drawer activity that displays a view pager.
 */
abstract class TabActivity : DrawerActivity() {
    protected lateinit var binding: ActivityTabBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityTabBinding.inflate(layoutInflater)

        super.onCreate(savedInstanceState)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.viewPager.adapter = createAdapter()
        binding.tabLayout.setupWithViewPager(binding.viewPager)
    }

    protected abstract fun createAdapter(): FragmentPagerAdapter

    override fun inflateRootView(): View {
        return binding.root
    }

    protected fun safelySetTitle(title: String?) {
        if (!title.isNullOrBlank()) {
            supportActionBar?.title = title
        }
    }
}
