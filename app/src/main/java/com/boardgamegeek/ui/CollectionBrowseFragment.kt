package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
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
            CollectionShelf.CollectionItemAdapter(
                R.menu.collection_shelf,
                onClick()
            ) { rating(it.averageRating) }
        )
        viewModel.recentlyViewedItems.observe(viewLifecycleOwner) {
            binding.recentlyViewedWidget.bindList(it)
        }

        binding.friendlessFavoriteWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                R.menu.collection_shelf,
                onClick()
            ) { rating(it.rating) }
        )
        viewModel.friendlessFavoriteItems.observe(viewLifecycleOwner) {
            binding.friendlessFavoriteWidget.bindList(it)
        }

        binding.hiddenGemsWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                R.menu.collection_shelf,
                onClick()
            ) { rating(it.rating) }
        )
        viewModel.underratedItems.observe(viewLifecycleOwner) {
            binding.hiddenGemsWidget.bindList(it)
        }
    }

    private fun onClick() = { item: CollectionItem, menuItem: MenuItem ->
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
            else -> false
        }
    }

    private fun rating(rating: Double): Pair<String, Int> {
        val text = rating.asPersonalRating(context, ResourcesCompat.ID_NULL)
        val color = rating.toColor(BggColors.ratingColors)
        return text to color
    }
}
