package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewmodel.compose.viewModel
import com.boardgamegeek.databinding.FragmentNestedComposeViewBinding
import com.boardgamegeek.model.Forum
import com.boardgamegeek.extensions.getSerializableCompat
import com.boardgamegeek.model.RefreshableResource
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.compose.ForumsContent
import com.boardgamegeek.ui.viewmodel.ForumsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForumsFragment : Fragment() {
    private var _binding: FragmentNestedComposeViewBinding? = null
    private val binding get() = _binding!!
    private var forumType = Forum.Type.REGION
    private var objectId = BggContract.INVALID_ID
    private var objectName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            forumType = it.getSerializableCompat(KEY_TYPE) ?: Forum.Type.REGION
            objectId = it.getInt(KEY_OBJECT_ID, BggContract.INVALID_ID)
            objectName = it.getString(KEY_OBJECT_NAME).orEmpty()
        }
    }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentNestedComposeViewBinding.inflate(inflater, container, false)

        binding.composeView.setContent {
            val nestedScrollInterop = rememberNestedScrollInteropConnection()

            val viewModel: ForumsViewModel = viewModel()
            val forums = viewModel.forums.observeAsState(RefreshableResource.refreshing(null))

            when (forumType) {
                Forum.Type.GAME -> viewModel.setGameId(objectId)
                Forum.Type.REGION -> viewModel.setRegion()
                Forum.Type.ARTIST,
                Forum.Type.DESIGNER -> viewModel.setPersonId(objectId)
                Forum.Type.PUBLISHER -> viewModel.setCompanyId(objectId)
            }

            ForumsContent(forums.value.data, PaddingValues(0.dp), nestedScrollInterop) { forum, header ->
                ForumActivity.start(requireContext(), forum.id, forum.title, objectId, objectName, forumType, header)
            }
        }

        return binding.root
    }

    companion object {
        private const val KEY_TYPE = "TYPE"
        private const val KEY_OBJECT_ID = "ID"
        private const val KEY_OBJECT_NAME = "NAME"

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
