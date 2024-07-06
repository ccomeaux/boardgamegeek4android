package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentForumsBinding
import com.boardgamegeek.model.Forum
import com.boardgamegeek.model.Status
import com.boardgamegeek.extensions.getSerializableCompat
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.adapter.ForumsRecyclerViewAdapter
import com.boardgamegeek.ui.viewmodel.ForumsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForumsFragment : Fragment() {
    private var _binding: FragmentForumsBinding? = null
    private val binding get() = _binding!!
    private var forumType = Forum.Type.REGION
    private var objectId = BggContract.INVALID_ID
    private var objectName = ""

    private val recyclerViewAdapter: ForumsRecyclerViewAdapter by lazy {
        ForumsRecyclerViewAdapter(objectId, objectName, forumType)
    }

    private val viewModel by activityViewModels<ForumsViewModel>()

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentForumsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            forumType = it.getSerializableCompat(KEY_TYPE) ?: Forum.Type.REGION
            objectId = it.getInt(KEY_OBJECT_ID, BggContract.INVALID_ID)
            objectName = it.getString(KEY_OBJECT_NAME).orEmpty()
        }

        binding.recyclerView.apply {
            setHasFixedSize(true)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            adapter = recyclerViewAdapter
            if (forumType == Forum.Type.REGION) setPadding(paddingLeft, 0, paddingRight, paddingBottom)
        }

        when (forumType) {
            Forum.Type.GAME -> viewModel.setGameId(objectId)
            Forum.Type.REGION -> viewModel.setRegion()
            Forum.Type.ARTIST,
            Forum.Type.DESIGNER -> viewModel.setPersonId(objectId)
            Forum.Type.PUBLISHER -> viewModel.setCompanyId(objectId)
        }
        viewModel.forums.observe(viewLifecycleOwner) {
            it?.let {
                when (it.status) {
                    Status.REFRESHING -> binding.progressView.show()
                    Status.ERROR -> {
                        binding.emptyView.text = it.message
                        binding.emptyView.isVisible = true
                        binding.recyclerView.isVisible = false
                        binding.progressView.hide()
                    }
                    Status.SUCCESS -> {
                        recyclerViewAdapter.forums = it.data.orEmpty()
                        binding.emptyView.setText(R.string.empty_forums)
                        binding.emptyView.isVisible = recyclerViewAdapter.itemCount == 0
                        binding.recyclerView.isVisible = recyclerViewAdapter.itemCount > 0
                        binding.progressView.hide()
                    }
                }
            }
        }
    }

    companion object {
        private const val KEY_TYPE = "TYPE"
        private const val KEY_OBJECT_ID = "ID"
        private const val KEY_OBJECT_NAME = "NAME"

        fun newInstance(): ForumsFragment {
            return ForumsFragment().apply {
                arguments = bundleOf(
                    KEY_TYPE to Forum.Type.REGION,
                    KEY_OBJECT_ID to BggContract.INVALID_ID,
                    KEY_OBJECT_NAME to "",
                )
            }
        }

        fun newInstanceForGame(id: Int, name: String): ForumsFragment {
            return ForumsFragment().apply {
                arguments = bundleOf(
                    KEY_TYPE to Forum.Type.GAME,
                    KEY_OBJECT_ID to id,
                    KEY_OBJECT_NAME to name,
                )
            }
        }

        fun newInstanceForArtist(id: Int, name: String): ForumsFragment {
            return ForumsFragment().apply {
                arguments = bundleOf(
                    KEY_TYPE to Forum.Type.ARTIST,
                    KEY_OBJECT_ID to id,
                    KEY_OBJECT_NAME to name,
                )
            }
        }

        fun newInstanceForDesigner(id: Int, name: String): ForumsFragment {
            return ForumsFragment().apply {
                arguments = bundleOf(
                    KEY_TYPE to Forum.Type.DESIGNER,
                    KEY_OBJECT_ID to id,
                    KEY_OBJECT_NAME to name,
                )
            }
        }

        fun newInstanceForPublisher(id: Int, name: String): ForumsFragment {
            return ForumsFragment().apply {
                arguments = bundleOf(
                    KEY_TYPE to Forum.Type.PUBLISHER,
                    KEY_OBJECT_ID to id,
                    KEY_OBJECT_NAME to name,
                )
            }
        }
    }
}
