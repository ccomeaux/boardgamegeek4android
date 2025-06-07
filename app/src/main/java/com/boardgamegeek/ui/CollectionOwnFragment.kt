package com.boardgamegeek.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentCollectionOwnBinding
import com.boardgamegeek.extensions.BggColors
import com.boardgamegeek.extensions.asPercentage
import com.boardgamegeek.extensions.asPersonalRating
import com.boardgamegeek.extensions.toColor
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.ui.viewmodel.CollectionDetailsViewModel
import com.boardgamegeek.ui.widget.CollectionShelf

class CollectionOwnFragment : Fragment() {
    private var _binding: FragmentCollectionOwnBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<CollectionDetailsViewModel>()

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentCollectionOwnBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.container.layoutTransition.setAnimateParentHierarchy(false)

        binding.gamesWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                R.menu.collection_shelf,
                onMenuClick = onMenuClick(),
            ) { rating(it.averageRating) }
        )
        viewModel.own.observe(viewLifecycleOwner) {
            binding.gamesWidget.bindList(it.first)
            binding.gamesWidget.setCount(it.second)
        }

        viewModel.growthRate.observe(viewLifecycleOwner) {
            binding.growthRateView.text = getString(R.string.msg_growth_rate, it)
        }
        viewModel.utilization.observe(viewLifecycleOwner) {
            binding.utilizationView.text = getString(R.string.msg_utilization, it.asPercentage())
        }

        binding.expansionsWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                R.menu.collection_shelf,
                onMenuClick = onMenuClick(),
            ) { rating(it.averageRating) }
        )
        viewModel.expansions.observe(viewLifecycleOwner) {
            binding.expansionsWidget.bindList(it.first)
            binding.expansionsWidget.setCount(it.second)
        }

        binding.accessoriesWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                R.menu.collection_shelf,
                onMenuClick = onMenuClick(),
            ) { rating(it.averageRating) }
        )
        viewModel.accessories.observe(viewLifecycleOwner) {
            binding.accessoriesWidget.bindList(it.first)
            binding.accessoriesWidget.setCount(it.second)
        }

        binding.recentlyAcquiredWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                R.menu.collection_shelf,
                onMenuClick = onMenuClick(),
            ) { it.acquiredFrom to Color.WHITE }
        )
        viewModel.recentlyAcquired.observe(viewLifecycleOwner) {
            binding.recentlyAcquiredWidget.bindList(it.first)
            binding.recentlyAcquiredWidget.setCount(it.second)
        }

        binding.hawtWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                R.menu.collection_shelf,
                onMenuClick = onMenuClick(),
            ) { rating(it.averageRating) }
        )
        viewModel.hawtItems.observe(viewLifecycleOwner) {
            binding.hawtWidget.bindList(it)
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
            else -> false
        }
    }

    private fun rating(rating: Double): Pair<String, Int> {
        val text = rating.asPersonalRating(context, ResourcesCompat.ID_NULL)
        val color = rating.toColor(BggColors.ratingColors)
        return text to color
    }
}
