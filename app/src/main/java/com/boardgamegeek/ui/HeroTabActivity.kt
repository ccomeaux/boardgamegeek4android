package com.boardgamegeek.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentPagerAdapter
import androidx.palette.graphics.Palette
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.boardgamegeek.R
import com.boardgamegeek.databinding.ActivityHeroTabBinding
import com.boardgamegeek.extensions.applyDarkScrim
import com.boardgamegeek.extensions.loadUrl
import com.boardgamegeek.util.ImageUtils
import com.boardgamegeek.util.ImageUtils.safelyLoadImage

/**
 * A navigation drawer activity that displays a hero image over a view pager.
 */
abstract class HeroTabActivity : DrawerActivity() {
    protected lateinit var binding: ActivityHeroTabBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        ActivityHeroTabBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    protected fun initializeViewPager() {
        binding.viewPager.adapter = createAdapter()
        createOnPageChangeListener()?.let { binding.viewPager.addOnPageChangeListener(it) }
        binding.tabLayout.setupWithViewPager(binding.viewPager)
    }

    protected abstract fun createAdapter(): FragmentPagerAdapter

    protected open fun createOnPageChangeListener(): OnPageChangeListener? {
        return null
    }

    override fun inflateRootView(): View {
        return binding.root
    }

    protected fun safelySetTitle(title: String?) {
        if (!title.isNullOrBlank()) {
            binding.collapsingToolbar.title = title
        }
    }

    protected fun loadToolbarImage(url: String) {
        binding.toolbarImage.loadUrl(url, object : ImageUtils.Callback {
            override fun onSuccessfulImageLoad(palette: Palette?) {
                onPaletteLoaded(palette)
                binding.scrimView.applyDarkScrim()
            }

            override fun onFailedImageLoad() {}
        })
    }

    protected fun loadToolbarImage(imageId: Int) {
        binding.toolbarImage.safelyLoadImage(imageId, object : ImageUtils.Callback {
            override fun onSuccessfulImageLoad(palette: Palette?) {
                onPaletteLoaded(palette)
                binding.scrimView.applyDarkScrim()
            }

            override fun onFailedImageLoad() {

            }
        })
    }

    protected open fun onPaletteLoaded(palette: Palette?) {
    }

    protected fun getCoordinatorLayout() = binding.coordinatorLayout
}
