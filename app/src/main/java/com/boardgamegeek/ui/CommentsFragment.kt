package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.recyclerview.widget.DividerItemDecoration
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentCommentsBinding
import com.boardgamegeek.ui.adapter.GameCommentsPagedListAdapter
import com.boardgamegeek.ui.viewmodel.GameCommentsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CommentsFragment : Fragment() {
    private var _binding: FragmentCommentsBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<GameCommentsViewModel>()
    private val adapter by lazy { GameCommentsPagedListAdapter() }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentCommentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        binding.recyclerView.adapter = adapter

        adapter.addLoadStateListener { loadStates ->
            when (val state = loadStates.refresh) {
                is LoadState.Loading -> {
                    binding.progressView.show()
                }
                is LoadState.NotLoading -> {
                    binding.emptyView.setText(R.string.empty_comments)
                    binding.emptyView.isVisible = adapter.itemCount == 0
                    binding.recyclerView.isVisible = adapter.itemCount > 0
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

        viewModel.comments.observe(viewLifecycleOwner) { comments ->
            lifecycleScope.launch {
                adapter.submitData(comments)
                binding.recyclerView.isVisible = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.adapter = null
        _binding = null
    }

    fun clear() {
        lifecycleScope.launch {
            adapter.submitData(PagingData.empty())
        }
    }
}
