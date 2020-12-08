package com.boardgamegeek.ui

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.boardgamegeek.R
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.ui.adapter.LinkedCollectionAdapter
import com.boardgamegeek.ui.viewmodel.CategoryViewModel
import kotlinx.android.synthetic.main.fragment_game_details.*
import java.util.*

class CategoryCollectionFragment : Fragment() {
    private var sortType = CategoryViewModel.CollectionSort.RATING
    private val viewModel by activityViewModels<CategoryViewModel>()

    private val adapter: LinkedCollectionAdapter by lazy {
        LinkedCollectionAdapter()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_linked_collection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView?.setHasFixedSize(true)
        recyclerView?.adapter = adapter
        setHasOptionsMenu(true)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        emptyMessage.text = getString(R.string.empty_linked_collection, getString(R.string.title_category).toLowerCase(Locale.getDefault()))
        viewModel.sort.observe(viewLifecycleOwner, Observer {
            sortType = it
        })
        viewModel.collection.observe(viewLifecycleOwner, Observer {
            if (it?.isNotEmpty() == true) {
                adapter.items = it
                emptyMessage?.fadeOut()
                recyclerView?.fadeIn()
            } else {
                adapter.items = emptyList()
                emptyMessage?.fadeIn()
                recyclerView?.fadeOut()
            }
            progressView?.hide()
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.linked_collection, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(when (sortType) {
            CategoryViewModel.CollectionSort.NAME -> R.id.menu_sort_name
            CategoryViewModel.CollectionSort.RATING -> R.id.menu_sort_rating
        })?.isChecked = true
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        viewModel.setSort(when (item.itemId) {
            R.id.menu_sort_name -> CategoryViewModel.CollectionSort.NAME
            R.id.menu_sort_rating -> CategoryViewModel.CollectionSort.RATING
            else -> return super.onOptionsItemSelected(item)
        })
        return true
    }
}