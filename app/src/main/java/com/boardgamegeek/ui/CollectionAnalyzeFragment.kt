package com.boardgamegeek.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
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
import com.boardgamegeek.ui.dialog.CollectionDetailsRatingNumberPadDialogFragment
import com.boardgamegeek.ui.viewmodel.CollectionDetailsViewModel
import com.boardgamegeek.ui.widget.CollectionShelf

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

        binding.gamesToRateWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                { item ->
                    rate(item)
                },
                { item ->
                    requireContext().getQuantityText(R.plurals.plays_suffix, item.numberOfPlays, item.numberOfPlays) to Color.WHITE
                }
            ))
        viewModel.ratableItems.observe(viewLifecycleOwner) {
            binding.gamesToRateWidget.bindList(it)
        }

        binding.gamesToCommentWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                { item ->
                    rate(item)
                },
                { item ->
                   rating(item.rating)
                }
            ))
        viewModel.commentableItems.observe(viewLifecycleOwner) {
            binding.gamesToCommentWidget.bindList(it)
        }
    }

    private fun rate(item: CollectionItem) {
        val fragment = CollectionDetailsRatingNumberPadDialogFragment.newInstance(item.internalId, item.gameName)
        (this.requireActivity() as? FragmentActivity)?.showAndSurvive(fragment)
    }

    private fun rating(rating: Double): Pair<String, Int> {
        val text = rating.asPersonalRating(context, ResourcesCompat.ID_NULL)
        val color = rating.toColor(BggColors.ratingColors)
        return text to color
    }
}
