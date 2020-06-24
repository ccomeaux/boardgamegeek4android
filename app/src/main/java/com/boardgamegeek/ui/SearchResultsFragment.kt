package com.boardgamegeek.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.util.Pair
import android.view.*
import androidx.annotation.PluralsRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.adapter.Callback
import com.boardgamegeek.ui.adapter.SearchResultsAdapter
import com.boardgamegeek.ui.viewmodel.SearchViewModel
import com.boardgamegeek.ui.widget.SafeViewTarget
import com.boardgamegeek.util.HelpUtils
import com.github.amlcurran.showcaseview.ShowcaseView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import kotlinx.android.synthetic.main.fragment_search_results.*
import kotlinx.android.synthetic.main.include_horizontal_progress.*
import org.jetbrains.anko.toast
import java.util.*

class SearchResultsFragment : Fragment(), ActionMode.Callback {
    private var showcaseView: ShowcaseView? = null
    private var actionMode: ActionMode? = null

    private val snackbar: Snackbar by lazy {
        Snackbar.make(containerView, "", Snackbar.LENGTH_INDEFINITE).apply {
            view.setBackgroundResource(R.color.dark_blue)
            setActionTextColor(ContextCompat.getColor(context, R.color.accent))
        }
    }

    private val prefs: SharedPreferences by lazy { requireActivity().preferences() }
    private val viewModel by activityViewModels<SearchViewModel>()
    private val firebaseAnalytics by lazy { FirebaseAnalytics.getInstance(requireContext()) }

    private val searchResultsAdapter: SearchResultsAdapter by lazy {
        SearchResultsAdapter(
                object : Callback {
                    override fun onItemClick(position: Int): Boolean {
                        if (actionMode == null) return false
                        toggleSelection(position)
                        return true
                    }

                    override fun onItemLongClick(position: Int): Boolean {
                        if (actionMode != null) return false
                        actionMode = requireActivity().startActionMode(this@SearchResultsFragment)
                        if (actionMode == null) return false
                        toggleSelection(position)
                        return true
                    }

                    private fun toggleSelection(position: Int) {
                        searchResultsAdapter.toggleSelection(position)
                        val count = searchResultsAdapter.selectedItemCount
                        if (count == 0) {
                            actionMode?.finish()
                        } else {
                            actionMode?.title = resources.getQuantityString(R.plurals.msg_games_selected, count, count)
                            actionMode?.invalidate()
                        }
                    }
                })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        viewModel.searchResults.observe(this, Observer { resource ->
            if (resource == null) return@Observer

            when (resource.status) {
                Status.REFRESHING -> progressContainer.fadeIn()
                Status.ERROR -> {
                    if (resource.message.isBlank()) {
                        emptyView.setText(R.string.empty_http_error) // TODO better message?
                    } else {
                        emptyView.text = getString(R.string.empty_http_error, resource.message)
                    }
                    emptyView.fadeIn()
                    recyclerView.fadeOut()
                    progressContainer.fadeOut()
                }
                Status.SUCCESS -> {
                    val data = resource.data
                    val query = viewModel.query.value
                    if (data == null || data.isEmpty()) {
                        if (query != null && query.second)
                            viewModel.searchInexact(query.first)
                        if (query == null || query.first.isBlank()) {
                            emptyView.setText(R.string.search_initial_help)
                        } else {
                            emptyView.setText(R.string.empty_search)
                        }
                        searchResultsAdapter.clear()
                        emptyView.fadeIn()
                        recyclerView.fadeOut()
                    } else {
                        searchResultsAdapter.results = data
                        emptyView.fadeOut()
                        recyclerView.fadeIn(isResumed)
                    }
                    if (query != null) {
                        showSnackbar(query.first, query.second, data?.size ?: 0)
                    }
                    progressContainer.fadeOut()
                }
            }

            maybeShowHelp()
        })
    }

