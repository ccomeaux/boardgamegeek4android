package com.boardgamegeek.ui

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.ui.adapter.LinkedCollectionAdapter
import com.boardgamegeek.ui.viewmodel.CategoryViewModel
import com.boardgamegeek.ui.viewmodel.CategoryViewModel.CollectionSort
import kotlinx.android.synthetic.main.fragment_linked_collection.*
import java.util.*

class CategoryCollectionFragment : Fragment(R.layout.fragment_linked_collection) {
    private var sortType = CollectionSort.RATING
    private val viewModel by activityViewModels<CategoryViewModel>()
    private val adapter: LinkedCollectionAdapter by lazy { LinkedCollectionAdapter() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter

        emptyMessage.text = getString(R.string.empty_linked_collection, getString(R.string.title_category).lowercase(Locale.getDefault()))
        swipeRefresh.setOnRefreshListener { viewModel.refresh() }

        viewModel.sort.observe(viewLifecycleOwner, {
            sortType = it
            activity?.invalidateOptionsMenu()
        })
        viewModel.collection.observe(viewLifecycleOwner, {
            if (it?.isNotEmpty() == true) {
                adapter.items = it
                emptyMessage.fadeOut()
                recyclerView.fadeIn()
            } else {
                adapter.items = emptyList()
                emptyMessage.fadeIn()
                recyclerView.fadeOut()
            }
            swipeRefresh.isRefreshing = false
            progressView.hide()
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.linked_collection, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(when (sortType) {
            CollectionSort.NAME -> R.id.menu_sort_name
            CollectionSort.RATING -> R.id.menu_sort_rating
        })?.isChecked = true
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_sort_name -> viewModel.setSort(CollectionSort.NAME)
            R.id.menu_sort_rating -> viewModel.setSort(CollectionSort.RATING)
            R.id.menu_refresh -> viewModel.refresh()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}
