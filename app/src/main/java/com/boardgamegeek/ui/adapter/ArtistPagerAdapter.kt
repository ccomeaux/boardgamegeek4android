package com.boardgamegeek.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.boardgamegeek.R
import com.boardgamegeek.ui.ArtistDescriptionFragment

class ArtistPagerAdapter(fragmentManager: FragmentManager, private val activity: FragmentActivity, private val id: Int) : FragmentPagerAdapter(fragmentManager) {
    override fun getItem(position: Int): Fragment {
        return ArtistDescriptionFragment.newInstance(id)
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return activity.getString(R.string.title_artist)
    }

    override fun getCount(): Int {
        return 1
    }
}