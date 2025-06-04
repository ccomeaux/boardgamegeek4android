package com.boardgamegeek.ui

import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentCollectionPlayBinding
import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.CollectionStatus
import com.boardgamegeek.ui.viewmodel.CollectionDetailsViewModel
import com.boardgamegeek.ui.widget.CollectionShelf
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class CollectionPlayFragment : Fragment() {
    private var _binding: FragmentCollectionPlayBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<CollectionDetailsViewModel>()

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentCollectionPlayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.container.layoutTransition.setAnimateParentHierarchy(false)

        binding.allButton.setOnClickListener {
            viewModel.filterPlayerCountType(CollectionDetailsViewModel.PlayerCountType.All)
            binding.playerCountChipGroup.isVisible = false
        }
        binding.supportsButton.setOnClickListener {
            viewModel.filterPlayerCountType(CollectionDetailsViewModel.PlayerCountType.Supports)
            binding.playerCountChipGroup.isVisible = true
        }
        binding.goodButton.setOnClickListener {
            viewModel.filterPlayerCountType(CollectionDetailsViewModel.PlayerCountType.GoodWith)
            binding.playerCountChipGroup.isVisible = true
        }
        binding.bestButton.setOnClickListener {
            viewModel.filterPlayerCountType(CollectionDetailsViewModel.PlayerCountType.BestWith)
            binding.playerCountChipGroup.isVisible = true
        }

        binding.playerCountChipGroup.children.filterIsInstance<Chip>().forEach {
            it.setOnClickListener { view ->
                filterByPlayerCount(view as CompoundButton)
            }
        }

        viewModel.playerCountType.observe(viewLifecycleOwner) {
            binding.allButton.isSelected = (it == CollectionDetailsViewModel.PlayerCountType.All)
        }

        viewModel.syncCollectionStatuses.observe(viewLifecycleOwner) {
            it?.let {
                binding.wantToPlayWidget.isVisible = it.contains(CollectionStatus.WantToPlay)
            }
        }

        binding.friendlessShouldPlayWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                null,
                { item ->
                    rating(item.rating)
                },
                R.menu.collection_shelf_play,
                onMenuClick(),
            )
        )
        viewModel.friendlessShouldPlayGames.observe(viewLifecycleOwner) {
            binding.friendlessShouldPlayWidget.bindList(it.first)
            binding.friendlessShouldPlayWidget.setCount(it.second)
        }

        binding.wantToPlayWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                null,
                { item ->
                    rating(item.averageRating)
                },
                R.menu.collection_shelf_want_to_play,
                onMenuClick(),
            )
        )
        viewModel.wantToPlayItems.observe(viewLifecycleOwner) {
            binding.wantToPlayWidget.bindList(it.first)
            binding.wantToPlayWidget.setCount(it.second)
        }

        binding.recentlyPlayedWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                null,
                { item ->
                    rating(item.averageRating)
                },
                R.menu.collection_shelf_play,
                onMenuClick(),
            )
        )
        viewModel.recentlyPlayedGames.observe(viewLifecycleOwner) {
            binding.recentlyPlayedWidget.bindList(it.first)
            binding.recentlyPlayedWidget.setCount(it.second)
        }

        binding.shelfOfOpportunityWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                null,
                { item ->
                    rating(item.averageRating)
                },
                R.menu.collection_shelf_play,
                onMenuClick(),
            )
        )
        viewModel.shelfOfOpportunityItems.observe(viewLifecycleOwner) {
            binding.shelfOfOpportunityWidget.bindList(it.first)
            binding.shelfOfOpportunityWidget.setCount(it.second)
        }


        binding.shelfOfNewOpportunityWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                null,
                { item ->
                    item.acquisitionDate.formatDateTime(context, flags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_ALL) to Color.WHITE
                },
                R.menu.collection_shelf_play,
                onMenuClick(),
            )
        )
        viewModel.shelfOfNewOpportunityItems.observe(viewLifecycleOwner) {
            binding.shelfOfNewOpportunityWidget.bindList(it.first)
            binding.shelfOfNewOpportunityWidget.setCount(it.second)
        }
    }

    private fun onMenuClick() = { item: CollectionItem, menuItem: MenuItem ->
        Boolean
        when (menuItem.itemId) {
            R.id.menu_view_game -> {
                GameActivity.start(requireContext(), item.gameId, item.gameName, item.thumbnailUrl, item.heroImageUrl)
                true
            }
            R.id.menu_view_item -> {
                GameCollectionItemActivity.start(requireContext(), item)
                true
            }
            R.id.menu_play_game -> {
                playGame(item)
                true
            }
            R.id.menu_remove_want_to_play -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(item.collectionName)
                    .setMessage(getString(R.string.msg_remove_status, getString(R.string.collection_status_want_to_play)))
                    .setCancelable(true)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.remove) { _: DialogInterface?, _: Int ->
                        viewModel.removeStatus(item.internalId, CollectionStatus.WantToPlay)
                    }
                    .create()
                    .show()
                true
            }
            else -> false
        }
    }

    private fun filterByPlayerCount(buttonView: CompoundButton) {
        val playerCount = when (buttonView) {
            binding.filter1pButton -> 1
            binding.filter2pButton -> 2
            binding.filter3pButton -> 3
            binding.filter4pButton -> 4
            binding.filter5pButton -> 5
            binding.filter6pButton -> 6
            else -> null
        }
        viewModel.filterPlayerCount(playerCount)
    }

    private fun playGame(item: CollectionItem) {
        when (requireContext().preferences().logPlayPreference()) {
            LOG_PLAY_TYPE_FORM -> LogPlayActivity.logPlay(
                requireContext(),
                item.gameId,
                item.gameName,
                item.robustHeroImageUrl,
                item.arePlayersCustomSorted
            )
            LOG_PLAY_TYPE_QUICK -> {
                requireActivity().createThemedBuilder()
                    .setMessage(getString(R.string.are_you_sure_log_quick_play, item.gameName))
                    .setPositiveButton(R.string.title_log_play) { _, _ ->
                        viewModel.logQuickPlay(item.gameId, item.gameName)
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .setCancelable(true)
                    .show()

            }
            LOG_PLAY_TYPE_WIZARD -> NewPlayActivity.start(requireContext(), item.gameId, item.gameName)
        }
    }

    private fun rating(rating: Double): Pair<String, Int> {
        val text = rating.asPersonalRating(context, ResourcesCompat.ID_NULL)
        val color = rating.toColor(BggColors.ratingColors)
        return text to color
    }
}
