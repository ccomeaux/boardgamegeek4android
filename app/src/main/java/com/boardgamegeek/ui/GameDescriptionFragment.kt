package com.boardgamegeek.ui


import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
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

class GameDescriptionFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {
    private var gameId: Int = 0
    private lateinit var viewModel: GameViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            gameId = arguments!!.getInt(ARG_GAME_ID, BggContract.INVALID_ID)
        }
        if (gameId == BggContract.INVALID_ID) throw IllegalArgumentException("Invalid game ID")
        viewModel = ViewModelProviders.of(activity!!).get(GameViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_game_description, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipe_refresh.setOnRefreshListener(this)
        swipe_refresh.setBggColors()

        game_info_id.text = gameId.toString()
        game_info_last_updated.timestamp = 0L

        viewModel.getGame(gameId).observe(this, Observer { game ->
            updateRefreshStatus(game?.status == Status.REFRESHING)
            when {
                game == null -> showEmpty()
                game.status == Status.ERROR && game.data == null -> showError(game.message)
                game.data == null -> showEmpty()
                else -> showData(game.data)
            }
        })
    }

    override fun onRefresh() {
        viewModel.refresh()
    }

    private fun showEmpty() {
        showError(getString(R.string.empty_game))
    }

    private fun showError(message: String?) {
        if (message?.isNotBlank() == true) {
            empty.text = message
            game_description.fadeOut()
            empty.fadeIn()
            progress.hide()
        }
    }

    private fun showData(game: Game) {
        empty.fadeOut()
        progress.hide()

        game_description.setTextMaybeHtml(game.description)
        game_description.fadeIn()

        game_info_id.text = game.id.toString()
        game_info_last_updated.timestamp = game.updated
    }

    private fun updateRefreshStatus(refreshing: Boolean) {
        swipe_refresh?.post { swipe_refresh?.isRefreshing = refreshing }
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