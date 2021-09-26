package com.boardgamegeek.ui

import android.content.Intent
import android.os.Bundle
import android.view.View.OnClickListener
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import androidx.palette.graphics.Palette
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.util.ImageUtils.Callback
import kotlinx.android.synthetic.main.activity_hero.*

/**
 * A navigation drawer activity that displays a hero image.
 */
abstract class HeroActivity : DrawerActivity(), OnRefreshListener {
    protected var fragment: Fragment? = null
        private set
    private var isRefreshing: Boolean = false
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
            swipeRefreshLayout.setOnRefreshListener(this)
            swipeRefreshLayout.setBggColors()
            swipeRefreshLayout.isEnabled = true
        } else {
            swipeRefreshLayout.isEnabled = false
        }
    }

    override fun onResume() {
        super.onResume()
        fab.setOnClickListener(fabOnClickListener)
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

    override val layoutResId = R.layout.activity_hero

    protected fun safelySetTitle(title: String) {
        if (title.isNotBlank()) {
            collapsingToolbar.title = title
        }
    }

    protected fun setFabImageResource(@DrawableRes imageResId: Int) {
        fab.setImageResource(imageResId)
    }

    protected fun loadToolbarImage(url: String) {
        toolbarImage.loadUrl(url, object : Callback {
            override fun onSuccessfulImageLoad(palette: Palette?) {
                scrimView.applyDarkScrim()
                if (palette != null) {
                    onPaletteGenerated(palette)
                    fab.colorize(palette.getIconSwatch().rgb)
                }
                fab.show()
            }

            override fun onFailedImageLoad() {
                fab.show()
            }
        })
    }

    protected abstract fun onPaletteGenerated(palette: Palette?)

    protected fun enableSwipeRefreshLayout(isEnabled: Boolean) {
        swipeRefreshLayout.isEnabled = isEnabled
    }

    override fun onRefresh() {
        //No-op; just here for the children
    }

    protected fun updateRefreshStatus(refreshing: Boolean) {
        this.isRefreshing = refreshing
        swipeRefreshLayout?.post { swipeRefreshLayout?.isRefreshing = isRefreshing }
    }

    companion object {
        private const val TAG_SINGLE_PANE = "single_pane"
    }
}
