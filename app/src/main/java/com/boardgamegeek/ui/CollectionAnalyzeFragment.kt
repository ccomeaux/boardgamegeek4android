package com.boardgamegeek.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentCollectionAnalyzeBinding
import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.ui.dialog.CollectionDetailsCommentDialogFragment
import com.boardgamegeek.ui.dialog.CollectionDetailsRatingNumberPadDialogFragment
import com.boardgamegeek.ui.viewmodel.CollectionDetailsViewModel
import com.boardgamegeek.ui.widget.CollectionShelf
import java.text.DecimalFormat

class CollectionAnalyzeFragment : Fragment() {
    private var _binding: FragmentCollectionAnalyzeBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<CollectionDetailsViewModel>()

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentCollectionAnalyzeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.container.layoutTransition.setAnimateParentHierarchy(false)

        val displayFormat = DecimalFormat("0.00")
        viewModel.collectionAnalyzeStats.observe(viewLifecycleOwner) {
            binding.analyzeSummaryView.text = getString(
                R.string.collection_analyze_summary,
                it.averagePersonalRating.asPersonalRating(requireContext()),
                it.averageAverageRating.asBoundedRating(requireContext(), displayFormat),
                it.correlationCoefficient.asScore(requireContext(), format = displayFormat)
            )
        }

        binding.gamesToRateWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                R.menu.collection_shelf_rate,
                onAnalyzeMenuClick(),
            ) { requireContext().getQuantityText(R.plurals.plays_suffix, it.numberOfPlays, it.numberOfPlays) to Color.WHITE }
        )
        viewModel.ratableItems.observe(viewLifecycleOwner) {
            binding.gamesToRateWidget.bindList(it.first)
            binding.gamesToRateWidget.setCount(it.second)
        }

        binding.gamesToCommentWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                R.menu.collection_shelf_comment,
                onAnalyzeMenuClick(),
            ) { rating(it.rating) }
        )
        viewModel.commentableItems.observe(viewLifecycleOwner) {
            binding.gamesToCommentWidget.bindList(it.first)
            binding.gamesToCommentWidget.setCount(it.second)
        }
    }

    private fun rate(item: CollectionItem) {
        val fragment = CollectionDetailsRatingNumberPadDialogFragment.newInstance(item.internalId, item.gameName)
        (this.requireActivity() as? FragmentActivity)?.showAndSurvive(fragment)
    }

    private fun comment(item: CollectionItem) {
        CollectionDetailsCommentDialogFragment.show(parentFragmentManager, R.string.comment, item.gameName, item.internalId, item.comment)
    }

    private fun onAnalyzeMenuClick() = { item: CollectionItem, menuItem: MenuItem ->
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
            R.id.menu_rate_item -> {
                rate(item)
                true
            }
            R.id.menu_comment_item -> {
                comment(item)
                true
            }
            else -> false
        }
    }

    private fun rating(rating: Double): Pair<String, Int> {
        val text = rating.asPersonalRating(context, ResourcesCompat.ID_NULL)
        val color = rating.toColor(BggColors.ratingColors)
        return text to color
    }
}
