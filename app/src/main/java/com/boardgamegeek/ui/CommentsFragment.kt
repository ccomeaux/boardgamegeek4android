package com.boardgamegeek.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.recyclerview.widget.DividerItemDecoration
import com.boardgamegeek.R
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.ui.adapter.GameCommentsPagedListAdapter
import com.boardgamegeek.ui.viewmodel.GameCommentsViewModel
import kotlinx.android.synthetic.main.fragment_comments.*
import kotlinx.coroutines.launch

class CommentsFragment : Fragment(R.layout.fragment_comments) {
    private val viewModel by activityViewModels<GameCommentsViewModel>()

    private val adapter by lazy {
        GameCommentsPagedListAdapter()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.setHasFixedSize(true)
        recyclerView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        recyclerView.adapter = adapter

        adapter.addLoadStateListener { loadStates ->
            when (val state = loadStates.refresh) {
                is LoadState.Loading -> {
                    progressView.show()
                }
                is LoadState.NotLoading -> {
                    if (adapter.itemCount == 0) {
                        emptyView.setText(R.string.empty_comments)
                        emptyView.fadeIn()
                        recyclerView.fadeOut()
                    } else {
                        emptyView.fadeOut()
                        recyclerView.fadeIn()
                    }
                    progressView.hide()
                }
                is LoadState.Error -> {
                    emptyView.text = state.error.localizedMessage
                    emptyView.fadeIn()
                    recyclerView.fadeOut()
                    progressView.hide()
                }
            }
        }

        viewModel.comments.observe(viewLifecycleOwner, { comments ->
            lifecycleScope.launch {
                adapter.submitData(comments)
                recyclerView.fadeIn()
            }
        })
    }

    fun clear() {
        lifecycleScope.launch {
            adapter.submitData(PagingData.empty())
        }
    }
}
