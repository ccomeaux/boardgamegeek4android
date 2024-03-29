package com.boardgamegeek.ui

import android.os.Bundle
import android.view.*
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentLinkedCollectionBinding
import com.boardgamegeek.ui.adapter.LinkedCollectionAdapter
import com.boardgamegeek.ui.viewmodel.CategoryViewModel
import com.boardgamegeek.ui.viewmodel.CategoryViewModel.CollectionSort
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class CategoryCollectionFragment : Fragment() {
    private var _binding: FragmentLinkedCollectionBinding? = null
    private val binding get() = _binding!!
    private var sortType = CollectionSort.RATING
    private val viewModel by activityViewModels<CategoryViewModel>()
    private val adapter: LinkedCollectionAdapter by lazy { LinkedCollectionAdapter() }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentLinkedCollectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.linked_collection, menu)
            }

            override fun onPrepareMenu(menu: Menu) {
                menu.findItem(
                    when (sortType) {
                        CollectionSort.NAME -> R.id.menu_sort_name
                        CollectionSort.RATING -> R.id.menu_sort_rating
                    }
                )?.isChecked = true
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.menu_sort_name -> viewModel.setSort(CollectionSort.NAME)
                    R.id.menu_sort_rating -> viewModel.setSort(CollectionSort.RATING)
                    R.id.menu_refresh -> viewModel.refresh()
                    else -> return false
                }
                return true
            }
        })

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter

        binding.emptyMessage.text = getString(R.string.empty_linked_collection, getString(R.string.title_category).lowercase(Locale.getDefault()))
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }

        viewModel.sort.observe(viewLifecycleOwner) {
            sortType = it
            activity?.invalidateOptionsMenu()
        }
        viewModel.collection.observe(viewLifecycleOwner) {
            adapter.items = it.orEmpty()
            binding.emptyMessage.isVisible = adapter.items.isEmpty()
            binding.recyclerView.isVisible = adapter.items.isNotEmpty()
            binding.swipeRefresh.isRefreshing = false
            binding.progressView.hide()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
