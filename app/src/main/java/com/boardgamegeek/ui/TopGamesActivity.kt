package com.boardgamegeek.ui

import android.support.v4.app.Fragment
import com.boardgamegeek.R

class TopGamesActivity : TopLevelSinglePaneActivity() {
    override val answersContentType = "Top Games"

    override fun onCreatePane(): Fragment = TopGamesFragment()

    override fun getDrawerResId(): Int = R.string.title_top_games
}
