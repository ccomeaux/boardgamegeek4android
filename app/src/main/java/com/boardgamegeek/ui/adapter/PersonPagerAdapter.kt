package com.boardgamegeek.ui.adapter

import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.boardgamegeek.R
import com.boardgamegeek.ui.*

class PersonPagerAdapter(
        private val activity: FragmentActivity,
        private val id: Int,
        private val name: String,
        private val type: PersonActivity.PersonType
) : FragmentStateAdapter(activity) {
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> PersonDescriptionFragment()
            1 -> PersonStatsFragment()
            2 -> PersonCollectionFragment()
            3 -> {
                when (type) {
                    PersonActivity.PersonType.ARTIST -> ForumsFragment.newInstanceForArtist(id, name)
                    PersonActivity.PersonType.DESIGNER -> ForumsFragment.newInstanceForDesigner(id, name)
                    PersonActivity.PersonType.PUBLISHER -> ForumsFragment.newInstanceForPublisher(id, name)
                }
            }
            else -> ErrorFragment()
        }
    }

    fun getPageTitle(position: Int): CharSequence {
        @StringRes val resId = when (position) {
            0 -> {
                when (type) {
                    PersonActivity.PersonType.ARTIST -> R.string.title_artist
                    PersonActivity.PersonType.DESIGNER -> R.string.title_designer
                    PersonActivity.PersonType.PUBLISHER -> R.string.title_publisher
                }
            }
            1 -> R.string.title_stats
            2 -> R.string.title_collection
            3 -> R.string.title_forums
            else -> 0
        }
        if (resId == 0) return ""
        return activity.getString(resId)
    }

    override fun getItemCount(): Int {
        return 4
    }
}
