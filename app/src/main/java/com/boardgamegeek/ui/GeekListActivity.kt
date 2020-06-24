package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.boardgamegeek.R
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.createBggUri
import com.boardgamegeek.extensions.linkToBgg
import com.boardgamegeek.extensions.share
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.viewmodel.GeekListViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import org.jetbrains.anko.clearTop
import org.jetbrains.anko.intentFor

class GeekListActivity : TabActivity() {
    private var geekListId = BggContract.INVALID_ID
    private var geekListTitle: String = ""
    private val viewModel by viewModels<GeekListViewModel>()
    private val adapter: GeekListPagerAdapter by lazy {
        GeekListPagerAdapter(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        geekListId = intent.getIntExtra(KEY_ID, BggContract.INVALID_ID)
        geekListTitle = intent.getStringExtra(KEY_TITLE)

        safelySetTitle(geekListTitle)

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "GeekList")
                param(FirebaseAnalytics.Param.ITEM_ID, geekListId.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, geekListTitle)
            }
        }

        viewModel.setId(geekListId)
        viewModel.geekList.observe(this, Observer { (status, data, _) ->
            if (status == Status.SUCCESS && data != null) {
                geekListTitle = data.title
                safelySetTitle(geekListTitle)
            }
        })
    }

    override val optionsMenuId = R.menu.view_share

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_view -> {
                linkToBgg("geeklist", geekListId)
                return true
            }
            R.id.menu_share -> {
                val description = String.format(getString(R.string.share_geeklist_text), geekListTitle)
                val uri = createBggUri("geeklist", geekListId)
                share(getString(R.string.share_geeklist_subject), "$description\n\n$uri")

                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE) {
                    param(FirebaseAnalytics.Param.CONTENT_TYPE, "GeekList")
                    param(FirebaseAnalytics.Param.ITEM_ID, geekListId.toString())
                    param(FirebaseAnalytics.Param.ITEM_ID, geekListTitle)
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun createAdapter(): FragmentStateAdapter {
        return adapter
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            0 -> getString(R.string.title_description)
            1 -> getString(R.string.title_items)
            2 -> getString(R.string.title_comments)
            else -> ""
        }
    }

    private class GeekListPagerAdapter(activity: FragmentActivity) :
            FragmentStateAdapter(activity) {
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> GeekListDescriptionFragment()
                1 -> GeekListItemsFragment()
                2 -> GeekListCommentsFragment()
                else -> ErrorFragment()
            }
        }

        override fun getItemCount() = 3
    }

    companion object {
        private const val KEY_ID = "GEEK_LIST_ID"
        private const val KEY_TITLE = "GEEK_LIST_TITLE"

        fun start(context: Context, id: Int, title: String) {
            context.startActivity(createIntent(context, id, title))
        }

        fun startUp(context: Context, id: Int, title: String) {
            context.startActivity(createIntent(context, id, title).clearTop())
        }

        private fun createIntent(context: Context, id: Int, title: String): Intent {
            return context.intentFor<GeekListActivity>(
                    KEY_ID to id,
                    KEY_TITLE to title
            )
        }
    }
}
