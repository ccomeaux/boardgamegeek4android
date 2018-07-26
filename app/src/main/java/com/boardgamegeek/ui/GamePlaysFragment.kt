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
import android.widget.TextView
import com.boardgamegeek.*
import com.boardgamegeek.entities.GameEntity
import com.boardgamegeek.entities.PlayEntity
import com.boardgamegeek.entities.Status
import com.boardgamegeek.events.PlaySelectedEvent
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.viewmodel.GameViewModel
import com.boardgamegeek.util.ColorUtils
import kotlinx.android.synthetic.main.fragment_game_plays.*
import org.jetbrains.anko.support.v4.act
import org.jetbrains.anko.support.v4.ctx

class GamePlaysFragment : Fragment() {
    private var gameId = BggContract.INVALID_ID
    private var gameName = ""
    private var imageUrl = ""
    private var thumbnailUrl = ""
    private var heroImageUrl = ""
    private var arePlayersCustomSorted = false
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
            colorsList?.fadeIn()
            colorsLabel?.fadeOut()
        } else {
            colorsLabel?.text = ctx.getQuantityText(R.plurals.colors_suffix, count, count)
            colorsLabel?.fadeIn()
            colorsList?.fadeOut()
        }
        colorsContainer?.fadeIn()
        colorsContainer.setOnClickListener {
            if (gameId != BggContract.INVALID_ID)
                GameColorsActivity.start(ctx, gameId, gameName, iconColor)
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

    private fun onGameQueryComplete(game: GameEntity?) {
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
        if (plays != null) {
            if (plays.isNotEmpty()) {
                val inProgressPlays = plays.filter { it.dirtyTimestamp > 0 }
                if (inProgressPlays.isNotEmpty()) {
                    inProgressPlaysList?.removeAllViews()
                    inProgressPlays.forEach { play ->
                        val row = LayoutInflater.from(ctx).inflate(R.layout.row_play_summary, inProgressPlaysList, false)
                        val title = if (play.startTime > 0) play.startTime.asPastMinuteSpan(ctx) else play.dateInMillis.asPastDaySpan(ctx)
                        row.findViewById<TextView>(R.id.line1)?.text = title
                        row.findViewById<TextView>(R.id.line2)?.setTextOrHide(play.describe(ctx))
                        row.setOnClickListener {
                            val event = PlaySelectedEvent(play.internalId, play.gameId, play.gameName, thumbnailUrl, imageUrl, heroImageUrl)
                            PlayActivity.start(ctx, event)
                        }
                        inProgressPlaysList?.addView(row)
                    }
                    inProgressPlaysContainer?.fadeIn()
                } else {
                    inProgressPlaysContainer?.fadeOut()
                }
            }

            val playCount = plays.sumBy { it.quantity }
            val description = playCount.asPlayCount(ctx)
            playCountIcon?.text = description.first.toString()
            playCountView?.text = ctx.getQuantityText(R.plurals.play_title_suffix, playCount, playCount)
            playCountDescriptionView?.setTextOrHide(description.second)
            playCountBackground?.setColorViewValue(description.third)
            playCountContainer?.setOnClickListener {
                if (gameId != BggContract.INVALID_ID)
                    GamePlaysActivity.start(ctx, gameId, gameName, imageUrl, thumbnailUrl, heroImageUrl, arePlayersCustomSorted, iconColor)
            }
            playCountContainer?.fadeIn()

            if (plays.isNotEmpty()) {
                val lastPlay = plays.filter { it.dirtyTimestamp == 0L }.maxBy { it.dateInMillis }
                if (lastPlay != null) {
                    lastPlayDateView?.text = ctx.getText(R.string.last_played_prefix, lastPlay.dateInMillis.asPastDaySpan(ctx))
                    lastPlayInfoView?.text = lastPlay.describe(ctx)
                    val event = PlaySelectedEvent(lastPlay.internalId, lastPlay.gameId, lastPlay.gameName, thumbnailUrl, imageUrl, heroImageUrl)
                    lastPlayContainer?.setOnClickListener { PlayActivity.start(ctx, event) }
                    lastPlayContainer?.fadeIn()
                } else {
                    lastPlayContainer?.fadeOut()
                }

                playStatsContainer?.setOnClickListener {
                    if (gameId != BggContract.INVALID_ID)
                        GamePlayStatsActivity.start(ctx, gameId, gameName, iconColor)
                }
                playStatsContainer?.fadeIn()
            }

        } else {
            playCountContainer?.fadeOut()
            lastPlayContainer?.fadeOut()
            playStatsContainer?.fadeOut()
        }
    }

    private fun colorize() {
        if (isAdded) {
            arrayOf(playsIcon, playStatsIcon, colorsIcon).forEach { it.setOrClearColorFilter(iconColor) }
        }
    }
}
