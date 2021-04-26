package com.boardgamegeek.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.ui.adapter.LinkedCollectionAdapter
import com.boardgamegeek.ui.viewmodel.PersonViewModel
import com.boardgamegeek.ui.viewmodel.PersonViewModel.*
import kotlinx.android.synthetic.main.fragment_linked_collection.*
import java.util.*

class PersonCollectionFragment : Fragment(R.layout.fragment_linked_collection) {
    private var sortType = CollectionSort.RATING

    private val adapter: LinkedCollectionAdapter by lazy {
        LinkedCollectionAdapter()
    }

    private val viewModel by activityViewModels<PersonViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter

        setHasOptionsMenu(true)

        setEmptyMessage(R.string.title_person)
        swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        viewModel.person.observe(viewLifecycleOwner, {
            setEmptyMessage(when (it.type) {
                PersonType.ARTIST -> R.string.title_artist
                PersonType.DESIGNER -> R.string.title_designer
                PersonType.PUBLISHER -> R.string.title_publisher
            })
        })
        viewModel.collection.observe(viewLifecycleOwner, {
            it?.let { list ->
                if (list.isNotEmpty()) {
                    adapter.items = it
                    emptyMessage.fadeOut()
                    recyclerView.fadeIn()
                } else {
                    adapter.items = emptyList()
                    emptyMessage.fadeIn()
                    recyclerView.fadeOut()
                }
                progressView.hide()
                swipeRefresh.isRefreshing = false
            }
        })
        viewModel.collectionSort.observe(viewLifecycleOwner, {
            sortType = it ?: CollectionSort.RATING
            activity?.invalidateOptionsMenu()
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
        viewModel.sort(when (item.itemId) {
            R.id.menu_sort_name -> CollectionSort.NAME
            R.id.menu_sort_rating -> CollectionSort.RATING
            else -> return super.onOptionsItemSelected(item)
        })
        return true
    }

    private fun setEmptyMessage(@StringRes resId: Int) {
        emptyMessage.text = getString(R.string.empty_linked_collection, getString(resId).toLowerCase(Locale.getDefault()))
    }
}
