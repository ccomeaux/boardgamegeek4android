package com.boardgamegeek.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentGameLinkedItemsBinding
import com.boardgamegeek.extensions.loadIcon
import com.boardgamegeek.extensions.setBggColors
import com.boardgamegeek.model.GameDetail
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.viewmodel.GameViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GameLinkedItemsFragment : Fragment() {
    private var _binding: FragmentGameLinkedItemsBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<GameViewModel>()

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentGameLinkedItemsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.swipeRefresh.setOnRefreshListener { viewModel.refreshGame() }
        binding.swipeRefresh.setBggColors()

        binding.footer.lastModifiedView.timestamp = 0

        viewModel.gameIsRefreshing.observe(viewLifecycleOwner) {
            it?.let { binding.swipeRefresh.isRefreshing }
        }
        viewModel.game.observe(viewLifecycleOwner) { game ->
            if (game == null) {
                binding.emptyMessage.isVisible = true
                binding.footer.gameIdView.isVisible = false
                binding.footer.lastModifiedView.isVisible = false
            } else {
                binding.footer.gameIdView.text = game.id.toString()
                binding.footer.lastModifiedView.timestamp = game.updated
                binding.footer.gameIdView.isVisible = true
                binding.footer.lastModifiedView.isVisible = true
                binding.emptyMessage.isVisible = false
                if (game.iconColor != Color.TRANSPARENT) {
                    listOf(
                        binding.expansionsHeaderView,
                        binding.baseGamesHeaderView
                    ).forEach { tv -> tv.setTextColor(game.iconColor) }
                }
            }
            binding.contentLoadingProgressBar.hide()
        }
        viewModel.expansions.observe(viewLifecycleOwner) {
            it?.let { list ->
                binding.expansionsHeaderView.isGone = list.isEmpty()
                binding.expansionsChipGroup.bindData(
                    list,
                    R.drawable.ic_baseline_flip_to_back_24,
                    R.string.expansions,
                    GameViewModel.ProducerType.EXPANSION,
                )
                binding.expansionsDividerView.isGone = list.isEmpty()
            }
        }
        viewModel.baseGames.observe(viewLifecycleOwner) {
            it?.let { list ->
                binding.baseGamesHeaderView.isGone = list.isEmpty()
                binding.baseGamesChipGroup.bindData(
                    list,
                    R.drawable.ic_baseline_flip_to_front_24,
                    R.string.base_games,
                    GameViewModel.ProducerType.BASE_GAME,
                )
                binding.baseGamesDividerView.isGone = list.isEmpty()
            }
        }
    }

    private fun ChipGroup.bindData(
        list: List<GameDetail>,
        @DrawableRes iconResId: Int,
        @StringRes labelResId: Int,
        type: GameViewModel.ProducerType,
        limit: Int = 4,
    ) {
        if (list.isEmpty()) {
            visibility = View.GONE
        } else {
            removeAllViews()
            if (list.size <= limit) {
                list.forEach { producer ->
                    addView(createChip(producer))
                }
            } else {
                list.take(limit - 1).forEach { producer ->
                    addView(createChip(producer))
                }
                val moreChip = Chip(context, null, R.style.Widget_MaterialComponents_Chip_Entry).apply {
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    if (iconResId != 0) setChipIconResource(iconResId)
                    text = context.getString(R.string.more_suffix, list.size - limit + 1)
                    setOnClickListener {
                        val gameId = viewModel.gameId.value ?: BggContract.INVALID_ID
                        val gameName = viewModel.game.value?.name.orEmpty()
                        GameDetailActivity.start(context, getString(labelResId), gameId, gameName, type)
                    }
                }
                addView(moreChip)
            }
            visibility = View.VISIBLE
        }
    }

    private fun createChip(producer: GameDetail): Chip {
        return Chip(context, null, R.style.Widget_MaterialComponents_Chip_Entry).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            text = producer.name
            if (producer.thumbnailUrl.isNotBlank())
                loadIcon(producer.thumbnailUrl)
            setOnClickListener {
                GameActivity.start(context, producer.id, producer.name)
            }
        }
    }
}
