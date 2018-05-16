package com.boardgamegeek.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.boardgamegeek.R
import com.boardgamegeek.fadeIn
import com.boardgamegeek.fadeOut
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.setBggColors
import com.boardgamegeek.ui.model.GameCollectionItem
import com.boardgamegeek.ui.model.Status
import com.boardgamegeek.ui.viewmodel.GameViewModel
import com.boardgamegeek.ui.widget.GameCollectionRow
import kotlinx.android.synthetic.main.fragment_game_collection.*
import org.jetbrains.anko.support.v4.act

class GameCollectionFragment : Fragment() {
    private var gameId: Int = 0

    private val viewModel: GameViewModel by lazy {
        ViewModelProviders.of(act).get(GameViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_game_collection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        gameId = arguments?.getInt(ARG_GAME_ID, BggContract.INVALID_ID) ?: BggContract.INVALID_ID
        if (gameId == BggContract.INVALID_ID) throw IllegalArgumentException("Invalid game ID")

        swipeRefresh.setOnRefreshListener { viewModel.refreshCollectionItems() }
        swipeRefresh.setBggColors()

        syncTimestamp.timestamp = 0L

        viewModel.getGameCollection().observe(this, Observer { items ->
            swipeRefresh?.post { swipeRefresh?.isRefreshing = items?.status == Status.REFRESHING }
            when {
                items == null -> showError()
                items.status == Status.ERROR -> showError(if (items.message.isNotBlank()) items.message else getString(R.string.empty_game_collection))
                else -> showData(items.data)
            }
        })
    }

    private fun showData(items: List<GameCollectionItem>?) {
        if (activity == null) return
        if (items != null) {
            emptyMessage.fadeOut()
            collectionContainer.removeAllViews()
            for (item in items) {
                val row = GameCollectionRow(context)
                item.apply {
                    row.bind(internalId, gameId, gameName, collectionId, yearPublished, imageUrl)
                    row.setThumbnail(thumbnailUrl)
                    row.setStatus(statuses, numberOfPlays, rating, comment)
                    row.setDescription(collectionName, collectionYearPublished)
                    row.setComment(comment)
                    row.setRating(rating)
                }
                collectionContainer.addView(row)
            }
            syncTimestamp.timestamp = items.minBy { it.syncTimestamp }?.syncTimestamp ?: 0L
            progress.hide()
        } else {
            showError()
            syncTimestamp.timestamp = System.currentTimeMillis()
        }
    }

    private fun showError(message: String = getString(R.string.empty_game_collection)) {
        emptyMessage.text = message
        emptyMessage.fadeIn()
        collectionContainer.removeAllViews()
        progress.hide()
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
