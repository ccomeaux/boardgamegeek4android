package com.boardgamegeek.ui

import android.content.Context
import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import org.jetbrains.anko.clearTop
import org.jetbrains.anko.intentFor

class ForumsActivity : TopLevelSinglePaneActivity() {
    override val answersContentType = "Forums"

    override fun onCreatePane(): Fragment = ForumsFragment.newInstance()

    override fun getDrawerResId() = R.string.title_forums

    companion object {
        @JvmStatic
        fun startUp(context: Context) = context.startActivity(context.intentFor<ForumsActivity>().clearTop())
    }
}
