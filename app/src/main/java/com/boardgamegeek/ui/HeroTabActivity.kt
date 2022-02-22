package com.boardgamegeek.ui

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.palette.graphics.Palette
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.boardgamegeek.databinding.ActivityHeroTabBinding
import com.boardgamegeek.extensions.ImageLoadCallback
import com.boardgamegeek.extensions.applyDarkScrim
import com.boardgamegeek.extensions.loadUrl
import com.boardgamegeek.extensions.safelyLoadImage
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

/**
 * A navigation drawer activity that displays a hero image over a view pager.
 */
abstract class HeroTabActivity : DrawerActivity() {
    private lateinit var binding: ActivityHeroTabBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun bindLayout() {
        binding = ActivityHeroTabBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    protected fun initializeViewPager() {
        binding.viewPager.adapter = createAdapter()
        createOnPageChangeListener()?.let { binding.viewPager.registerOnPageChangeCallback(it) }
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = getPageTitle(position)
        }.attach()
    }

    protected abstract fun getPageTitle(position: Int): CharSequence

    protected abstract fun createAdapter(): FragmentStateAdapter

    protected open fun createOnPageChangeListener(): ViewPager2.OnPageChangeCallback? {
        return null
    }

    protected fun safelySetTitle(title: String?) {
        if (!title.isNullOrBlank()) {
            binding.collapsingToolbar.title = title
        }
    }

    protected fun loadToolbarImage(url: String) {
        binding.toolbarImage.loadUrl(url, object : ImageLoadCallback {
            override fun onSuccessfulImageLoad(palette: Palette?) {
                onPaletteLoaded(palette)
                binding.scrimView.applyDarkScrim()
            }

            override fun onFailedImageLoad() {}
        })
    }

    protected fun loadToolbarImage(imageId: Int) {
        lifecycleScope.launch {
            binding.toolbarImage.safelyLoadImage(imageId, object : ImageLoadCallback {
                override fun onSuccessfulImageLoad(palette: Palette?) {
                    onPaletteLoaded(palette)
                    binding.scrimView.applyDarkScrim()
                }

                override fun onFailedImageLoad() {}
            })
        }
    }

    protected open fun onPaletteLoaded(palette: Palette?) {
    }
}
