package com.boardgamegeek.ui

import android.content.Intent
import android.os.Bundle
import android.view.View.OnClickListener
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import androidx.palette.graphics.Palette
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.boardgamegeek.R
import com.boardgamegeek.databinding.ActivityHeroBinding
import com.boardgamegeek.extensions.*

/**
 * A navigation drawer activity that displays a hero image.
 */
abstract class HeroActivity : DrawerActivity(), OnRefreshListener {
    protected lateinit var binding: ActivityHeroBinding

    protected var fragment: Fragment? = null
        private set
    protected var fabOnClickListener: OnClickListener? = null

    private val isRefreshable: Boolean
        get() = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        readIntent(intent)

        if (savedInstanceState == null) {
            createFragment()
        } else {
            fragment = supportFragmentManager.findFragmentByTag(TAG_SINGLE_PANE)
        }
        if (isRefreshable) {
            binding.swipeRefreshLayout.setOnRefreshListener(this)
            binding.swipeRefreshLayout.setBggColors()
            binding.swipeRefreshLayout.isEnabled = true
        } else {
            binding.swipeRefreshLayout.isEnabled = false
        }
    }

    override fun bindLayout() {
        binding = ActivityHeroBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onResume() {
        super.onResume()
        binding.fab.setOnClickListener(fabOnClickListener)
    }

    protected abstract fun readIntent(intent: Intent)

    private fun createFragment() {
        fragment = onCreatePane()
        fragment?.let {
            supportFragmentManager
                .beginTransaction()
                .add(R.id.root_container, it, TAG_SINGLE_PANE)
                .commit()
        }
    }

    /**
     * Called in `onCreate` when the fragment constituting this activity is needed. The returned fragment's
     * arguments will be set to the intent used to invoke this activity.
     */
    protected abstract fun onCreatePane(): Fragment

    protected fun safelySetTitle(title: String) {
        if (title.isNotBlank()) {
            binding.collapsingToolbar.title = title
        }
    }

    protected fun setFabImageResource(@DrawableRes imageResId: Int) {
        binding.fab.setImageResource(imageResId)
    }

    protected fun loadToolbarImage(url: String) {
        binding.toolbarImage.loadImage(url, callback = object : ImageLoadCallback {
            override fun onSuccessfulImageLoad(palette: Palette?) {
                binding.scrimView.applyDarkScrim()
                if (palette != null) {
                    onPaletteGenerated(palette)
                    binding.fab.colorize(palette.getIconColor())
                }
                binding.fab.show()
            }

            override fun onFailedImageLoad() {
                binding.fab.show()
            }
        })
    }

    protected abstract fun onPaletteGenerated(palette: Palette?)

    protected fun enableSwipeRefreshLayout(isEnabled: Boolean) {
        binding.swipeRefreshLayout.isEnabled = isEnabled
    }

    override fun onRefresh() {
        //No-op; just here for the children
    }

    companion object {
        private const val TAG_SINGLE_PANE = "single_pane"
    }
}
