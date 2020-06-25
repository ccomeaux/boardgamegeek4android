package com.boardgamegeek.ui

import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.entities.ForumEntity
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.extensions.get
import com.boardgamegeek.extensions.set
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.adapter.ThreadRecyclerViewAdapter
import com.boardgamegeek.ui.viewmodel.ThreadViewModel
import com.boardgamegeek.ui.widget.SafeViewTarget
import com.boardgamegeek.util.HelpUtils
import com.github.amlcurran.showcaseview.ShowcaseView
import com.github.amlcurran.showcaseview.targets.Target
import hugo.weaving.DebugLog
import kotlinx.android.synthetic.main.fragment_thread.*
import org.jetbrains.anko.support.v4.defaultSharedPreferences
import org.jetbrains.anko.support.v4.withArguments
import kotlin.math.abs

class ThreadFragment : Fragment(R.layout.fragment_thread) {
    private var threadId = BggContract.INVALID_ID
    private var forumId = BggContract.INVALID_ID
    private var forumTitle: String = ""
    private var objectId = BggContract.INVALID_ID
    private var objectName = ""
    private var objectType = ForumEntity.ForumType.REGION

    private var showcaseView: ShowcaseView? = null
    private var currentAdapterPosition = 0
    private var latestArticleId: Int = INVALID_ARTICLE_ID

    val viewModel by activityViewModels<ThreadViewModel>()

    private val adapter: ThreadRecyclerViewAdapter by lazy {
        ThreadRecyclerViewAdapter(forumId, forumTitle, objectId, objectName, objectType)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        readBundle(arguments)

        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                currentAdapterPosition = (recyclerView.layoutManager as? LinearLayoutManager)?.findLastCompletelyVisibleItemPosition()
                        ?: RecyclerView.NO_POSITION
                if (currentAdapterPosition != RecyclerView.NO_POSITION) {
                    val currentArticleId = adapter.getItemId(currentAdapterPosition)
                    if (currentArticleId > latestArticleId) {
                        latestArticleId = currentArticleId.toInt()
                    }
                }
            }
        })

        viewModel.setThreadId(threadId)
        viewModel.articles.observe(viewLifecycleOwner, Observer { (status, data, message) ->
            when (status) {
                Status.REFRESHING -> {
                    progressView.show()
                }
                Status.ERROR -> {
                    if (message.isNotEmpty()) {
                        emptyView.text = message
                    } else {
                        emptyView.setText(R.string.empty_thread)
                    }
                    recyclerView.fadeOut()
                    emptyView.fadeIn(isResumed)
                    progressView.hide()
                }
                Status.SUCCESS -> {
                    adapter.threadId = data?.threadId ?: BggContract.INVALID_ID
                    adapter.threadSubject = data?.subject ?: ""
                    if (data == null || data.articles.isEmpty()) {
                        recyclerView.fadeOut()
                        emptyView.fadeIn(isResumed)
                    } else {
                        emptyView.fadeOut()
                        adapter.articles = data.articles
                        recyclerView.fadeIn(isResumed)
                        maybeShowHelp()
                    }
                    progressView.hide()
                }
            }
            activity?.invalidateOptionsMenu()
        })
    }

    private fun readBundle(bundle: Bundle?) {
        bundle?.let {
            threadId = it.getInt(KEY_THREAD_ID, BggContract.INVALID_ID)
            forumId = it.getInt(KEY_FORUM_ID, BggContract.INVALID_ID)
            forumTitle = it.getString(KEY_FORUM_TITLE) ?: ""
            objectId = it.getInt(KEY_OBJECT_ID, BggContract.INVALID_ID)
            objectName = it.getString(KEY_OBJECT_NAME) ?: ""
            objectType = it.getSerializable(KEY_OBJECT_TYPE) as ForumEntity.ForumType
        }
    }

    override fun onResume() {
        super.onResume()
        latestArticleId = defaultSharedPreferences[getThreadKey(threadId), INVALID_ARTICLE_ID]
                ?: INVALID_ARTICLE_ID
    }

    override fun onPause() {
        super.onPause()
        if (latestArticleId != INVALID_ARTICLE_ID) {
            defaultSharedPreferences[getThreadKey(threadId)] = latestArticleId
        }
    }

    private fun getThreadKey(threadId: Int): String {
        return "THREAD-$threadId"
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.thread, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.menu_scroll_last)?.isVisible = latestArticleId != INVALID_ARTICLE_ID && adapter.itemCount > 0
        menu.findItem(R.id.menu_scroll_bottom)?.isVisible = true && adapter.itemCount > 0
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_help -> {
                showHelp()
                return true
            }
            R.id.menu_scroll_last -> {
                scrollToLatestArticle()
                return true
            }
            R.id.menu_scroll_bottom -> {
                scrollToBottom()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun scrollToLatestArticle() {
        if (latestArticleId != INVALID_ARTICLE_ID) {
            scrollToPosition(adapter.getPosition(latestArticleId))
        }
    }

    private fun scrollToBottom() {
        scrollToPosition(adapter.itemCount - 1)
    }

    private fun scrollToPosition(position: Int) {
        if (position != RecyclerView.NO_POSITION) {
            val difference = abs(currentAdapterPosition - position)
            if (difference <= SMOOTH_SCROLL_THRESHOLD) {
                recyclerView.smoothScrollToPosition(position)
            } else {
                recyclerView.scrollToPosition(position)
            }
        }
    }

    @DebugLog
    private fun showHelp() {
        val builder = HelpUtils.getShowcaseBuilder(activity)
        if (builder != null) {
            showcaseView = builder.setContentText(R.string.help_thread)
                    .setTarget(findTarget() ?: Target.NONE)
                    .setOnClickListener {
                        showcaseView?.hide()
                        HelpUtils.updateHelp(context, HelpUtils.HELP_THREAD_KEY, HELP_VERSION)
                    }.build()
            showcaseView?.show()
        }
    }

    private fun findTarget(): Target? {
        val child = HelpUtils.getRecyclerViewVisibleChild(recyclerView)
        return if (child == null) null else SafeViewTarget(child.findViewById(R.id.viewButton))
    }

    private fun maybeShowHelp() {
        if (HelpUtils.shouldShowHelp(context, HelpUtils.HELP_THREAD_KEY, HELP_VERSION)) {
            Handler().postDelayed({ showHelp() }, 100)
        }
    }

    companion object {
        private const val KEY_FORUM_ID = "FORUM_ID"
        private const val KEY_FORUM_TITLE = "FORUM_TITLE"
        private const val KEY_OBJECT_ID = "OBJECT_ID"
        private const val KEY_OBJECT_NAME = "OBJECT_NAME"
        private const val KEY_OBJECT_TYPE = "OBJECT_TYPE"
        private const val KEY_THREAD_ID = "THREAD_ID"
        private const val HELP_VERSION = 2
        private const val SMOOTH_SCROLL_THRESHOLD = 10
        private const val INVALID_ARTICLE_ID = -1

        fun newInstance(threadId: Int, forumId: Int, forumTitle: String?, objectId: Int, objectName: String?, objectType: ForumEntity.ForumType?): ThreadFragment {
            return ThreadFragment().withArguments(
                    KEY_THREAD_ID to threadId,
                    KEY_FORUM_ID to forumId,
                    KEY_FORUM_TITLE to forumTitle,
                    KEY_OBJECT_ID to objectId,
                    KEY_OBJECT_NAME to objectName,
                    KEY_OBJECT_TYPE to objectType
            )
        }
    }
}