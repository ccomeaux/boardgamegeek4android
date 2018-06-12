package com.boardgamegeek.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.boardgamegeek.R
import com.boardgamegeek.entities.Status
import com.boardgamegeek.fadeIn
import com.boardgamegeek.fadeOut
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.adapter.ForumsRecyclerViewAdapter
import com.boardgamegeek.ui.viewmodel.ForumsViewModel
import kotlinx.android.synthetic.main.fragment_forums.*
import org.jetbrains.anko.support.v4.act
import org.jetbrains.anko.support.v4.ctx

class ForumsFragment : Fragment() {
    private var gameId = BggContract.INVALID_ID
    private var gameName: String? = null

    private val adapter: ForumsRecyclerViewAdapter by lazy {
        ForumsRecyclerViewAdapter(gameId, gameName)
    }

    private val viewModel: ForumsViewModel by lazy {
        ViewModelProviders.of(act).get(ForumsViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_forums, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        gameId = arguments?.getInt(KEY_GAME_ID, BggContract.INVALID_ID) ?: BggContract.INVALID_ID
        gameName = arguments?.getString(KEY_GAME_NAME)

        recyclerView?.layoutManager = LinearLayoutManager(ctx, LinearLayoutManager.VERTICAL, false)
        recyclerView?.setHasFixedSize(true)
        recyclerView?.addItemDecoration(DividerItemDecoration(ctx, DividerItemDecoration.VERTICAL))
        recyclerView?.adapter = adapter
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (gameId == BggContract.INVALID_ID) {
            viewModel.setRegion()
        } else {
            viewModel.setGameId(gameId)
        }
        viewModel.forums.observe(this, Observer {
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
        private const val KEY_GAME_ID = "GAME_ID"
        private const val KEY_GAME_NAME = "GAME_NAME"

        fun newInstance(): ForumsFragment = ForumsFragment()

        fun newInstance(gameId: Int, gameName: String): ForumsFragment {
            val args = Bundle()
            args.putInt(KEY_GAME_ID, gameId)
            args.putString(KEY_GAME_NAME, gameName)

            val fragment = ForumsFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
