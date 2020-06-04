package com.boardgamegeek.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import com.boardgamegeek.R
import com.boardgamegeek.entities.ForumEntity
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.adapter.ForumsRecyclerViewAdapter
import com.boardgamegeek.ui.viewmodel.ForumsViewModel
import kotlinx.android.synthetic.main.fragment_forums.*
import org.jetbrains.anko.support.v4.withArguments

class ForumsFragment : Fragment(R.layout.fragment_forums) {
    private var forumType = ForumEntity.ForumType.REGION
    private var objectId = BggContract.INVALID_ID
    private var objectName = ""

    private val adapter: ForumsRecyclerViewAdapter by lazy {
        ForumsRecyclerViewAdapter(objectId, objectName, forumType)
    }

    private val viewModel by activityViewModels<ForumsViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            forumType = it.getSerializable(KEY_TYPE) as? ForumEntity.ForumType? ?: ForumEntity.ForumType.REGION
            objectId = it.getInt(KEY_OBJECT_ID, BggContract.INVALID_ID)
            objectName = it.getString(KEY_OBJECT_NAME) ?: ""
        }

        recyclerView?.setHasFixedSize(true)
        recyclerView?.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        recyclerView?.adapter = adapter
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
        viewModel.forums.observe(viewLifecycleOwner, Observer {
            when (it?.status) {
                null, Status.REFRESHING -> {
                    progressView?.show()
                }
                Status.ERROR -> {
                    emptyView?.text = it.message
                    emptyView?.fadeIn()
                    recyclerView?.fadeOut()
                    progressView?.hide()
                }
                Status.SUCCESS -> {
                    adapter.forums = it.data ?: emptyList()
                    if (adapter.itemCount == 0) {
                        emptyView?.fadeIn()
                        recyclerView?.fadeOut()
                    } else {
                        recyclerView?.fadeIn()
                        emptyView?.fadeOut()
                    }
                    progressView?.hide()
                }
            }
        })
    }

    companion object {
        private const val KEY_TYPE = "TYPE"
        private const val KEY_OBJECT_ID = "ID"
        private const val KEY_OBJECT_NAME = "NAME"

        fun newInstance(): ForumsFragment {
            return ForumsFragment().withArguments(
                    KEY_TYPE to ForumEntity.ForumType.REGION,
                    KEY_OBJECT_ID to BggContract.INVALID_ID,
                    KEY_OBJECT_NAME to ""
            )
        }

        fun newInstanceForGame(id: Int, name: String): ForumsFragment {
            return ForumsFragment().withArguments(
                    KEY_TYPE to ForumEntity.ForumType.GAME,
                    KEY_OBJECT_ID to id,
                    KEY_OBJECT_NAME to name
            )
        }

        fun newInstanceForArtist(id: Int, name: String): ForumsFragment {
            return ForumsFragment().withArguments(
                    KEY_TYPE to ForumEntity.ForumType.ARTIST,
                    KEY_OBJECT_ID to id,
                    KEY_OBJECT_NAME to name
            )
        }

        fun newInstanceForDesigner(id: Int, name: String): ForumsFragment {
            return ForumsFragment().withArguments(
                    KEY_TYPE to ForumEntity.ForumType.DESIGNER,
                    KEY_OBJECT_ID to id,
                    KEY_OBJECT_NAME to name
            )
        }

        fun newInstanceForPublisher(id: Int, name: String): ForumsFragment {
            return ForumsFragment().withArguments(
                    KEY_TYPE to ForumEntity.ForumType.PUBLISHER,
                    KEY_OBJECT_ID to id,
                    KEY_OBJECT_NAME to name
            )
        }
    }
}
