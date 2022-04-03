package com.boardgamegeek.ui

import androidx.fragment.app.Fragment

import com.boardgamegeek.R

class DataActivity : TopLevelSinglePaneActivity() {
    override val firebaseContentType = "Data"

    override fun onCreatePane(): Fragment = DataFragment()

    override val navigationItemId: Int = R.id.data
}
