package com.boardgamegeek.ui

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentLinkedCollectionBinding
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.ui.adapter.LinkedCollectionAdapter
import com.boardgamegeek.ui.viewmodel.CategoryViewModel
import java.util.*

class CategoryCollectionFragment : Fragment() {
    private var _binding: FragmentLinkedCollectionBinding? = null
    private val binding get() = _binding!!
    private var sortType = CategoryViewModel.CollectionSort.RATING

    private val adapter: LinkedCollectionAdapter by lazy {
        LinkedCollectionAdapter()
    }

    private val viewModel: CategoryViewModel by lazy {
        ViewModelProvider(this).get(CategoryViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLinkedCollectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter
        setHasOptionsMenu(true)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.emptyMessage.text = getString(R.string.empty_linked_collection, getString(R.string.title_category).toLowerCase(Locale.getDefault()))
        viewModel.sort.observe(this, Observer {
            sortType = it
        })
        viewModel.collection.observe(this, Observer {
            if (it?.isNotEmpty() == true) {
                adapter.items = it
                binding.emptyMessage.fadeOut()
                binding.recyclerView.fadeIn()
            } else {
                adapter.items = emptyList()
                binding.emptyMessage.fadeIn()
                binding.recyclerView.fadeOut()
            }
            binding.progressView.hide()
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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

    companion object {
        @JvmStatic
        fun newInstance(): CategoryCollectionFragment {
            return CategoryCollectionFragment()
        }
    }
}