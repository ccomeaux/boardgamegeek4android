package com.boardgamegeek.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import com.boardgamegeek.R
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.ui.adapter.GeekListsPagedListAdapter
import com.boardgamegeek.ui.viewmodel.GeekListsViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import kotlinx.android.synthetic.main.fragment_geeklists.*

class GeekListsFragment : Fragment(R.layout.fragment_geeklists) {
    private var sortType = GeekListsViewModel.SortType.HOT
    private val viewModel by activityViewModels<GeekListsViewModel>()
    private val adapter: GeekListsPagedListAdapter by lazy {
        GeekListsPagedListAdapter()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sortType = savedInstanceState?.getSerializable(KEY_SORT_TYPE) as? GeekListsViewModel.SortType
                ?: GeekListsViewModel.SortType.HOT
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.setHasFixedSize(true)
        recyclerView.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        recyclerView.adapter = adapter

        viewModel.geekLists.observe(viewLifecycleOwner, Observer { geekListEntities ->
            if (geekListEntities.size == 0) {
                recyclerView.fadeOut()
                emptyView.fadeIn(isResumed)
            } else {
                emptyView.fadeOut()
                adapter.submitList(geekListEntities)
                recyclerView.fadeIn(isResumed)
            }
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
        when (sortType) {
            GeekListsViewModel.SortType.RECENT -> menu.findItem(R.id.menu_sort_geeklists_recent).isChecked = true
            GeekListsViewModel.SortType.ACTIVE -> menu.findItem(R.id.menu_sort_geeklists_active).isChecked = true
            GeekListsViewModel.SortType.HOT -> menu.findItem(R.id.menu_sort_geeklists_hot).isChecked = true
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val sort = when (item.itemId) {
            R.id.menu_sort_geeklists_recent -> GeekListsViewModel.SortType.RECENT
            R.id.menu_sort_geeklists_active -> GeekListsViewModel.SortType.ACTIVE
            R.id.menu_sort_geeklists_hot -> GeekListsViewModel.SortType.HOT
            else -> GeekListsViewModel.SortType.HOT
        }
        if (sort != sortType) {
            sortType = sort
            item.isChecked = true
            adapter.submitList(null)
            viewModel.setSort(sortType)
            FirebaseAnalytics.getInstance(requireContext()).logEvent("Sort") {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "GeekLists")
                param("SortBy", sortType.toString())
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val KEY_SORT_TYPE = "KEY_SORT_TYPE"
    }
}