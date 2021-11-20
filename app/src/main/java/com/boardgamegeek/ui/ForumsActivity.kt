package com.boardgamegeek.ui

import android.content.Context
import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import com.boardgamegeek.extensions.clearTop
import com.boardgamegeek.extensions.intentFor

class ForumsActivity : TopLevelSinglePaneActivity() {
    override val answersContentType = "Forums"

    override fun onCreatePane(): Fragment = ForumsFragment.newInstance()

    override val navigationItemId = R.id.forums

    companion object {
        fun startUp(context: Context) = context.startActivity(context.intentFor<ForumsActivity>().clearTop())
    }
}
