package com.boardgamegeek.ui

import android.graphics.Color
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.databinding.FragmentCollectionBrowseBinding
import com.boardgamegeek.extensions.BggColors
import com.boardgamegeek.extensions.asPersonalRating
import com.boardgamegeek.extensions.toColor
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.ui.viewmodel.CollectionDetailsViewModel
import com.boardgamegeek.ui.widget.CollectionShelf

class CollectionBrowseFragment : Fragment() {
    private var _binding: FragmentCollectionBrowseBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<CollectionDetailsViewModel>()

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentCollectionBrowseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.container.layoutTransition.setAnimateParentHierarchy(false)

        binding.recentlyViewedWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(bindBadge = { item: CollectionItem ->
                rating(item.averageRating)
            }),
        )
        viewModel.recentlyViewedItems.observe(viewLifecycleOwner) {
            binding.recentlyViewedWidget.bindList(it)
        }

        binding.highlyRatedWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                bindBadge = { item ->
                    rating(item.rating)
                })
        )
        viewModel.highlyRatedItems.observe(viewLifecycleOwner) {
            binding.highlyRatedWidget.bindList(it)
        }

        binding.friendlessFavoriteWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                bindBadge = { item ->
                    rating(item.rating)
                })
        )
        viewModel.friendlessFavoriteItems.observe(viewLifecycleOwner) {
            binding.friendlessFavoriteWidget.bindList(it)
        }

        binding.hiddenGemsWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                bindBadge = { item ->
                    item.zScore.asPersonalRating(context, ResourcesCompat.ID_NULL) to Color.WHITE
                })
        )
        viewModel.underratedItems.observe(viewLifecycleOwner) {
            binding.hiddenGemsWidget.bindList(it)
        }

        binding.hawtWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                bindBadge = { item ->
                    rating(item.averageRating)
                }
            )
        )
        viewModel.hawtItems.observe(viewLifecycleOwner) {
            binding.hawtWidget.bindList(it)
        }

        val dateFormat = DateFormat.getDateFormat(context)
        binding.whyOwnWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                bindBadge = { item ->
                    (item.lastPlayDate?.let { dateFormat.format(it) } ?: "") to Color.WHITE
                }
            )
        )
        viewModel.whyOwnItems.observe(viewLifecycleOwner) {
            binding.whyOwnWidget.bindList(it)
        }
    }

    private fun rating(rating: Double): Pair<String, Int> {
        val text = rating.asPersonalRating(context, ResourcesCompat.ID_NULL)
        val color = rating.toColor(BggColors.ratingColors)
        return text to color
    }
}
