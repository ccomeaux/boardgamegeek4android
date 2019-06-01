package com.boardgamegeek.ui

import androidx.fragment.app.Fragment
import com.boardgamegeek.R

class CollectionDetailsActivity : TopLevelSinglePaneActivity() {
    override fun onCreatePane(): Fragment = CollectionDetailsFragment()

    override val navigationItemId: Int = R.id.collection_details
}
