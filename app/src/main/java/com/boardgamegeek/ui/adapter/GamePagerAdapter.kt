package com.boardgamegeek.ui.adapter

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.colorize
import com.boardgamegeek.extensions.showAndSurvive
import com.boardgamegeek.ui.*
import com.boardgamegeek.ui.dialog.CollectionStatusDialogFragment
import com.boardgamegeek.ui.viewmodel.GameViewModel
import com.boardgamegeek.util.PreferencesUtils

const val INVALID_RES_ID = 0

class GamePagerAdapter(fragmentManager: FragmentManager, private val activity: FragmentActivity, private val gameId: Int, var gameName: String) :
        FragmentPagerAdapter(fragmentManager) {
    var currentPosition = 0
        set(value) {
            field = value
            displayFab()
        }

    private var thumbnailUrl = ""
    private var imageUrl = ""
    private var heroImageUrl = ""
    private var arePlayersCustomSorted = false
    private var isFavorite = false
    @ColorInt
    private var iconColor = Color.TRANSPARENT
    private val tabs = arrayListOf<Tab>()

    private val fab: FloatingActionButton by lazy { activity.findViewById<FloatingActionButton>(R.id.fab) }
    private val viewModel: GameViewModel by lazy { ViewModelProviders.of(activity).get(GameViewModel::class.java) }

    private inner class Tab(
            @field:StringRes val titleResId: Int,
            @field:DrawableRes var imageResId: Int = INVALID_RES_ID,
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
            R.string.title_plays -> GamePlaysFragment()
            R.string.title_forums -> ForumsFragment.newInstance(gameId, gameName)
            R.string.links -> GameLinksFragment.newInstance(gameId, gameName, iconColor)
            else -> null
        }
    }

    override fun getCount(): Int = tabs.size

    private fun updateTabs() {
        tabs.clear()
        tabs.add(Tab(R.string.title_description, R.drawable.fab_log_play) {
            LogPlayActivity.logPlay(activity, gameId, gameName, thumbnailUrl, imageUrl, heroImageUrl, arePlayersCustomSorted)
        })
        tabs.add(Tab(R.string.title_info, R.drawable.fab_favorite_off) {
            if (updateFavIcon(!isFavorite)) displayFab()
            viewModel.updateFavorite(!isFavorite)
        })
        if (shouldShowCollection())
            tabs.add(Tab(R.string.title_collection, R.drawable.fab_add) {
                activity.showAndSurvive(CollectionStatusDialogFragment.newInstance())
            })
        if (shouldShowPlays())
            tabs.add(Tab(R.string.title_plays, R.drawable.fab_log_play) {
                LogPlayActivity.logPlay(activity, gameId, gameName, thumbnailUrl, imageUrl, heroImageUrl, arePlayersCustomSorted)
            })
        tabs.add(Tab(R.string.title_forums))
        tabs.add(Tab(R.string.links))

        viewModel.game.observe(activity, Observer { resource ->
            if (resource?.status == Status.SUCCESS) {
                resource.data?.let { entity ->
                    gameName = entity.name
                    imageUrl = entity.imageUrl
                    thumbnailUrl = entity.thumbnailUrl
                    heroImageUrl = entity.heroImageUrl
                    arePlayersCustomSorted = entity.customPlayerSort
                    iconColor = entity.iconColor
                    isFavorite = entity.isFavorite
                    updateFavIcon(isFavorite)
                    fab.colorize(iconColor)
                    fab.setOnClickListener { tabs.getOrNull(currentPosition)?.listener?.invoke() }
                    displayFab()
                }
            }
        })
    }

    private fun updateFavIcon(isFavorite: Boolean): Boolean {
        tabs.find { it.titleResId == R.string.title_info }?.let {
            val resId = if (isFavorite) R.drawable.fab_favorite_on else R.drawable.fab_favorite_off
            if (resId != it.imageResId) {
                it.imageResId = resId
                if (it.titleResId == tabs.getOrNull(currentPosition)?.titleResId ?: INVALID_RES_ID)
                    return true
            }
        }
        return false
    }

    private fun displayFab() {
        @DrawableRes val resId = tabs.getOrNull(currentPosition)?.imageResId ?: INVALID_RES_ID
        if (resId != INVALID_RES_ID) {
            val existingResId = fab.getTag(R.id.res_id) as? Int? ?: INVALID_RES_ID
            if (fab.isShown && existingResId != resId) {
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
            fab.setTag(R.id.res_id, resId)
        } else {
            fab.hide()
            fab.setTag(R.id.res_id, INVALID_RES_ID)
        }
    }

    private fun shouldShowPlays() = Authenticator.isSignedIn(activity) && PreferencesUtils.getSyncPlays(activity)

    private fun shouldShowCollection() = Authenticator.isSignedIn(activity) && PreferencesUtils.isCollectionSetToSync(activity)
}