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
import com.boardgamegeek.entities.GameDetail
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.loadIcon
import com.boardgamegeek.extensions.setBggColors
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
                        listOf(binding.designerHeaderView, binding.artistsHeaderView, binding.publishersHeaderView, binding.categoriesHeaderView, binding.mechanicsHeaderView)
                            .forEach { tv -> tv.setTextColor(game.iconColor) }
                    }
                }
            }
            binding.progress.hide()
        }
        viewModel.designers.observe(viewLifecycleOwner) { entities ->
            entities?.let { list ->
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
        viewModel.artists.observe(viewLifecycleOwner) { entities ->
            entities?.let { list ->
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
        viewModel.publishers.observe(viewLifecycleOwner) { entities ->
            entities?.let { list ->
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
        viewModel.categories.observe(viewLifecycleOwner) { entities ->
            entities?.let { list ->
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
        viewModel.mechanics.observe(viewLifecycleOwner) { entities ->
            entities?.let { list ->
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
                        val gameName = viewModel.game.value?.data?.name.orEmpty()
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
            setOnClickListener {
                when (type) {
                    GameViewModel.ProducerType.ARTIST -> PersonActivity.startForArtist(context, producer.id, producer.name)
                    GameViewModel.ProducerType.DESIGNER -> PersonActivity.startForDesigner(context, producer.id, producer.name)
                    GameViewModel.ProducerType.PUBLISHER -> PersonActivity.startForPublisher(context, producer.id, producer.name)
                    else -> {}
                }
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
