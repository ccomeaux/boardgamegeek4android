package com.boardgamegeek.ui

import androidx.fragment.app.Fragment
import com.boardgamegeek.R

class TopGamesActivity : TopLevelSinglePaneActivity() {
    override val firebaseContentType = "Top Games"

    override fun onCreatePane(): Fragment = TopGamesFragment()

    override val navigationItemId = R.id.top_games
}
