package com.boardgamegeek.ui

import android.os.Bundle
import android.view.*
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentLinkedCollectionBinding
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.ui.adapter.LinkedCollectionAdapter
import com.boardgamegeek.ui.viewmodel.MechanicViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class MechanicCollectionFragment : Fragment() {
    private var _binding: FragmentLinkedCollectionBinding? = null
    private val binding get() = _binding!!
    private var sortType: CollectionItem.SortType? = null
    private val adapter: LinkedCollectionAdapter by lazy { LinkedCollectionAdapter() }
    private val viewModel by activityViewModels<MechanicViewModel>()

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
                        CollectionItem.SortType.NAME -> R.id.menu_sort_name
                        CollectionItem.SortType.RATING -> R.id.menu_sort_rating
                        else -> View.NO_ID
                    }
                )?.isChecked = true
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.menu_sort_name -> viewModel.setSort(CollectionItem.SortType.NAME)
                    R.id.menu_sort_rating -> viewModel.setSort(CollectionItem.SortType.RATING)
                    R.id.menu_refresh -> viewModel.reload()
                    else -> return false
                }
                return true
            }
        })

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter

        binding.emptyMessage.text = getString(R.string.empty_linked_collection, getString(R.string.title_mechanic).lowercase(Locale.getDefault()))
        binding.swipeRefresh.setOnRefreshListener { viewModel.reload() }

        viewModel.sort.observe(viewLifecycleOwner) {
            sortType = it
            activity?.invalidateOptionsMenu()
        }
        viewModel.collection.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            binding.emptyMessage.isVisible = it.isEmpty()
            binding.recyclerView.isVisible = it.isNotEmpty()
            binding.swipeRefresh.isRefreshing = false
            binding.progressView.hide()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
