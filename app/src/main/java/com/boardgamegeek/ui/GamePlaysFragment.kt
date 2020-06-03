package com.boardgamegeek.ui

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.format.DateUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.boardgamegeek.R
import com.boardgamegeek.entities.GameEntity
import com.boardgamegeek.entities.PlayEntity
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.viewmodel.GameViewModel
import com.boardgamegeek.ui.widget.SelfUpdatingView
import kotlinx.android.synthetic.main.fragment_game_plays.*

class GamePlaysFragment : Fragment(R.layout.fragment_game_plays) {
    private var gameId = BggContract.INVALID_ID
    private var gameName = ""
    private var imageUrl = ""
    private var thumbnailUrl = ""
    private var heroImageUrl = ""
    private var arePlayersCustomSorted = false

    @ColorInt
    private var iconColor = Color.TRANSPARENT

    private val viewModel by activityViewModels<GameViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefresh?.setOnRefreshListener { viewModel.refresh() }
        swipeRefresh?.setBggColors()

        syncTimestampView?.timestamp = 0L

        viewModel.game.observe(viewLifecycleOwner, Observer {
            onGameQueryComplete(it?.data)
        })

        viewModel.plays.observe(viewLifecycleOwner, Observer {
            swipeRefresh?.post { swipeRefresh?.isRefreshing = it?.status == Status.REFRESHING }
            onPlaysQueryComplete(it?.data)
            progressView.hide()
        })

        viewModel.playColors.observe(viewLifecycleOwner, Observer {
            updateColors(it)
            progressView.hide()
        })
    }

    private fun updateColors(colors: List<String>?) {
        val count = colors?.size ?: 0
        if (colors != null && count > 0 && colors.all { it.isKnownColor() }) {
            colorsList.removeAllViews()
            colors.forEach {
                requireContext().createSmallCircle().apply {
                    setColorViewValue(it.asColorRgb())
                    colorsList.addView(this)
                }
            }
            colorsList?.fadeIn()
            colorsLabel?.fadeOut()
        } else {
            colorsLabel?.text = context?.getQuantityText(R.plurals.colors_suffix, count, count) ?: ""
            colorsLabel?.fadeIn()
            colorsList?.fadeOut()
        }
        colorsContainer?.fadeIn()
        colorsContainer.setOnClickListener {
            if (gameId != BggContract.INVALID_ID)
                GameColorsActivity.start(requireContext(), gameId, gameName, iconColor)
        }
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
                    inProgressPlaysList.removeAllViews()
                    inProgressPlays.take(3).forEach { play ->
                        val row = LayoutInflater.from(context).inflate(R.layout.row_play_summary_updating, inProgressPlaysList, false)
                        row.findViewById<InProgressPlay>(R.id.line1).play = play
                        row.findViewById<InProgressPlay>(R.id.line1).timeHintUpdateInterval = 1_000L
                        row.findViewById<TextView>(R.id.line2)?.setTextOrHide(play.describe(requireContext()))
                        row.setOnClickListener {
                            PlayActivity.start(context, play.internalId, play.gameId, play.gameName, thumbnailUrl, imageUrl, heroImageUrl)
                        }
                        inProgressPlaysList?.addView(row)
                    }
                    inProgressPlaysContainer.fadeIn()
                } else {
                    inProgressPlaysContainer.fadeOut()
                }
            } else {
                inProgressPlaysContainer.fadeOut()
            }

            val playCount = plays.sumBy { it.quantity }
            val description = playCount.asPlayCount(requireContext())
            playCountIcon.text = description.first.toString()
            playCountView.text = requireContext().getQuantityText(R.plurals.play_title_suffix, playCount, playCount)
            playCountDescriptionView.setTextOrHide(description.second)
            playCountBackground.setColorViewValue(description.third)
            playCountContainer.setOnClickListener {
                if (gameId != BggContract.INVALID_ID)
                    GamePlaysActivity.start(requireContext(), gameId, gameName, imageUrl, thumbnailUrl, heroImageUrl, arePlayersCustomSorted, iconColor)
            }
            playCountContainer.fadeIn()

            if (plays.isNotEmpty()) {
                val lastPlay = plays.asSequence().filter { it.dirtyTimestamp == 0L }.maxBy { it.dateInMillis }
                if (lastPlay != null) {
                    lastPlayDateView.text = requireContext().getText(R.string.last_played_prefix, lastPlay.dateForDisplay(requireContext()))
                    lastPlayInfoView.setTextOrHide(lastPlay.describe(requireContext()))
                    lastPlayContainer.setOnClickListener {
                        PlayActivity.start(context, lastPlay.internalId, lastPlay.gameId, lastPlay.gameName, thumbnailUrl, imageUrl, heroImageUrl)
                    }
                    lastPlayContainer.fadeIn()
                } else {
                    lastPlayContainer.fadeOut()
                }

                playStatsContainer.setOnClickListener {
                    if (gameId != BggContract.INVALID_ID)
                        GamePlayStatsActivity.start(requireContext(), gameId, gameName, iconColor)
                }
                playStatsContainer.fadeIn()
            } else {
                playStatsContainer.fadeOut()
                lastPlayContainer.fadeOut()
            }
        } else {
            playCountContainer.fadeOut()
            lastPlayContainer.fadeOut()
            playStatsContainer.fadeOut()
        }
    }

    private fun colorize() {
        if (isAdded) {
            arrayOf(inProgressPlaysIcon, playsIcon, playStatsIcon, colorsIcon).forEach { it.setOrClearColorFilter(iconColor) }
        }
    }

    class InProgressPlay @JvmOverloads constructor(
            context: Context,
            attrs: AttributeSet? = null,
            defStyleAttr: Int = android.R.attr.textViewStyle
    ) : SelfUpdatingView(context, attrs, defStyleAttr) {
        var play: PlayEntity? = null

        override fun updateText() {
            play?.let {
                text = when {
                    it.startTime > 0 -> context.getText(R.string.playing_for_prefix, DateUtils.formatElapsedTime((System.currentTimeMillis() - it.startTime) / 1000))
                    DateUtils.isToday(it.dateInMillis) -> context.getText(R.string.playing_prefix, it.dateForDisplay(context))
                    else -> context.getText(R.string.playing_since_prefix, it.dateForDisplay(context))
                }
            }
        }
    }
}
