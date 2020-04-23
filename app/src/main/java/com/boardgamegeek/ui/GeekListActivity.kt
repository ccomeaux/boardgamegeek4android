package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import com.boardgamegeek.R
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.linkToBgg
import com.boardgamegeek.extensions.share
import com.boardgamegeek.io.model.GeekListResponse
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.viewmodel.GeekListViewModel
import com.boardgamegeek.util.ActivityUtils
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.ContentViewEvent
import com.crashlytics.android.answers.ShareEvent
import org.jetbrains.anko.clearTop
import org.jetbrains.anko.intentFor

class GeekListActivity : TabActivity() {
    private var geekListId = BggContract.INVALID_ID
    private var geekListTitle: String = ""
    private val viewModel by viewModels<GeekListViewModel>()
    private val adapter: GeekListPagerAdapter by lazy {
        GeekListPagerAdapter(supportFragmentManager, this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        geekListId = intent.getIntExtra(KEY_ID, BggContract.INVALID_ID)
        geekListTitle = intent.getStringExtra(KEY_TITLE)

        safelySetTitle(geekListTitle)

        if (savedInstanceState == null) {
            Answers.getInstance().logContentView(ContentViewEvent()
                    .putContentType("GeekList")
                    .putContentId(geekListId.toString())
                    .putContentName(geekListTitle))
        }

        viewModel.setId(geekListId)
        viewModel.geekList.observe(this, Observer { resource: RefreshableResource<GeekListResponse?>? ->
            if (resource?.status === Status.SUCCESS && resource.data != null) {
                geekListTitle = resource.data.title
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
                val uri = ActivityUtils.createBggUri("geeklist", geekListId)
                share(getString(R.string.share_geeklist_subject), "$description\n\n$uri")

                Answers.getInstance().logShare(ShareEvent()
                        .putContentType("GeekList")
                        .putContentName(geekListTitle)
                        .putContentId(geekListId.toString()))
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun createAdapter(): FragmentPagerAdapter {
        return adapter
    }

    private class GeekListPagerAdapter(fragmentManager: FragmentManager, private val context: Context) :
            FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        override fun getPageTitle(position: Int): CharSequence? {
            return when (position) {
                0 -> context.getString(R.string.title_description)
                1 -> context.getString(R.string.title_items)
                else -> ""
            }
        }

        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> GeekListDescriptionFragment.newInstance()
                1 -> GeekListItemsFragment.newInstance()
                else -> ErrorFragment.newInstance()
            }
        }

        override fun getCount() = 2
    }

    companion object {
        private const val KEY_ID = "GEEK_LIST_ID"
        private const val KEY_TITLE = "GEEK_LIST_TITLE"

        @JvmStatic
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
