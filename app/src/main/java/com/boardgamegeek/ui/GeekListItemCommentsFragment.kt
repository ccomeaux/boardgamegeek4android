package com.boardgamegeek.ui

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import com.boardgamegeek.R
import com.boardgamegeek.entities.GeekListCommentEntity
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.ui.adapter.GeekListCommentsRecyclerViewAdapter
import kotlinx.android.synthetic.main.fragment_geeklist_comments.*

class GeekListItemCommentsFragment : Fragment(R.layout.fragment_geeklist_comments) {
    private val adapter: GeekListCommentsRecyclerViewAdapter by lazy {
        GeekListCommentsRecyclerViewAdapter()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
        recyclerView.adapter = adapter

        val comments: List<GeekListCommentEntity> = arguments?.getParcelableArrayList(KEY_COMMENTS)
            ?: emptyList()

        adapter.comments = comments
        if (comments.isEmpty()) {
            emptyView.fadeIn()
            recyclerView.fadeOut()
        } else {
            emptyView.fadeOut()
            recyclerView.fadeIn()
        }
        progressView.hide()
    }

    companion object {
        private const val KEY_COMMENTS = "GEEK_LIST_COMMENTS"

        fun newInstance(comments: List<GeekListCommentEntity>?): GeekListItemCommentsFragment {
            return GeekListItemCommentsFragment().apply {
                arguments = bundleOf(KEY_COMMENTS to comments)
            }
        }
    }
}
