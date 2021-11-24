package com.boardgamegeek.ui

import android.os.Bundle
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.DividerItemDecoration
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentGeeklistsBinding
import com.boardgamegeek.ui.adapter.GeekListsPagedListAdapter
import com.boardgamegeek.ui.viewmodel.GeekListsViewModel
import com.boardgamegeek.ui.viewmodel.GeekListsViewModel.SortType
import kotlinx.coroutines.launch

class GeekListsFragment : Fragment(R.layout.fragment_geeklists) {
    private var _binding: FragmentGeeklistsBinding? = null
    private val binding get() = _binding!!
    private var sortType = SortType.HOT
    private val viewModel by activityViewModels<GeekListsViewModel>()
    private val adapter: GeekListsPagedListAdapter by lazy { GeekListsPagedListAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sortType = savedInstanceState?.getSerializable(KEY_SORT_TYPE) as? SortType ?: SortType.HOT
        setHasOptionsMenu(true)
    }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentGeeklistsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        binding.recyclerView.adapter = adapter

        adapter.addLoadStateListener { loadStates ->
            when (val state = loadStates.refresh) {
                is LoadState.Loading -> {
                    binding.progressView.show()
                }
                is LoadState.NotLoading -> {
                    if (adapter.itemCount == 0) {
                        binding.emptyView.setText(R.string.empty_geeklists)
                        binding.emptyView.isVisible = true
                        binding.recyclerView.isVisible = false
                    } else {
                        binding.emptyView.isVisible = false
                        binding.recyclerView.isVisible = true
                    }
                    binding.progressView.hide()
                }
                is LoadState.Error -> {
                    binding.emptyView.text = state.error.localizedMessage
                    binding.emptyView.isVisible = true
                    binding.recyclerView.isVisible = false
                    binding.progressView.hide()
                }
            }
        }

        viewModel.geekLists.observe(viewLifecycleOwner) { geekListEntities ->
            lifecycleScope.launch { adapter.submitData(geekListEntities) }
            binding.progressView.hide()
        }
        viewModel.setSort(sortType)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(KEY_SORT_TYPE, sortType)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.geeklists, menu)
        menu.findItem(
            when (sortType) {
                SortType.RECENT -> R.id.menu_sort_geeklists_recent
                SortType.ACTIVE -> R.id.menu_sort_geeklists_active
                SortType.HOT -> R.id.menu_sort_geeklists_hot
            }
        )?.isChecked = true
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
