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
import android.widget.ImageView
import android.widget.LinearLayout
import com.boardgamegeek.*
import com.boardgamegeek.entities.PlayEntity
import com.boardgamegeek.entities.Status
import com.boardgamegeek.events.PlaySelectedEvent
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.model.Game
import com.boardgamegeek.ui.viewmodel.GameViewModel
import com.boardgamegeek.util.ColorUtils
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
            progressView.hide()
        })

        viewModel.playColors.observe(this, Observer {
            updateColors(it)
            progressView.hide()
        })
    }

    private fun updateColors(colors: List<String>?) {
        val count = colors?.size ?: 0
        if (colors != null && count > 0 && colors.all { ColorUtils.isKnownColor(it) }) {
            colorsList.removeAllViews()
            colors.forEach {
                val view = createViewToBeColored()
                view.setColorViewValue(ColorUtils.parseColor(it))
                colorsList.addView(view)
            }
            colorsList?.visibility = View.VISIBLE
            colorsLabel?.visibility = View.GONE
        } else {
            colorsLabel?.text = ctx.getQuantityText(R.plurals.colors_suffix, count, count)
            colorsLabel?.visibility = View.VISIBLE
            colorsList?.visibility = View.GONE
        }
        colorsContainer?.visibility = View.VISIBLE
        colorsContainer.setOnClickListener {
            if (gameId != BggContract.INVALID_ID)
                GameColorsActivity.start(context, gameId, gameName, iconColor)
        }
    }

    private fun createViewToBeColored(): ImageView {
        val view = ImageView(activity)
        val size = resources.getDimensionPixelSize(R.dimen.color_circle_diameter_small)
        val margin = resources.getDimensionPixelSize(R.dimen.color_circle_diameter_small_margin)
        val lp = LinearLayout.LayoutParams(size, size)
        lp.setMargins(margin, margin, margin, margin)
        view.layoutParams = lp
        return view
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

    private fun onPlaysQueryComplete(plays: List<PlayEntity>?) {
        if (plays != null && plays.isNotEmpty()) {
            playsContainer?.visibility = View.VISIBLE

            val playCount = plays.sumBy { it.quantity }
            val description = playCount.asPlayCount(ctx)
            playCountIcon?.text = description.first.toString()
            playsLabel?.text = ctx.getQuantityText(R.plurals.plays_prefix, playCount, playCount, if (description.second.isNotBlank()) " (${description.second})" else "")
            playCountBackground?.setColorViewValue(description.third)

            val lastPlay = plays.maxBy { it.dateInMillis }!!
            lastPlayDateView?.text = ctx.getText(R.string.last_played_prefix, lastPlay.dateInMillis.asPastDaySpan(ctx))
            lastPlayInfoView?.text = lastPlay.describe(ctx)
            val event = PlaySelectedEvent(lastPlay.internalId, lastPlay.gameId, lastPlay.gameName,
                    thumbnailUrl ?: "", imageUrl ?: "", heroImageUrl ?: "")
            lastPlayContainer?.setOnClickListener { PlayActivity.start(ctx, event) }
            lastPlayContainer?.visibility = View.VISIBLE

            playsContainer.setOnClickListener {
                if (gameId != BggContract.INVALID_ID)
                    GamePlaysActivity.start(ctx, gameId, gameName, imageUrl, thumbnailUrl, heroImageUrl, arePlayersCustomSorted, iconColor)
            }

            playStatsContainer?.visibility = View.VISIBLE
            playStatsContainer?.setOnClickListener {
                if (gameId != BggContract.INVALID_ID)
                    GamePlayStatsActivity.start(ctx, gameId, gameName, iconColor)
            }
        } else {
            playsContainer?.visibility = View.GONE
            lastPlayContainer?.visibility = View.GONE
        }
    }

    private fun colorize() {
        if (isAdded) {
            arrayOf(playsIcon, playStatsIcon, colorsIcon).forEach { it.setOrClearColorFilter(iconColor) }
        }
    }
}
