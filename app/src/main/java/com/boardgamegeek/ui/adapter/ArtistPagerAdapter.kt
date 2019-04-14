package com.boardgamegeek.ui.adapter

import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.boardgamegeek.R
import com.boardgamegeek.ui.ArtistDescriptionFragment
import com.boardgamegeek.ui.ForumsFragment

class ArtistPagerAdapter(
        fragmentManager: FragmentManager,
        private val activity: FragmentActivity,
        private val id: Int,
        private val name: String
) : FragmentPagerAdapter(fragmentManager) {
    override fun getItem(position: Int): Fragment? {
        return when (position) {
            0 -> ArtistDescriptionFragment.newInstance(id)
            1 -> ForumsFragment.newInstanceForArtist(id, name)
            else -> null
        }
    }

    override fun getPageTitle(position: Int): CharSequence? {
        @StringRes val resId = when (position) {
            0 -> R.string.title_artist
            1 -> R.string.title_forums
            else -> 0
        }
        if (resId == 0) return ""
        return activity.getString(resId)
    }

    override fun getCount(): Int {
        return 2
    }
}