package com.boardgamegeek.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.DividerItemDecoration
import com.boardgamegeek.R
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.ui.adapter.GeekListsPagedListAdapter
import com.boardgamegeek.ui.viewmodel.GeekListsViewModel
import com.boardgamegeek.ui.viewmodel.GeekListsViewModel.SortType
import kotlinx.android.synthetic.main.fragment_geeklists.emptyView
import kotlinx.android.synthetic.main.fragment_geeklists.progressView
import kotlinx.android.synthetic.main.fragment_geeklists.recyclerView
import kotlinx.coroutines.launch

class GeekListsFragment : Fragment(R.layout.fragment_geeklists) {
    private var sortType = SortType.HOT
    private val viewModel by activityViewModels<GeekListsViewModel>()
    private val adapter: GeekListsPagedListAdapter by lazy {
        GeekListsPagedListAdapter()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sortType = savedInstanceState?.getSerializable(KEY_SORT_TYPE) as? SortType ?: SortType.HOT
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.setHasFixedSize(true)
        recyclerView.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        recyclerView.adapter = adapter

        adapter.addLoadStateListener { loadStates ->
            when (val state = loadStates.refresh) {
                is LoadState.Loading -> {
                    progressView.show()
                }
                is LoadState.NotLoading -> {
                    if (adapter.itemCount == 0) {
                        emptyView.setText(R.string.empty_geeklists)
                        emptyView.fadeIn()
                        recyclerView.fadeOut()
                    } else {
                        emptyView.fadeOut()
                        recyclerView.fadeIn()
                    }
                    progressView.hide()
                }
                is LoadState.Error -> {
                    emptyView.text = state.error.localizedMessage
                    emptyView.fadeIn()
                    recyclerView.fadeOut()
                    progressView.hide()
                }
            }
        }

        viewModel.geekLists.observe(viewLifecycleOwner, { geekListEntities ->
            lifecycleScope.launch { adapter.submitData(geekListEntities) }
            progressView.hide()
        })
        viewModel.setSort(sortType)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(KEY_SORT_TYPE, sortType)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.geeklists, menu)
        menu.findItem(when (sortType) {
            SortType.RECENT -> R.id.menu_sort_geeklists_recent
            SortType.ACTIVE -> R.id.menu_sort_geeklists_active
            SortType.HOT -> R.id.menu_sort_geeklists_hot
        })?.isChecked = true
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_sort_geeklists_recent -> SortType.RECENT
            R.id.menu_sort_geeklists_active -> SortType.ACTIVE
            R.id.menu_sort_geeklists_hot -> SortType.HOT
            else -> null
        }?.let {
            if (it != sortType) {
                sortType = it
                item.isChecked = true
                viewModel.setSort(sortType)
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val KEY_SORT_TYPE = "KEY_SORT_TYPE"
    }
}
