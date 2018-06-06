package com.boardgamegeek.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.graphics.Color
import android.os.Bundle
import android.support.annotation.ColorInt
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.boardgamegeek.*
import com.boardgamegeek.entities.Status
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.model.Game
import com.boardgamegeek.ui.model.PlaysByGame
import com.boardgamegeek.ui.viewmodel.GameViewModel
import kotlinx.android.synthetic.main.fragment_game_plays.*
import org.jetbrains.anko.support.v4.act
import org.jetbrains.anko.support.v4.ctx

class GamePlaysFragment : Fragment() {
    private var gameId: Int = BggContract.INVALID_ID
    private var gameName: String? = null
    private var imageUrl: String? = null
    private var thumbnailUrl: String? = null
    private var heroImageUrl: String? = null
    private var arePlayersCustomSorted: Boolean = false
    @ColorInt
    private var iconColor = Color.TRANSPARENT

    private val viewModel: GameViewModel by lazy {
        ViewModelProviders.of(act).get(GameViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_game_plays, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefresh?.setOnRefreshListener { viewModel.refresh() }
        swipeRefresh?.setBggColors()

        syncTimestampView?.timestamp = 0L

        viewModel.game.observe(this, Observer {
            onGameQueryComplete(it?.data)
        })

        viewModel.plays.observe(this, Observer {
            swipeRefresh?.post { swipeRefresh?.isRefreshing = it?.status == Status.REFRESHING }
            onPlaysQueryComplete(it?.data)
        })

        viewModel.playColors.observe(this, Observer {
            val count = it?.size ?: 0
            colorsLabel?.text = ctx.getQuantityText(R.plurals.colors_suffix, count, count)
            colorsContainer?.visibility = View.VISIBLE
            colorsContainer.setOnClickListener {
                if (gameId != BggContract.INVALID_ID)
                    GameColorsActivity.start(context, gameId, gameName, iconColor)
            }
        })
    }

    private fun onGameQueryComplete(game: Game?) {
        if (game == null) return
        gameId = game.id
        gameName = game.name
        imageUrl = game.imageUrl
        thumbnailUrl = game.thumbnailUrl
        heroImageUrl = game.heroImageUrl
        arePlayersCustomSorted = game.customPlayerSort
        syncTimestampView?.timestamp = game.updatedPlays
        iconColor = game.iconColor
        colorize()
    }

    private fun onPlaysQueryComplete(plays: PlaysByGame?) {
        if (plays != null) {
            playsContainer?.visibility = View.VISIBLE

            var description = plays.playCount.asPlayCount(ctx)
            if (description.isNotBlank()) {
                description = " ($description)"
            }
            playsLabel?.text = ctx.getQuantityText(R.plurals.plays_prefix, plays.playCount, plays.playCount, description)

            if (plays.maxDate > 0) {
                lastPlayView?.text = ctx.getText(R.string.last_played_prefix, plays.maxDate.asPastDaySpan(ctx))
                lastPlayView?.visibility = View.VISIBLE
            } else {
                lastPlayView?.visibility = View.GONE
            }
            playsContainer.setOnClickListener {
                if (gameId != BggContract.INVALID_ID)
                    GamePlaysActivity.start(ctx, gameId, gameName, imageUrl, thumbnailUrl, heroImageUrl, arePlayersCustomSorted, iconColor)
            }

            playStatsConatainer?.visibility = if (plays.playCount == 0) View.GONE else View.VISIBLE
            playStatsConatainer.setOnClickListener {
                if (gameId != BggContract.INVALID_ID)
                    GamePlayStatsActivity.start(ctx, gameId, gameName, iconColor)
            }

        } else {
            playsContainer?.visibility = View.GONE
        }
    }

    private fun colorize() {
        if (isAdded) {
            arrayOf(playsIcon, playStatsIcon, colorsIcon).forEach { it.setOrClearColorFilter(iconColor) }
        }
    }
}
