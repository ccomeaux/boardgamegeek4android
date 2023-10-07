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
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentGamePlaysBinding
import com.boardgamegeek.entities.Play
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.viewmodel.GameViewModel
import com.boardgamegeek.ui.widget.SelfUpdatingView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GamePlaysFragment : Fragment() {
    private var _binding: FragmentGamePlaysBinding? = null
    private val binding get() = _binding!!
    private var gameId = BggContract.INVALID_ID
    private var gameName = ""
    private var heroImageUrl = ""
    private var arePlayersCustomSorted = false

    @ColorInt
    private var iconColor = Color.TRANSPARENT

    private val viewModel by activityViewModels<GameViewModel>()

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentGamePlaysBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.constraintLayout.layoutTransition.setAnimateParentHierarchy(false)
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.swipeRefresh.setBggColors()

        binding.syncTimestampView.timestamp = 0L

        viewModel.game.observe(viewLifecycleOwner) {
            it?.data?.let { game ->
                gameId = game.id
                gameName = game.name
                heroImageUrl = game.heroImageUrl
                arePlayersCustomSorted = game.customPlayerSort
                binding.syncTimestampView.timestamp = game.updatedPlays
                iconColor = game.iconColor
                listOf(binding.inProgressPlaysIcon, binding.playsIcon, binding.playStatsIcon, binding.colorsIcon).forEach { v ->
                    v.setOrClearColorFilter(iconColor)
                }
            }
        }

        viewModel.plays.observe(viewLifecycleOwner) {
            it?.let { (status, data, message) ->
                binding.swipeRefresh.isRefreshing = status == Status.REFRESHING
                data.orEmpty().run {
                    bindTotalPlays(this)
                    bindPlaysInProgress(this)
                    bindLastPlay(this)
                    bindStats(this)
                }
                if (status == Status.ERROR) toast(message)
            }
        }

        viewModel.playColors.observe(viewLifecycleOwner) {
            it?.let { bindColors(it) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun bindTotalPlays(plays: List<Play>) {
        val playCount = plays.sumOf { it.quantity }
        val (count, description, color) = playCount.asPlayCount(requireContext())
        binding.playCountIcon.text = count.toString()
        binding.playCountView.text = requireContext().getQuantityText(R.plurals.play_title_suffix, playCount, playCount)
        binding.playCountDescriptionView.setTextOrHide(description)
        binding.playCountBackground.setColorViewValue(color)
        binding.playCountContainer.setOnClickListener {
            if (gameId != BggContract.INVALID_ID)
                GamePlaysActivity.start(
                    requireContext(),
                    gameId,
                    gameName,
                    heroImageUrl,
                    arePlayersCustomSorted,
                    iconColor,
                )
        }
    }

    private fun bindPlaysInProgress(plays: List<Play>) {
        val inProgressPlays = plays.filter { it.dirtyTimestamp > 0 }
        if (inProgressPlays.isNotEmpty()) {
            binding.inProgressPlaysList.removeAllViews()
            inProgressPlays.take(3).forEach { play ->
                // we assume the plays are sorted by most recent
                val row = binding.inProgressPlaysList.inflate(R.layout.row_play_summary_updating)
                row.findViewById<InProgressPlay>(R.id.line1)?.let {
                    it.play = play
                    it.timeHintUpdateInterval = 1_000L
                }
                row.findViewById<TextView>(R.id.line2)?.setTextOrHide(play.describe(requireContext()))
                row.setOnClickListener {
                    PlayActivity.start(requireContext(), play.internalId)
                }
                binding.inProgressPlaysList.addView(row)
            }
            binding.inProgressPlaysViews.isVisible = true
        } else {
            binding.inProgressPlaysViews.isVisible = false
        }
    }

    private fun bindLastPlay(plays: List<Play>) {
        val lastPlay = plays.filter { it.dirtyTimestamp == 0L }.maxByOrNull { it.dateInMillis }
        if (lastPlay != null) {
            binding.lastPlayViews.isVisible = true
            binding.lastPlayDateView.text = requireContext().getText(R.string.last_played_prefix, lastPlay.dateForDisplay(requireContext()))
            binding.lastPlayInfoView.setTextOrHide(lastPlay.describe(requireContext()))
            binding.lastPlayContainer.setOnClickListener {
                PlayActivity.start(requireContext(), lastPlay.internalId)
            }
        } else {
            binding.lastPlayViews.isVisible = false
        }
    }

    private fun bindStats(plays: List<Play>) {
        binding.playStatsViews.isVisible = plays.isNotEmpty()
        binding.playStatsContainer.setOnClickListener {
            if (gameId != BggContract.INVALID_ID)
                GamePlayStatsActivity.start(requireContext(), gameId, gameName, iconColor)
        }
    }

    private fun bindColors(colors: List<String>) {
        binding.colorsLabel.text = requireContext().getQuantityText(R.plurals.colors_suffix, colors.size, colors.size)
        binding.colorsList.removeAllViews()
        if (colors.isNotEmpty() && colors.all { it.isKnownColor() }) {
            colors.forEach {
                requireContext().createSmallCircle().apply {
                    setColorViewValue(it.asColorRgb())
                    binding.colorsList.addView(this)
                }
            }
            binding.colorsList.isVisible = true
        } else {
            binding.colorsList.isVisible = false
        }
        binding.colorsContainer.setOnClickListener {
            if (gameId != BggContract.INVALID_ID)
                GameColorsActivity.start(requireContext(), gameId, gameName, iconColor)
        }
    }

    class InProgressPlay @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = android.R.attr.textViewStyle,
    ) : SelfUpdatingView(context, attrs, defStyleAttr) {
        var play: Play? = null

        override fun updateText() {
            play?.let {
                text = when {
                    it.startTime > 0 -> context.getText(
                        R.string.playing_for_prefix,
                        DateUtils.formatElapsedTime((System.currentTimeMillis() - it.startTime) / 1000)
                    )
                    it.dateInMillis.isToday() -> context.getText(
                        R.string.playing_prefix,
                        it.dateForDisplay(context)
                    )
                    else -> context.getText(R.string.playing_since_prefix, it.dateForDisplay(context))
                }
            }
        }
    }
}
