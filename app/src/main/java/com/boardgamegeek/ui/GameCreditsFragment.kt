package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.boardgamegeek.R
import com.boardgamegeek.entities.GameDetailEntity
import com.boardgamegeek.entities.GameEntity
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.extensions.setBggColors
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.viewmodel.GameViewModel
import com.boardgamegeek.ui.widget.GameDetailRow
import kotlinx.android.synthetic.main.fragment_game_credits.*
import kotlinx.android.synthetic.main.include_game_footer.*

class GameCreditsFragment : Fragment() {
    private val viewModel by activityViewModels<GameViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_game_credits, container, false)
        val viewGroup: ViewGroup = root.findViewById(R.id.dataContainer)
        viewGroup.layoutTransition.setAnimateParentHierarchy(false)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefresh?.setOnRefreshListener { viewModel.refresh() }
        swipeRefresh?.setBggColors()

        lastModifiedView?.timestamp = 0

        viewModel.gameId.observe(viewLifecycleOwner, Observer { gameId ->
            gameIdView?.text = gameId.toString()
        })

        viewModel.game.observe(viewLifecycleOwner, Observer {
            swipeRefresh?.post { swipeRefresh?.isRefreshing = it?.status == Status.REFRESHING }
            when {
                it == null -> showError(getString(R.string.empty_game))
                it.status == Status.ERROR && it.data == null -> showError(it.message)
                it.data == null -> showError(getString(R.string.empty_game))
                else -> onGameContentChanged(it.data)
            }
            progress.hide()

            viewModel.designers.observe(viewLifecycleOwner, Observer { gameDetails -> onListQueryComplete(gameDetails, game_info_designers) })

            viewModel.artists.observe(viewLifecycleOwner, Observer { gameDetails -> onListQueryComplete(gameDetails, game_info_artists) })

            viewModel.publishers.observe(viewLifecycleOwner, Observer { gameDetails -> onListQueryComplete(gameDetails, game_info_publishers) })

            viewModel.categories.observe(viewLifecycleOwner, Observer { gameDetails -> onListQueryComplete(gameDetails, game_info_categories) })

            viewModel.mechanics.observe(viewLifecycleOwner, Observer { gameDetails -> onListQueryComplete(gameDetails, game_info_mechanics) })
        })
    }

    private fun showError(message: String?) {
        if (message?.isNotBlank() == true) {
            emptyMessage?.text = message
            dataContainer?.fadeOut()
            emptyMessage?.fadeIn()
        }
    }

    private fun colorize(@ColorInt iconColor: Int) {
        if (!isAdded) return

        listOf(game_info_designers, game_info_artists, game_info_publishers, game_info_categories, game_info_mechanics)
                .forEach { it?.colorize(iconColor) }
    }

    private fun onGameContentChanged(game: GameEntity) {
        colorize(game.iconColor)

        gameIdView.text = game.id.toString()
        lastModifiedView.timestamp = game.updated

        emptyMessage.fadeOut()
        dataContainer.fadeIn()
    }

    private fun onListQueryComplete(list: List<GameDetailEntity>?, view: GameDetailRow?) {
        view?.bindData(
                viewModel.gameId.value ?: BggContract.INVALID_ID,
                viewModel.game.value?.data?.name ?: "",
                list)
    }
}