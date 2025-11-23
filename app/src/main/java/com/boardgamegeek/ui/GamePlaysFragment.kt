package com.boardgamegeek.ui

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.format.DateUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.boardgamegeek.R
import com.boardgamegeek.entities.GameEntity
import com.boardgamegeek.entities.PlayEntity
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.databinding.FragmentGamePlaysBinding
import com.boardgamegeek.ui.viewmodel.GameViewModel
import com.boardgamegeek.ui.widget.SelfUpdatingView

class GamePlaysFragment : Fragment() {
    private var _binding: FragmentGamePlaysBinding? = null
    private val binding get() = _binding!!
    private var gameId = BggContract.INVALID_ID
    private var gameName = ""
    private var imageUrl = ""
    private var thumbnailUrl = ""
    private var heroImageUrl = ""
    private var arePlayersCustomSorted = false
    @ColorInt
    private var iconColor = Color.TRANSPARENT

    private val viewModel: GameViewModel by lazy {
        ViewModelProvider(this).get(GameViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGamePlaysBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.swipeRefresh.setBggColors()

        binding.syncTimestampView.timestamp = 0L

        viewModel.game.observe(this, Observer {
            onGameQueryComplete(it?.data)
        })

        viewModel.plays.observe(this, Observer {
            binding.swipeRefresh.post { binding.swipeRefresh.isRefreshing = it?.status == Status.REFRESHING }
            onPlaysQueryComplete(it?.data)
            binding.progressView.hide()
        })

        viewModel.playColors.observe(this, Observer {
            updateColors(it)
            binding.progressView.hide()
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateColors(colors: List<String>?) {
        val count = colors?.size ?: 0
        if (colors != null && count > 0 && colors.all { it.isKnownColor() }) {
            binding.colorsList.removeAllViews()
            colors.forEach {
                requireContext().createSmallCircle().apply {
                    setColorViewValue(it.asColorRgb())
                    binding.colorsList.addView(this)
                }
            }
            binding.colorsList.fadeIn()
            binding.colorsLabel.fadeOut()
        } else {
            binding.colorsLabel.text = context?.getQuantityText(R.plurals.colors_suffix, count, count) ?: ""
            binding.colorsLabel.fadeIn()
            binding.colorsList.fadeOut()
        }
        binding.colorsContainer.fadeIn()
        binding.colorsContainer.setOnClickListener {
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
        binding.syncTimestampView.timestamp = game.updatedPlays
        iconColor = game.iconColor
        colorize()
    }

    private fun onPlaysQueryComplete(plays: List<PlayEntity>?) {
        if (plays != null) {
            if (plays.isNotEmpty()) {
                val inProgressPlays = plays.filter { it.dirtyTimestamp > 0 }
                if (inProgressPlays.isNotEmpty()) {
                    binding.inProgressPlaysList.removeAllViews()
                    inProgressPlays.take(3).forEach { play ->
                        val row = LayoutInflater.from(context).inflate(R.layout.row_play_summary_updating, binding.inProgressPlaysList, false)
                        row.findViewById<InProgressPlay>(R.id.line1).play = play
                        row.findViewById<InProgressPlay>(R.id.line1).timeHintUpdateInterval = 1_000L
                        row.findViewById<TextView>(R.id.line2)?.setTextOrHide(play.describe(requireContext()))
                        row.setOnClickListener {
                            PlayActivity.start(context, play.internalId, play.gameId, play.gameName, thumbnailUrl, imageUrl, heroImageUrl)
                        }
                        binding.inProgressPlaysList.addView(row)
                    }
                    binding.inProgressPlaysContainer.fadeIn()
                } else {
                    binding.inProgressPlaysContainer.fadeOut()
                }
            } else {
                binding.inProgressPlaysContainer.fadeOut()
            }

            val playCount = plays.sumBy { it.quantity }
            val description = playCount.asPlayCount(requireContext())
            binding.playCountIcon.text = description.first.toString()
            binding.playCountView.text = requireContext().getQuantityText(R.plurals.play_title_suffix, playCount, playCount)
            binding.playCountDescriptionView.setTextOrHide(description.second)
            binding.playCountBackground.setColorViewValue(description.third)
            binding.playCountContainer.setOnClickListener {
                if (gameId != BggContract.INVALID_ID)
                    GamePlaysActivity.start(requireContext(), gameId, gameName, imageUrl, thumbnailUrl, heroImageUrl, arePlayersCustomSorted, iconColor)
            }
            binding.playCountContainer.fadeIn()

            if (plays.isNotEmpty()) {
                val lastPlay = plays.asSequence().filter { it.dirtyTimestamp == 0L }.maxByOrNull { it.dateInMillis }
                if (lastPlay != null) {
                    binding.lastPlayDateView.text = requireContext().getText(R.string.last_played_prefix, lastPlay.dateForDisplay(requireContext()))
                    binding.lastPlayInfoView.setTextOrHide(lastPlay.describe(requireContext()))
                    binding.lastPlayContainer.setOnClickListener {
                        PlayActivity.start(context, lastPlay.internalId, lastPlay.gameId, lastPlay.gameName, thumbnailUrl, imageUrl, heroImageUrl)
                    }
                    binding.lastPlayContainer.fadeIn()
                } else {
                    binding.lastPlayContainer.fadeOut()
                }

                binding.playStatsContainer.setOnClickListener {
                    if (gameId != BggContract.INVALID_ID)
                        GamePlayStatsActivity.start(context, gameId, gameName, iconColor)
                }
                binding.playStatsContainer.fadeIn()
            } else {
                binding.playStatsContainer.fadeOut()
                binding.lastPlayContainer.fadeOut()
            }
        } else {
            binding.playCountContainer.fadeOut()
            binding.lastPlayContainer.fadeOut()
            binding.playStatsContainer.fadeOut()
        }
    }

    private fun colorize() {
        if (isAdded) {
            arrayOf(binding.inProgressPlaysIcon, binding.playsIcon, binding.playStatsIcon, binding.colorsIcon).forEach { it.setOrClearColorFilter(iconColor) }
        }
    }

    companion object {
        fun newInstance(): GamePlaysFragment {
            return GamePlaysFragment()
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
