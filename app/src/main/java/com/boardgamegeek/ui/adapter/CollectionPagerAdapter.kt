package com.boardgamegeek.ui.adapter

import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.boardgamegeek.R
import com.boardgamegeek.ui.*

class CollectionPagerAdapter(
    private val activity: FragmentActivity,
) : FragmentStateAdapter(activity) {
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> CollectionBrowseFragment()
            1 -> CollectionPlayFragment()
            2 -> CollectionAcquireFragment()
            3 -> CollectionDivestFragment()
            4 -> CollectionAnalyzeFragment()
            5 -> CollectionCreditsFragment()
            else -> ErrorFragment()
        }
    }

    fun getPageTitle(position: Int): CharSequence {
        @StringRes val resId = when (position) {
            0 -> R.string.title_browse
            1 -> R.string.title_play
            2 -> R.string.title_acquire
            3 -> R.string.title_divest
            4 -> R.string.title_analyze
            5 -> R.string.title_credits
            else -> ResourcesCompat.ID_NULL
        }
        return activity.getString(if (resId == ResourcesCompat.ID_NULL) R.string.title_error else resId)
    }

    override fun getItemCount() = 6
}
