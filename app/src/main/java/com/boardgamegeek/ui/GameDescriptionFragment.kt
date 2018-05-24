package com.boardgamegeek.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.boardgamegeek.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.model.Game
import com.boardgamegeek.ui.model.Status
import com.boardgamegeek.ui.viewmodel.GameViewModel
import kotlinx.android.synthetic.main.fragment_game_description.*
import kotlinx.android.synthetic.main.include_game_footer.*
import org.jetbrains.anko.support.v4.act

class GameDescriptionFragment : Fragment() {
    private var gameId: Int = 0

    private val viewModel: GameViewModel by lazy {
        ViewModelProviders.of(act).get(GameViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_game_description, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        gameId = arguments?.getInt(ARG_GAME_ID, BggContract.INVALID_ID) ?: BggContract.INVALID_ID
        if (gameId == BggContract.INVALID_ID) throw IllegalArgumentException("Invalid game ID")

        swipeRefresh.setOnRefreshListener { viewModel.refreshGame() }
        swipeRefresh.setBggColors()

        game_info_id.text = gameId.toString()
        game_info_last_updated.timestamp = 0L

        viewModel.getGame(gameId).observe(this, Observer {
            swipeRefresh?.post { swipeRefresh?.isRefreshing = it?.status == Status.REFRESHING }
            when {
                it == null -> showError(getString(R.string.empty_game))
                it.status == Status.ERROR && it.data == null -> showError(it.message)
                it.data == null -> showError(getString(R.string.empty_game))
                else -> showData(it.data)
            }
        })
    }

    private fun showError(message: String?) {
        if (message?.isNotBlank() == true) {
            emptyMessage.text = message
            gameDescription.fadeOut()
            emptyMessage.fadeIn()
            progress.hide()
        }
    }

    private fun showData(game: Game) {
        emptyMessage.fadeOut()
        progress.hide()

        gameDescription.setTextMaybeHtml(game.description)
        gameDescription.fadeIn()

        game_info_id.text = game.id.toString()
        game_info_last_updated.timestamp = game.updated
    }

    companion object {
        private const val ARG_GAME_ID = "GAME_ID"

        @JvmStatic
        fun newInstance(gameId: Int): GameDescriptionFragment {
            val fragment = GameDescriptionFragment()
            val args = Bundle()
            args.putInt(ARG_GAME_ID, gameId)
            fragment.arguments = args
            return fragment
        }
    }
}
