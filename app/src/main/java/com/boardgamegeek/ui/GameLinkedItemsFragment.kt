package com.boardgamegeek.ui

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
import com.boardgamegeek.model.GameDetail
import com.boardgamegeek.model.Status
import com.boardgamegeek.extensions.loadIcon
import com.boardgamegeek.extensions.setBggColors
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

        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.swipeRefresh.setBggColors()

        binding.footer.lastModifiedView.timestamp = 0

        viewModel.gameId.observe(viewLifecycleOwner) { gameId ->
            binding.footer.gameIdView.text = gameId.toString()
        }

        viewModel.game.observe(viewLifecycleOwner) {
            binding.swipeRefresh.post { binding.swipeRefresh.isRefreshing = it?.status == Status.REFRESHING }
            when {
                it == null -> showError(getString(R.string.empty_game))
                it.status == Status.ERROR && it.data == null -> showError(it.message)
                it.data == null -> showError(getString(R.string.empty_game))
                else -> {
                    it.data.let { game ->
                        binding.footer.gameIdView.text = game.id.toString()
                        binding.footer.lastModifiedView.timestamp = game.updated
                        binding.emptyMessage.isVisible = false
                        listOf(binding.expansionsHeaderView, binding.baseGamesHeaderView).forEach { tv -> tv.setTextColor(game.iconColor) }
                    }
                }
            }
            binding.progress.hide()
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
                        val gameName = viewModel.game.value?.data?.name.orEmpty()
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

    private fun showError(message: String?) {
        if (message?.isNotBlank() == true) {
            binding.emptyMessage.text = message
            binding.emptyMessage.isVisible = true
        }
    }
}
