package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.DividerItemDecoration
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentForumBinding
import com.boardgamegeek.entities.Forum
import com.boardgamegeek.extensions.getSerializableCompat
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.adapter.ForumPagedListAdapter
import com.boardgamegeek.ui.viewmodel.ForumViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ForumFragment : Fragment() {
    private var _binding: FragmentForumBinding? = null
    private val binding get() = _binding!!
    private var forumId = BggContract.INVALID_ID
    private var forumTitle = ""
    private var objectId = BggContract.INVALID_ID
    private var objectName = ""
    private var objectType = Forum.Type.REGION

    private val viewModel by activityViewModels<ForumViewModel>()

    private val adapter: ForumPagedListAdapter by lazy {
        ForumPagedListAdapter(forumId, forumTitle, objectId, objectName, objectType)
    }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentForumBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        arguments?.let {
            forumId = it.getInt(KEY_FORUM_ID, BggContract.INVALID_ID)
            forumTitle = it.getString(KEY_FORUM_TITLE).orEmpty()
            objectId = it.getInt(KEY_OBJECT_ID, BggContract.INVALID_ID)
            objectName = it.getString(KEY_OBJECT_NAME).orEmpty()
            objectType = it.getSerializableCompat(KEY_OBJECT_TYPE) ?: Forum.Type.REGION
        }

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        binding.recyclerView.adapter = adapter

        adapter.addLoadStateListener { loadStates ->
            when (val state = loadStates.refresh) {
                is LoadState.Loading -> {
                    binding.progressView.show()
                }
                is LoadState.NotLoading -> {
                    binding.emptyView.setText(R.string.empty_forum)
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

        viewModel.threads.observe(viewLifecycleOwner) { threads ->
            lifecycleScope.launch { adapter.submitData(threads) }
            binding.recyclerView.isVisible = true
        }
        viewModel.setForumId(forumId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val KEY_FORUM_ID = "FORUM_ID"
        private const val KEY_FORUM_TITLE = "FORUM_TITLE"
        private const val KEY_OBJECT_ID = "OBJECT_ID"
        private const val KEY_OBJECT_NAME = "OBJECT_NAME"
        private const val KEY_OBJECT_TYPE = "OBJECT_TYPE"

        fun newInstance(forumId: Int, forumTitle: String?, objectId: Int, objectName: String?, objectType: Forum.Type?): ForumFragment {
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
