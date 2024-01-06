package com.boardgamegeek.ui

import android.os.Bundle
import android.view.*
import androidx.annotation.StringRes
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentLinkedCollectionBinding
import com.boardgamegeek.ui.adapter.LinkedCollectionAdapter
import com.boardgamegeek.ui.viewmodel.PersonViewModel
import com.boardgamegeek.ui.viewmodel.PersonViewModel.CollectionSort
import com.boardgamegeek.ui.viewmodel.PersonViewModel.PersonType
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class PersonCollectionFragment : Fragment() {
    private var _binding: FragmentLinkedCollectionBinding? = null
    private val binding get() = _binding!!
    private var sortType = CollectionSort.RATING
    private val adapter: LinkedCollectionAdapter by lazy { LinkedCollectionAdapter() }
    private val viewModel by activityViewModels<PersonViewModel>()

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentLinkedCollectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter

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
                viewModel.sort(
                    when (menuItem.itemId) {
                        R.id.menu_sort_name -> CollectionSort.NAME
                        R.id.menu_sort_rating -> CollectionSort.RATING
                        else -> return false
                    }
                )
                return true
            }
        })

        setEmptyMessage(R.string.title_person)
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        viewModel.type.observe(viewLifecycleOwner) {
            it?.let {
                setEmptyMessage(
                    when (it) {
                        PersonType.ARTIST -> R.string.title_artist
                        PersonType.DESIGNER -> R.string.title_designer
                        PersonType.PUBLISHER -> R.string.title_publisher
                    }
                )
            }
        }
        viewModel.collection.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            binding.emptyMessage.isVisible = it.isEmpty()
            binding.recyclerView.isVisible = it.isNotEmpty()
            binding.progressView.hide()
            binding.swipeRefresh.isRefreshing = false
        }
        viewModel.collectionSort.observe(viewLifecycleOwner) {
            sortType = it ?: CollectionSort.RATING
            activity?.invalidateOptionsMenu()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setEmptyMessage(@StringRes resId: Int) {
        binding.emptyMessage.text = getString(R.string.empty_linked_collection, getString(resId).lowercase(Locale.getDefault()))
    }
}
