package com.boardgamegeek.ui.adapter

import android.graphics.Color
import android.support.annotation.ColorInt
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.view.ViewGroup
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.tasks.AddCollectionItemTask
import com.boardgamegeek.ui.*
import com.boardgamegeek.ui.dialog.CollectionStatusDialogFragment
import com.boardgamegeek.util.DialogUtils
import com.boardgamegeek.util.PreferencesUtils
import com.boardgamegeek.util.TaskUtils

class GamePagerAdapter(fragmentManager: FragmentManager, private val activity: FragmentActivity, private val gameId: Int, var gameName: String) :
        FragmentPagerAdapter(fragmentManager) {
    var currentPosition: Int = 0
    var thumbnailUrl: String = ""
    var imageUrl: String = ""
    var heroImageUrl: String = ""
    var arePlayersCustomSorted = false
    @ColorInt var iconColor = Color.TRANSPARENT
    private val tabs = arrayListOf<Tab>()
    private val fab = activity.findViewById<FloatingActionButton>(R.id.fab)
    private val rootContainer = activity.findViewById<ViewGroup>(R.id.root_container)

    private inner class Tab @JvmOverloads constructor(
            @field:StringRes val titleResId: Int,
            @field:DrawableRes val imageResId: Int = INVALID_RES_ID,
            val listener: () -> Unit = {})

    init {
        updateTabs()
    }

    override fun notifyDataSetChanged() {
        super.notifyDataSetChanged()
        updateTabs()
    }


    override fun getPageTitle(position: Int): CharSequence {
        @StringRes val resId = tabs.getOrNull(position)?.titleResId ?: INVALID_RES_ID
        return if (resId != INVALID_RES_ID) activity.getString(resId) else ""
    }

    override fun getItem(position: Int): Fragment? {
        return when (tabs.getOrNull(position)?.titleResId) {
            R.string.title_description -> GameDescriptionFragment.newInstance(gameId)
            R.string.title_info -> GameFragment.newInstance(gameId, gameName)
            R.string.title_collection -> GameCollectionFragment.newInstance(gameId)
            R.string.title_plays -> GamePlaysFragment.newInstance(gameId, gameName)
            R.string.links -> GameLinksFragment.newInstance(gameId, gameName, iconColor)
            else -> null
        }
    }

    override fun getCount(): Int = tabs.size

    private fun updateTabs() {
        tabs.clear()
        tabs.add(Tab(R.string.title_description, R.drawable.fab_log_play, {
            onPlayFabClicked()
        }))
        tabs.add(Tab(R.string.title_info, R.drawable.fab_log_play, {
            onPlayFabClicked()
        }))
        if (shouldShowCollection())
            tabs.add(Tab(R.string.title_collection, R.drawable.fab_add) {
                onCollectionFabClicked()
            })
        if (shouldShowPlays())
            tabs.add(Tab(R.string.title_plays, R.drawable.fab_log_play) {
                onPlayFabClicked()
            })
        tabs.add(Tab(R.string.links))
    }

    fun displayFab() {
        @DrawableRes val resId = tabs.getOrNull(currentPosition)?.imageResId ?: INVALID_RES_ID
        if (resId != INVALID_RES_ID) {
            if (fab.isShown) {
                fab.hide(object : FloatingActionButton.OnVisibilityChangedListener() {
                    override fun onHidden(fab: FloatingActionButton?) {
                        super.onHidden(fab)
                        fab?.setImageResource(resId)
                        fab?.show()
                    }
                })
            } else {
                fab.setImageResource(resId)
                fab.show()
            }
        } else {
            fab.hide()
        }
    }

    fun onFabClicked() {
        tabs.getOrNull(currentPosition)?.listener?.invoke()
    }

    private fun onCollectionFabClicked() {
        val statusDialogFragment = CollectionStatusDialogFragment.newInstance(rootContainer) { selectedStatuses, wishlistPriority ->
            val task = AddCollectionItemTask(activity, gameId, selectedStatuses, wishlistPriority)
            TaskUtils.executeAsyncTask(task)
        }
        statusDialogFragment.setTitle(R.string.title_add_a_copy)
        DialogUtils.showFragment(activity, statusDialogFragment, "status_dialog")
    }

    private fun onPlayFabClicked() {
        LogPlayActivity.logPlay(activity, gameId, gameName, thumbnailUrl, imageUrl, heroImageUrl, arePlayersCustomSorted)
    }

    private fun shouldShowPlays() = Authenticator.isSignedIn(activity) && PreferencesUtils.getSyncPlays(activity)

    private fun shouldShowCollection() = Authenticator.isSignedIn(activity) && PreferencesUtils.isCollectionSetToSync(activity)

    companion object {
        const val INVALID_RES_ID = 0
    }
}