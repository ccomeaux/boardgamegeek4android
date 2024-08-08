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
import com.boardgamegeek.databinding.FragmentGameCreditsBinding
import com.boardgamegeek.extensions.loadIcon
import com.boardgamegeek.extensions.setBggColors
import com.boardgamegeek.extensions.setOrClearOnClickListener
import com.boardgamegeek.model.GameDetail
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.viewmodel.GameViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GameCreditsFragment : Fragment() {
    private var _binding: FragmentGameCreditsBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<GameViewModel>()

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentGameCreditsBinding.inflate(inflater, container, false)
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
                listOf(binding.designerHeaderView, binding.artistsHeaderView, binding.publishersHeaderView, binding.categoriesHeaderView, binding.mechanicsHeaderView)
                    .forEach { tv -> tv.setTextColor(game.iconColor) }
            }
            binding.contentLoadingProgressBar.hide()
        }
        viewModel.designers.observe(viewLifecycleOwner) {
            it?.let { list ->
                binding.designerHeaderView.isGone = list.isEmpty()
                binding.designersChipGroup.bindData(
                    list,
                    R.drawable.ic_baseline_edit_24,
                    R.string.designers,
                    GameViewModel.ProducerType.DESIGNER,
                )
                binding.designersDividerView.isGone = list.isEmpty()
            }
        }
        viewModel.artists.observe(viewLifecycleOwner) {
            it?.let { list ->
                binding.artistsHeaderView.isGone = list.isEmpty()
                binding.artistsChipGroup.bindData(
                    list,
                    R.drawable.ic_baseline_brush_24,
                    R.string.artists,
                    GameViewModel.ProducerType.ARTIST,
                )
                binding.artistsDividerView.isGone = list.isEmpty()
            }
        }
        viewModel.publishers.observe(viewLifecycleOwner) {
            it?.let { list ->
                binding.publishersHeaderView.isGone = list.isEmpty()
                binding.publishersChipGroup.bindData(
                    list,
                    R.drawable.ic_baseline_import_contacts_24,
                    R.string.publishers,
                    GameViewModel.ProducerType.PUBLISHER,
                )
                binding.publishersHeaderView.isGone = list.isEmpty()
            }
        }
        viewModel.categories.observe(viewLifecycleOwner) {
            it?.let { list ->
                binding.categoriesHeaderView.isGone = list.isEmpty()
                binding.categoriesChipGroup.bindData(
                    list,
                    R.drawable.ic_baseline_category_24,
                    R.string.categories,
                    GameViewModel.ProducerType.CATEGORY,
                )
                binding.categoriesDividerView.isGone = list.isEmpty()
            }
        }
        viewModel.mechanics.observe(viewLifecycleOwner) {
            it?.let { list ->
                binding.mechanicsHeaderView.isGone = list.isEmpty()
                binding.mechanicsChipGroup.bindData(
                    list,
                    R.drawable.ic_baseline_settings_24,
                    R.string.mechanics,
                    GameViewModel.ProducerType.MECHANIC,
                )
                binding.mechanicsDividerView.isGone = list.isEmpty()
            }
        }
    }

    private fun ChipGroup.bindData(
        list: List<GameDetail>,
        @DrawableRes iconResId: Int,
        @StringRes labelResId: Int,
        type: GameViewModel.ProducerType,
    ) {
        if (list.isEmpty()) {
            visibility = View.GONE
        } else {
            removeAllViews()
            val limit = 4
            if (list.size <= limit) {
                list.forEach { producer ->
                    addView(createChip(producer, type))
                }
            } else {
                list.take(limit - 1).forEach { producer ->
                    addView(createChip(producer, type))
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

    private fun createChip(producer: GameDetail, type: GameViewModel.ProducerType): Chip {
        return Chip(context, null, R.style.Widget_MaterialComponents_Chip_Entry).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            text = producer.name
            if (producer.thumbnailUrl.isNotBlank())
                loadIcon(producer.thumbnailUrl)
            when (type) {
                GameViewModel.ProducerType.ARTIST -> setOnClickListener { PersonActivity.startForArtist(context, producer.id, producer.name) }
                GameViewModel.ProducerType.DESIGNER -> setOnClickListener { PersonActivity.startForDesigner(context, producer.id, producer.name) }
                GameViewModel.ProducerType.PUBLISHER -> setOnClickListener { PersonActivity.startForPublisher(context, producer.id, producer.name) }
                else -> setOrClearOnClickListener()
            }
        }
    }
}
