package com.boardgamegeek.ui

import android.os.Bundle
import android.view.*
import androidx.annotation.PluralsRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.databinding.FragmentSearchResultsBinding
import com.boardgamegeek.entities.PlayUploadResult
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.adapter.SearchResultsAdapter
import com.boardgamegeek.ui.adapter.SearchResultsAdapter.Callback
import com.boardgamegeek.ui.viewmodel.SearchViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchResultsFragment : Fragment(), ActionMode.Callback {
    private var _binding: FragmentSearchResultsBinding? = null
    private val binding get() = _binding!!
    private var actionMode: ActionMode? = null

    private val snackbar: Snackbar by lazy {
        Snackbar.make(binding.coordinatorLayout, "", Snackbar.LENGTH_INDEFINITE).apply {
            view.setBackgroundResource(R.color.dark_blue)
            setActionTextColor(ContextCompat.getColor(context, R.color.accent))
        }
    }

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

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentSearchResultsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.progress.progressView.isIndeterminate = true
        binding.recyclerView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        binding.recyclerView.adapter = searchResultsAdapter

        viewModel.errorMessage.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                binding.coordinatorLayout.snackbar(it)
            }
        }

        viewModel.loggedPlayResult.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                requireContext().notifyLoggedPlay(it)
            }
        }

        viewModel.searchResults.observe(viewLifecycleOwner) { resource ->
            resource?.let { (status, data, message) ->
                when (status) {
                    Status.REFRESHING -> binding.progress.progressContainer.isVisible = true
                    Status.ERROR -> {
                        binding.emptyView.text = getString(R.string.search_error, viewModel.query.value?.first.orEmpty(), message)
                        binding.emptyView.isVisible = true
                        binding.recyclerView.isVisible = false
                        binding.progress.progressContainer.isVisible = false
                    }
                    Status.SUCCESS -> {
                        val query = viewModel.query.value
                        if (data.isNullOrEmpty()) {
                            binding.emptyView.setText(
                                if (query == null || query.first.isBlank()) R.string.search_initial_help else R.string.empty_search
                            )
                            searchResultsAdapter.clear()
                            binding.emptyView.isVisible = true
                            binding.recyclerView.isVisible = false
                        } else {
                            searchResultsAdapter.results = data
                            binding.emptyView.isVisible = false
                            binding.recyclerView.isVisible = true
                        }
                        query?.let { showSnackbar(it.first, it.second, data?.size ?: 0) }
                        binding.progress.progressContainer.isVisible = false
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showSnackbar(queryText: String, isExactMatch: Boolean, count: Int) {
        if (queryText.isBlank()) {
            snackbar.dismiss()
        } else {
            @PluralsRes val messageId = if (isExactMatch) R.plurals.search_results_exact else R.plurals.search_results
            snackbar.setText(resources.getQuantityString(messageId, count, count, queryText))
            if (isExactMatch) {
                snackbar.setAction(R.string.more) {
                    viewModel.searchInexact(queryText)
                }
            } else {
                snackbar.setAction("", null)
            }
            snackbar.show()
        }
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.game_context, menu)
        searchResultsAdapter.clearSelections()
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        val count = searchResultsAdapter.selectedItemCount
        if (Authenticator.isSignedIn(context)) {
            menu.findItem(R.id.menu_log_play_form)?.isVisible = count == 1
            menu.findItem(R.id.menu_log_play_wizard)?.isVisible = count == 1
            menu.findItem(R.id.menu_log_play)?.isVisible = true
        } else {
            menu.findItem(R.id.menu_log_play)?.isVisible = false
        }
        menu.findItem(R.id.menu_link)?.isVisible = count == 1
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        actionMode = null
        searchResultsAdapter.clearSelections()
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        if (searchResultsAdapter.getSelectedItems().isEmpty()) return false
        val game = searchResultsAdapter.getSelectedItems().firstOrNull()
        when (item.itemId) {
            R.id.menu_log_play_form -> {
                game?.let {
                    LogPlayActivity.logPlay(requireContext(), it.id, it.name)
                }
                mode.finish()
            }
            R.id.menu_log_play_quick -> {
                context?.toast(
                    resources.getQuantityString(
                        R.plurals.msg_logging_plays,
                        searchResultsAdapter.selectedItemCount
                    )
                )
                searchResultsAdapter.getSelectedItems().forEach {
                    viewModel.logQuickPlay(it.id, it.name)
                }
                mode.finish()
            }
            R.id.menu_log_play_wizard -> {
                game?.let {
                    NewPlayActivity.start(requireContext(), it.id, it.name)
                }
                mode.finish()
            }
            R.id.menu_share -> {
                val shareMethod = "Search"
                if (searchResultsAdapter.selectedItemCount == 1) {
                    game?.let { requireActivity().shareGame(it.id, it.name, shareMethod, firebaseAnalytics) }
                } else {
                    val games = searchResultsAdapter.getSelectedItems().map { it.id to it.name }
                    requireActivity().shareGames(games, shareMethod, firebaseAnalytics)
                }
                mode.finish()
            }
            R.id.menu_link -> {
                game?.let { context.linkBgg(it.id) }
                mode.finish()
            }
            else -> return false
        }
        return false
    }
}
