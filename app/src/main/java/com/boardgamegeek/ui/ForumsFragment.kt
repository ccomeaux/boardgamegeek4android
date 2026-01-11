package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentForumsBinding
import com.boardgamegeek.entities.ForumEntity
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.adapter.ForumsRecyclerViewAdapter
import com.boardgamegeek.ui.viewmodel.ForumsViewModel

class ForumsFragment : Fragment() {
    private var _binding: FragmentForumsBinding? = null
    private val binding get() = _binding!!
    private var forumType = ForumEntity.ForumType.REGION
    private var objectId = BggContract.INVALID_ID
    private var objectName: String? = null

    private val adapter: ForumsRecyclerViewAdapter by lazy {
        ForumsRecyclerViewAdapter(objectId, objectName, forumType)
    }

    private val viewModel: ForumsViewModel by lazy {
        ViewModelProvider(requireActivity()).get(ForumsViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentForumsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        forumType = (arguments?.getSerializable(KEY_TYPE) as ForumEntity.ForumType?) ?: ForumEntity.ForumType.REGION
        objectId = arguments?.getInt(KEY_OBJECT_ID, BggContract.INVALID_ID) ?: BggContract.INVALID_ID
        objectName = arguments?.getString(KEY_OBJECT_NAME)

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        binding.recyclerView.adapter = adapter
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        when (forumType) {
            ForumEntity.ForumType.GAME -> viewModel.setGameId(objectId)
            ForumEntity.ForumType.REGION -> viewModel.setRegion()
            ForumEntity.ForumType.ARTIST,
            ForumEntity.ForumType.DESIGNER -> viewModel.setPersonId(objectId)
            ForumEntity.ForumType.PUBLISHER -> viewModel.setCompanyId(objectId)
        }
        viewModel.forums.observe(this, Observer {
            when (it?.status) {
                null, Status.REFRESHING -> {
                    binding.progressView.show()
                }
                Status.ERROR -> {
                    binding.emptyView.text = it.message
                    binding.emptyView.fadeIn()
                    binding.recyclerView.fadeOut()
                    binding.progressView.hide()
                }
                Status.SUCCESS -> {
                    adapter.forums = it.data ?: emptyList()
                    if (adapter.itemCount == 0) {
                        binding.emptyView.fadeIn()
                        binding.recyclerView.fadeOut()
                    } else {
                        binding.recyclerView.fadeIn()
                        binding.emptyView.fadeOut()
                    }
                    binding.progressView.hide()
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val KEY_TYPE = "TYPE"
        private const val KEY_OBJECT_ID = "ID"
        private const val KEY_OBJECT_NAME = "NAME"

        fun newInstance(): ForumsFragment {
            return ForumsFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(KEY_TYPE, ForumEntity.ForumType.REGION)
                    putInt(KEY_OBJECT_ID, BggContract.INVALID_ID)
                    putString(KEY_OBJECT_NAME, "")
                }
            }
        }

        fun newInstanceForGame(id: Int, name: String): ForumsFragment {
            return ForumsFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(KEY_TYPE, ForumEntity.ForumType.GAME)
                    putInt(KEY_OBJECT_ID, id)
                    putString(KEY_OBJECT_NAME, name)
                }
            }
        }

        fun newInstanceForArtist(id: Int, name: String): ForumsFragment {
            return ForumsFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(KEY_TYPE, ForumEntity.ForumType.ARTIST)
                    putInt(KEY_OBJECT_ID, id)
                    putString(KEY_OBJECT_NAME, name)
                }
            }
        }

        fun newInstanceForDesigner(id: Int, name: String): ForumsFragment {
            return ForumsFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(KEY_TYPE, ForumEntity.ForumType.DESIGNER)
                    putInt(KEY_OBJECT_ID, id)
                    putString(KEY_OBJECT_NAME, name)
                }
            }
        }

        fun newInstanceForPublisher(id: Int, name: String): ForumsFragment {
            return ForumsFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(KEY_TYPE, ForumEntity.ForumType.PUBLISHER)
                    putInt(KEY_OBJECT_ID, id)
                    putString(KEY_OBJECT_NAME, name)
                }
            }
        }
    }
}
