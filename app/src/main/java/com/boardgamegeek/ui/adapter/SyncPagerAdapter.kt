package com.boardgamegeek.ui.adapter

import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.boardgamegeek.R
import com.boardgamegeek.ui.ErrorFragment
import com.boardgamegeek.ui.SyncCollectionFragment
import com.boardgamegeek.ui.SyncPlaysFragment
import com.boardgamegeek.ui.SyncUsersFragment

class SyncPagerAdapter(
    private val activity: FragmentActivity,
) : FragmentStateAdapter(activity) {
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> SyncCollectionFragment()
            1 -> SyncPlaysFragment()
            2 -> SyncUsersFragment()
            else -> ErrorFragment()
        }
    }

    fun getPageTitle(position: Int): CharSequence {
        @StringRes val resId = when (position) {
            0 -> R.string.title_collection
            1 -> R.string.title_plays
            2 -> R.string.title_users
            else -> ResourcesCompat.ID_NULL
        }
        return activity.getString(if (resId == ResourcesCompat.ID_NULL) R.string.title_error else resId)
    }

    override fun getItemCount() = 3
}
