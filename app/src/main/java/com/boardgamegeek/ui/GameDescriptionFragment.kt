package com.boardgamegeek.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.boardgamegeek.R
import com.boardgamegeek.entities.GameEntity
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.extensions.setBggColors
import com.boardgamegeek.extensions.setTextMaybeHtml
import com.boardgamegeek.ui.viewmodel.GameViewModel
import kotlinx.android.synthetic.main.fragment_game_description.*
import kotlinx.android.synthetic.main.include_game_footer.*

class GameDescriptionFragment : Fragment(R.layout.fragment_game_description) {
    private val viewModel by activityViewModels<GameViewModel>()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        swipeRefresh?.setOnRefreshListener { viewModel.refresh() }
        swipeRefresh?.setBggColors()

        lastModifiedView.timestamp = 0L

        viewModel.gameId.observe(viewLifecycleOwner, Observer {
            gameIdView.text = it.toString()
        })

        viewModel.game.observe(viewLifecycleOwner, Observer {
            swipeRefresh?.post { swipeRefresh?.isRefreshing = it?.status == Status.REFRESHING }
            when {
                it == null -> showError(getString(R.string.empty_game))
                it.status == Status.ERROR && it.data == null -> showError(it.message)
                it.data == null -> showError(getString(R.string.empty_game))
                else -> showData(it.data)
            }
            progress.hide()
        })
    }

    private fun showError(message: String?) {
        if (message?.isNotBlank() == true) {
            emptyMessage?.text = message
            gameDescription.fadeOut()
            emptyMessage.fadeIn()
        }
    }

    private fun showData(game: GameEntity) {
        emptyMessage.fadeOut()

        gameDescription?.setTextMaybeHtml(game.description)
        gameDescription?.fadeIn()

        gameIdView?.text = game.id.toString()
        lastModifiedView?.timestamp = game.updated
    }
}