    private fun showSnackbar(queryText: String, isExactMatch: Boolean, count: Int) {
        if (queryText.isBlank()) {
            snackbar.dismiss()
        } else {
            @PluralsRes val messageId = if (isExactMatch) R.plurals.search_results_exact else R.plurals.search_results
            snackbar.setText(resources.getQuantityString(messageId, count, count, queryText))
            if (isExactMatch) {
                snackbar.setAction(R.string.more) {
                    firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SEARCH) {
                        param(FirebaseAnalytics.Param.SEARCH_TERM, queryText)
                        param("exact", false.toString())
                    }
                    viewModel.searchInexact(queryText)
                }
            } else {
                snackbar.setAction("", null)
            }
            snackbar.show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_search_results, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressView.isIndeterminate = true
        recyclerView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        recyclerView.adapter = searchResultsAdapter
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.help, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_help) {
            showHelp()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun maybeShowHelp() {
        if (HelpUtils.shouldShowHelp(context, HelpUtils.HELP_SEARCHRESULTS_KEY, HELP_VERSION)) {
            Handler().postDelayed({ showHelp() }, 100)
        }
    }

    private fun showHelp() {
        val builder = HelpUtils.getShowcaseBuilder(activity)
        if (builder != null) {
            builder.setContentText(R.string.help_searchresults)
                    .setOnClickListener {
                        showcaseView?.hide()
                        HelpUtils.updateHelp(context, HelpUtils.HELP_SEARCHRESULTS_KEY, HELP_VERSION)
                    }
            val viewTarget = SafeViewTarget(HelpUtils.getRecyclerViewVisibleChild(recyclerView))
            builder.setTarget(viewTarget)
            showcaseView = builder.build()?.apply {
                this.setButtonPosition(HelpUtils.getCenterLeftLayoutParams(context))
                this.show()
            }
        }
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.game_context, menu)
        searchResultsAdapter.clearSelections()
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        val count = searchResultsAdapter.selectedItemCount
        menu.findItem(R.id.menu_log_play).isVisible = Authenticator.isSignedIn(context) && count == 1 && prefs.showLogPlay()
        menu.findItem(R.id.menu_log_play_quick).isVisible = Authenticator.isSignedIn(context) && prefs.showQuickLogPlay()
        menu.findItem(R.id.menu_link).isVisible = count == 1
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        actionMode = null
        searchResultsAdapter.clearSelections()
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        if (searchResultsAdapter.getSelectedItems().isEmpty()) {
            return false
        }
        val game = searchResultsAdapter.getItem(searchResultsAdapter.getSelectedItems()[0])
        when (item.itemId) {
            R.id.menu_log_play -> {
                game?.let {
                    LogPlayActivity.logPlay(context, it.id, it.name, null, null, null, false)
                }
                mode.finish()
                return true
            }
            R.id.menu_log_play_quick -> {
                context?.toast(resources.getQuantityString(R.plurals.msg_logging_plays, searchResultsAdapter.selectedItemCount))
                for (position in searchResultsAdapter.getSelectedItems()) {
                    searchResultsAdapter.getItem(position)?.let {
                        requireActivity().logQuickPlay(it.id, it.name)
                    }
                }
                mode.finish()
                return true
            }
            R.id.menu_share -> {
                val shareMethod = "Search"
                if (searchResultsAdapter.selectedItemCount == 1) {
                    game?.let { requireActivity().shareGame(it.id, it.name, shareMethod, firebaseAnalytics) }
                } else {
                    val games = ArrayList<Pair<Int, String>>(searchResultsAdapter.selectedItemCount)
                    for (position in searchResultsAdapter.getSelectedItems()) {
                        searchResultsAdapter.getItem(position)?.let {
                            games.add(Pair.create(it.id, it.name))
                        }
                    }
                    requireActivity().shareGames(games, shareMethod, firebaseAnalytics)
                }
                mode.finish()
                return true
            }
            R.id.menu_link -> {
                game?.let { context.linkBgg(it.id) }
                mode.finish()
                return true
            }
        }
        return false
    }

    companion object {
        private const val HELP_VERSION = 2
    }
}
