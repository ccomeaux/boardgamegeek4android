package com.boardgamegeek.ui

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.boardgamegeek.R

/**
 * A non-top-level DrawerActivity that supports a single pane.
 */
abstract class SimpleSinglePaneActivity : DrawerActivity() {
    var fragment: Fragment? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        readIntent(intent)

        fragment = if (savedInstanceState == null) {
            createFragment()
        } else {
            supportFragmentManager.findFragmentByTag(TAG_SINGLE_PANE)
        }
    }

    protected open fun readIntent(intent: Intent) {}

    private fun createFragment(): Fragment {
        val fragment = onCreatePane(intent)
        supportFragmentManager
                .beginTransaction()
                .add(R.id.root_container, fragment, TAG_SINGLE_PANE)
                .commit()
        return fragment
    }

    /**
     * Called in `onCreate` when the fragment constituting this activity is needed. The returned fragment's
     * arguments will be set to the intent used to invoke this activity.
     */
    protected abstract fun onCreatePane(intent: Intent): Fragment

    companion object {
        private const val TAG_SINGLE_PANE = "single_pane"
    }
}
