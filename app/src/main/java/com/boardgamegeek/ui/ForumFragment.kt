package com.boardgamegeek.ui

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.DividerItemDecoration
import com.boardgamegeek.R
import com.boardgamegeek.entities.ForumEntity
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.adapter.ForumPagedListAdapter
import com.boardgamegeek.ui.viewmodel.ForumViewModel
import kotlinx.android.synthetic.main.fragment_forum.*
import kotlinx.coroutines.launch

class ForumFragment : Fragment(R.layout.fragment_forum) {
    private var forumId = BggContract.INVALID_ID
    private var forumTitle = ""
    private var objectId = BggContract.INVALID_ID
    private var objectName = ""
    private var objectType = ForumEntity.ForumType.REGION

    private val viewModel by activityViewModels<ForumViewModel>()

    private val adapter: ForumPagedListAdapter by lazy {
        ForumPagedListAdapter(forumId, forumTitle, objectId, objectName, objectType)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        arguments?.let {
            forumId = it.getInt(KEY_FORUM_ID, BggContract.INVALID_ID)
            forumTitle = it.getString(KEY_FORUM_TITLE).orEmpty()
            objectId = it.getInt(KEY_OBJECT_ID, BggContract.INVALID_ID)
            objectName = it.getString(KEY_OBJECT_NAME).orEmpty()
            objectType = it.getSerializable(KEY_OBJECT_TYPE) as ForumEntity.ForumType
        }

        recyclerView.setHasFixedSize(true)
        recyclerView.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        recyclerView.adapter = adapter

        adapter.addLoadStateListener { loadStates ->
            when (val state = loadStates.refresh) {
                is LoadState.Loading -> {
                    progressView.show()
                }
                is LoadState.NotLoading -> {
                    if (adapter.itemCount == 0) {
                        emptyView.setText(R.string.empty_forum)
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

        viewModel.threads.observe(viewLifecycleOwner, { threads ->
            lifecycleScope.launch { adapter.submitData(threads) }
            recyclerView.fadeIn()
        })
        viewModel.setForumId(forumId)
    }

    companion object {
        private const val KEY_FORUM_ID = "FORUM_ID"
        private const val KEY_FORUM_TITLE = "FORUM_TITLE"
        private const val KEY_OBJECT_ID = "OBJECT_ID"
        private const val KEY_OBJECT_NAME = "OBJECT_NAME"
        private const val KEY_OBJECT_TYPE = "OBJECT_TYPE"

        fun newInstance(forumId: Int, forumTitle: String?, objectId: Int, objectName: String?, objectType: ForumEntity.ForumType?): ForumFragment {
            return ForumFragment().apply {
                arguments = bundleOf(
                    KEY_FORUM_ID to forumId,
                    KEY_FORUM_TITLE to forumTitle,
                    KEY_OBJECT_ID to objectId,
                    KEY_OBJECT_NAME to objectName,
                    KEY_OBJECT_TYPE to objectType,
                )
            }
        }
    }
}
