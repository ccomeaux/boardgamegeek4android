package com.boardgamegeek.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import com.boardgamegeek.R
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.ui.adapter.GeekListCommentsRecyclerViewAdapter
import com.boardgamegeek.ui.viewmodel.GeekListViewModel
import kotlinx.android.synthetic.main.fragment_geeklist_comments.*

class GeekListCommentsFragment : Fragment(R.layout.fragment_geeklist_comments) {
    private val viewModel by activityViewModels<GeekListViewModel>()
    private val adapter: GeekListCommentsRecyclerViewAdapter by lazy {
        GeekListCommentsRecyclerViewAdapter()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
        recyclerView.adapter = adapter

        viewModel.geekList.observe(viewLifecycleOwner, Observer { (status, data, _) ->
            when (status) {
                Status.REFRESHING -> progressView.show()
                Status.ERROR -> {
                    emptyView.fadeIn()
                    recyclerView.fadeOut()
                    progressView.hide()
                }
                Status.SUCCESS -> {
                    adapter.comments = data?.comments.orEmpty()
                    if (adapter.comments.isEmpty()) {
                        emptyView.fadeIn()
                        recyclerView.fadeOut()
                    } else {
                        emptyView.fadeOut()
                        recyclerView.fadeIn()
                    }
                    progressView.hide()
                }
            }
        })
    }
}
