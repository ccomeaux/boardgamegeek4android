package com.boardgamegeek.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import com.boardgamegeek.R
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.ui.adapter.GameCommentsPagedListAdapter
import com.boardgamegeek.ui.viewmodel.GameCommentsViewModel
import kotlinx.android.synthetic.main.fragment_comments.*

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

        viewModel.comments.observe(viewLifecycleOwner, Observer { comments ->
            adapter.submitList(comments)
            if (comments.size == 0) {
                recyclerView.fadeOut()
                emptyView.fadeIn(isResumed)
            } else {
                emptyView.fadeOut()
                recyclerView.fadeIn(isResumed)
            }
            progressView.hide()
        })
    }

    fun clear() {
        adapter.submitList(null)
    }
}