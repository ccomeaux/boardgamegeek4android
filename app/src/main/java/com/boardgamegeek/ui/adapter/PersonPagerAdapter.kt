package com.boardgamegeek.ui.adapter

import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.boardgamegeek.R
import com.boardgamegeek.ui.PersonActivity
import com.boardgamegeek.ui.ForumsFragment
import com.boardgamegeek.ui.PersonCollectionFragment
import com.boardgamegeek.ui.PersonDescriptionFragment

class PersonPagerAdapter(
        fragmentManager: FragmentManager,
        private val activity: FragmentActivity,
        private val id: Int,
        private val name: String,
        private val type: PersonActivity.PersonType
) : FragmentPagerAdapter(fragmentManager) {
    override fun getItem(position: Int): Fragment? {
        return when (position) {
            0 -> PersonDescriptionFragment.newInstance()
            1 -> PersonCollectionFragment.newInstance()
            2 -> {
                when (type) {
                    PersonActivity.PersonType.ARTIST -> ForumsFragment.newInstanceForArtist(id, name)
                    PersonActivity.PersonType.DESIGNER -> ForumsFragment.newInstanceForDesigner(id, name)
                    PersonActivity.PersonType.PUBLISHER -> ForumsFragment.newInstanceForPublisher(id, name)
                }
            }
            else -> null
        }
    }

    override fun getPageTitle(position: Int): CharSequence? {
        @StringRes val resId = when (position) {
            0 -> {
                when (type) {
                    PersonActivity.PersonType.ARTIST -> R.string.title_artist
                    PersonActivity.PersonType.DESIGNER -> R.string.title_designer
                    PersonActivity.PersonType.PUBLISHER -> R.string.title_publisher
                }
            }
            1 -> R.string.title_collection
            2 -> R.string.title_forums
            else -> 0
        }
        if (resId == 0) return ""
        return activity.getString(resId)
    }

    override fun getCount(): Int {
        return 3
    }
}