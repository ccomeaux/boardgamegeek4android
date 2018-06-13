package com.boardgamegeek.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.entities.Status
import com.boardgamegeek.entities.YEAR_UNKNOWN
import com.boardgamegeek.fadeIn
import com.boardgamegeek.fadeOut
import com.boardgamegeek.service.SyncService
import com.boardgamegeek.setBggColors
import com.boardgamegeek.ui.adapter.GameCollectionItemAdapter
import com.boardgamegeek.ui.viewmodel.GameViewModel
import kotlinx.android.synthetic.main.fragment_game_collection.*
import org.jetbrains.anko.support.v4.act
import org.jetbrains.anko.support.v4.ctx

class GameCollectionFragment : Fragment() {
    private val adapter: GameCollectionItemAdapter by lazy {
        GameCollectionItemAdapter()
    }

    private val viewModel: GameViewModel by lazy {
        ViewModelProviders.of(act).get(GameViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_game_collection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefresh?.isEnabled = false
        swipeRefresh?.setBggColors()
        syncTimestamp?.timestamp = 0L

        recyclerView?.layoutManager = LinearLayoutManager(ctx)
        recyclerView?.setHasFixedSize(true)
        recyclerView?.adapter = adapter

        viewModel.game.observe(this, Observer {
            adapter.gameYearPublished = it?.data?.yearPublished ?: YEAR_UNKNOWN
        })

        viewModel.collectionItems.observe(this, Observer {
            swipeRefresh?.post { swipeRefresh?.isRefreshing = it?.status == Status.REFRESHING }
            when {
                it == null -> showError()
                it.status == Status.ERROR -> showError(if (it.message.isNotBlank()) it.message else getString(R.string.empty_game_collection))
                else -> showData(it.data)
            }
            progressView.hide()
        })
    }

    private fun showData(items: List<CollectionItemEntity>?) {
        if (!isAdded) return
        if (items != null && items.isNotEmpty()) {
            adapter.items = items
            syncTimestamp.timestamp = items.minBy { it.syncTimestamp }?.syncTimestamp ?: 0L
            emptyMessage.fadeOut()
            recyclerView?.fadeIn()
        } else {
            syncTimestamp.timestamp = System.currentTimeMillis()
            showError()
        }
        swipeRefresh?.setOnRefreshListener {
            if (items != null && items.any { it.isDirty })
                SyncService.sync(ctx, SyncService.FLAG_SYNC_COLLECTION_UPLOAD)
            viewModel.refresh()
        }
        swipeRefresh?.isEnabled = true
    }

    private fun showError(message: String = getString(R.string.empty_game_collection)) {
        emptyMessage.text = message
        emptyMessage.fadeIn()
        recyclerView?.fadeOut()
    }

    companion object {
        private const val ARG_GAME_ID = "GAME_ID"

        fun newInstance(gameId: Int): GameCollectionFragment {
            val args = Bundle()
            args.putInt(ARG_GAME_ID, gameId)
            val fragment = GameCollectionFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
