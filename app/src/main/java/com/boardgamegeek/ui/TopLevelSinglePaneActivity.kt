package com.boardgamegeek.ui

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment

import com.boardgamegeek.R
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.ContentViewEvent

private const val TAG_SINGLE_PANE = "single_pane"

abstract class TopLevelSinglePaneActivity : TopLevelActivity() {
    var fragment: Fragment? = null
        private set

    open val answersContentType: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        readIntent(intent)

        if (savedInstanceState == null) {
            createFragment()
            if (!answersContentType.isBlank()) {
                Answers.getInstance().logContentView(ContentViewEvent().putContentType(answersContentType))
            }
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
}
