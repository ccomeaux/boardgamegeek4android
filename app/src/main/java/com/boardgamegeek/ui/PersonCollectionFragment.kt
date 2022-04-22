package com.boardgamegeek.ui

import android.os.Bundle
import android.view.*
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentLinkedCollectionBinding
import com.boardgamegeek.ui.adapter.LinkedCollectionAdapter
import com.boardgamegeek.ui.viewmodel.PersonViewModel
import com.boardgamegeek.ui.viewmodel.PersonViewModel.CollectionSort
import com.boardgamegeek.ui.viewmodel.PersonViewModel.PersonType
import java.util.*

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

        setHasOptionsMenu(true)

        setEmptyMessage(R.string.title_person)
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        viewModel.person.observe(viewLifecycleOwner) {
            setEmptyMessage(
                when (it.type) {
                    PersonType.ARTIST -> R.string.title_artist
                    PersonType.DESIGNER -> R.string.title_designer
                    PersonType.PUBLISHER -> R.string.title_publisher
                }
            )
        }
        viewModel.collection.observe(viewLifecycleOwner) {
            it?.let { list ->
                adapter.items = list
                binding.emptyMessage.isVisible = list.isEmpty()
                binding.recyclerView.isVisible = list.isNotEmpty()
                binding.progressView.hide()
                binding.swipeRefresh.isRefreshing = false
            }
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.linked_collection, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(
            when (sortType) {
                CollectionSort.NAME -> R.id.menu_sort_name
                CollectionSort.RATING -> R.id.menu_sort_rating
            }
        )?.isChecked = true
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        viewModel.sort(
            when (item.itemId) {
                R.id.menu_sort_name -> CollectionSort.NAME
                R.id.menu_sort_rating -> CollectionSort.RATING
                else -> return super.onOptionsItemSelected(item)
            }
        )
        return true
    }

    private fun setEmptyMessage(@StringRes resId: Int) {
        binding.emptyMessage.text = getString(R.string.empty_linked_collection, getString(resId).lowercase(Locale.getDefault()))
    }
}
