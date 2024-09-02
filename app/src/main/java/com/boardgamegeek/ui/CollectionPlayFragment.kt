package com.boardgamegeek.ui

import android.graphics.Color
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
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

        viewModel.syncCollectionStatuses.observe(viewLifecycleOwner) {
            it?.let {
                binding.wantToPlayWidget.isVisible = it.contains(CollectionStatus.WantToPlay)
            }
        }

        binding.wantToPlayWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                { item: CollectionItem ->
                    playGame(item)
                }
            ) { item ->
                rating(item.averageRating)
            }
        )
        viewModel.wantToPlayItems.observe(viewLifecycleOwner) {
            binding.wantToPlayWidget.bindList(it)
        }

        binding.recentlyPlayedWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                { item: CollectionItem ->
                    playGame(item)
                }
            ) { item ->
                rating(item.averageRating)
            }
        )
        viewModel.recentlyPlayedItems.observe(viewLifecycleOwner) {
            binding.recentlyPlayedWidget.bindList(it)
        }

        binding.shelfOfOpportunityWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                { item: CollectionItem ->
                    playGame(item)
                }
            ) { item ->
                rating(item.averageRating)
            }
        )
        viewModel.shelfOfOpportunityItems.observe(viewLifecycleOwner) {
            binding.shelfOfOpportunityWidget.bindList(it)
        }


        binding.shelfOfNewOpportunityWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                { item: CollectionItem ->
                    playGame(item)
                }
            ) { item ->
                item.acquisitionDate.formatDateTime(context, flags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_ALL) to Color.WHITE
            }
        )
        viewModel.shelfOfNewOpportunityItems.observe(viewLifecycleOwner) {
            binding.shelfOfNewOpportunityWidget.bindList(it)
        }
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
