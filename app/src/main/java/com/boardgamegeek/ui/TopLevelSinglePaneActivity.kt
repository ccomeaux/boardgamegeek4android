package com.boardgamegeek.ui

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment

import com.boardgamegeek.R

abstract class TopLevelSinglePaneActivity : TopLevelActivity() {
    var fragment: Fragment? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        readIntent(intent)

        if (savedInstanceState == null) {
            createFragment()
        } else {
            fragment = supportFragmentManager.findFragmentByTag(TAG_SINGLE_PANE)
        }
    }

    protected open fun readIntent(intent: Intent) {}

    public override fun onNewIntent(intent: Intent) {
        createFragment()
    }

    protected abstract fun onCreatePane(): Fragment

    private fun createFragment() {
        fragment = onCreatePane()
        supportFragmentManager.beginTransaction().add(R.id.root_container, fragment, TAG_SINGLE_PANE).commit()
    }

    companion object {
        private const val TAG_SINGLE_PANE = "single_pane"
    }
}
