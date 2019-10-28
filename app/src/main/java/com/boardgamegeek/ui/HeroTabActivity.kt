package com.boardgamegeek.ui

import android.os.Bundle
import androidx.fragment.app.FragmentPagerAdapter
import androidx.palette.graphics.Palette
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.boardgamegeek.R
import com.boardgamegeek.extensions.applyDarkScrim
import com.boardgamegeek.extensions.loadUrl
import com.boardgamegeek.util.ImageUtils
import com.boardgamegeek.util.ImageUtils.safelyLoadImage
import kotlinx.android.synthetic.main.activity_hero_tab.*

/**
 * A navigation drawer activity that displays a hero image over a view pager.
 */
abstract class HeroTabActivity : DrawerActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    protected fun initializeViewPager() {
        viewPager.adapter = createAdapter()
        createOnPageChangeListener()?.let { viewPager.addOnPageChangeListener(it) }
        tabLayout.setupWithViewPager(viewPager)
    }

    protected abstract fun createAdapter(): FragmentPagerAdapter

    protected open fun createOnPageChangeListener(): OnPageChangeListener? {
        return null
    }

    override val layoutResId = R.layout.activity_hero_tab

    protected fun safelySetTitle(title: String?) {
        if (!title.isNullOrBlank()) {
            collapsingToolbar.title = title
        }
    }

    protected fun loadToolbarImage(url: String) {
        toolbarImage.loadUrl(url, object : ImageUtils.Callback {
            override fun onSuccessfulImageLoad(palette: Palette?) {
                onPaletteLoaded(palette)
                scrimView.applyDarkScrim()
            }

            override fun onFailedImageLoad() {}
        })
    }

    protected fun loadToolbarImage(imageId: Int) {
        toolbarImage.safelyLoadImage(imageId, object : ImageUtils.Callback {
            override fun onSuccessfulImageLoad(palette: Palette?) {
                onPaletteLoaded(palette)
                scrimView.applyDarkScrim()
            }

            override fun onFailedImageLoad() {

            }
        })
    }

    protected open fun onPaletteLoaded(palette: Palette?) {
    }

    protected fun getCoordinatorLayout() = coordinatorLayout!!
}
