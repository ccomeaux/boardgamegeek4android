package com.boardgamegeek.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.entities.Status
import com.boardgamegeek.entities.YEAR_UNKNOWN
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.extensions.setBggColors
import com.boardgamegeek.ui.adapter.GameCollectionItemAdapter
import com.boardgamegeek.ui.viewmodel.GameViewModel
import kotlinx.android.synthetic.main.fragment_game_collection.*
import org.jetbrains.anko.support.v4.toast

class GameCollectionFragment : Fragment(R.layout.fragment_game_collection) {
    private val adapter: GameCollectionItemAdapter by lazy {
        GameCollectionItemAdapter(requireContext())
    }

    private val viewModel by activityViewModels<GameViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefresh?.isEnabled = false
        swipeRefresh?.setBggColors()
        syncTimestamp?.timestamp = 0L

        recyclerView?.setHasFixedSize(false)
        recyclerView?.adapter = adapter

        viewModel.game.observe(viewLifecycleOwner, {
            adapter.gameYearPublished = it?.data?.yearPublished ?: YEAR_UNKNOWN
        })

        viewModel.collectionItems.observe(viewLifecycleOwner, {
            swipeRefresh?.post { swipeRefresh?.isRefreshing = it?.status == Status.REFRESHING }
            when {
                it == null -> showError()
                it.status == Status.ERROR -> {
                    val errorMessage = if (it.message.isNotBlank()) it.message else getString(R.string.empty_game_collection)
                    if (it.data?.isNotEmpty() == true) {
                        showData(it.data)
                        showError(errorMessage, true)
                    } else {
                        showError(errorMessage, false)
                    }
                }
                else -> showData(it.data ?: emptyList())
            }
            progressView.hide()
        })
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun showData(items: List<CollectionItemEntity>) {
        if (!isAdded) return
        if (items.isNotEmpty()) {
            adapter.items = items
            syncTimestamp.timestamp = items.minByOrNull { it.syncTimestamp }?.syncTimestamp ?: 0L
            emptyMessage.fadeOut()
            recyclerView?.fadeIn()
        } else {
            syncTimestamp.timestamp = System.currentTimeMillis()
            showError()
            recyclerView?.fadeOut()
        }
        swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }
        swipeRefresh.isEnabled = true
    }

    private fun showError(message: String = getString(R.string.empty_game_collection), hasData: Boolean = false) {
        if (hasData) {
            toast(message)
        } else {
            emptyMessage.text = message
            emptyMessage.fadeIn()
        }
    }
}
