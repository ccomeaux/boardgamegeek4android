package com.boardgamegeek.ui

import android.graphics.Color
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentCollectionAcquireBinding
import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.CollectionStatus
import com.boardgamegeek.ui.dialog.CollectionDetailPrivateInfoDialogFragment
import com.boardgamegeek.ui.viewmodel.CollectionDetailsViewModel
import com.boardgamegeek.ui.widget.CollectionShelf

class CollectionAcquireFragment : Fragment() {
    private var _binding: FragmentCollectionAcquireBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<CollectionDetailsViewModel>()

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentCollectionAcquireBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.syncCollectionStatuses.observe(viewLifecycleOwner) {
            it?.let {
                binding.preorderedWidget.isVisible = it.contains(CollectionStatus.Preordered)
                binding.wishlistWidget.isVisible = it.contains(CollectionStatus.Wishlist)
                binding.wantToBuyWidget.isVisible = it.contains(CollectionStatus.WantToBuy)
            }
        }

        binding.preorderedWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                null,
                { item: CollectionItem ->
                    item.acquisitionDate.formatDateTime(context, flags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_ALL) to Color.WHITE
                },
                R.menu.game_acquire,
                onAcquireMenuClick()
            )
        )
        viewModel.preordered.observe(viewLifecycleOwner) {
            binding.preorderedWidget.bindList(it)
        }

        binding.wishlistWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                null,
                { item: CollectionItem ->
                    item.wishListPriority.asWishListPriority(context) to
                            item.wishListPriority.toDouble().toColor(BggColors.fiveStageColors)
                },
                R.menu.game_acquire,
                onAcquireMenuClick(),
            )
        )
        viewModel.wishlist.observe(viewLifecycleOwner) {
            binding.wishlistWidget.bindList(it)
        }

        binding.wantToBuyWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                null,
                { rating(it.averageRating) },
                R.menu.game_acquire,
                onAcquireMenuClick(),
            )
        )
        viewModel.wantToBuy.observe(viewLifecycleOwner) {
            binding.wantToBuyWidget.bindList(it)
        }

        binding.wantInTradeWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                null,
                { rating(it.averageRating) },
                R.menu.game_acquire,
                onAcquireMenuClick(),
            )
        )
        viewModel.wantInTrade.observe(viewLifecycleOwner) {
            binding.wantInTradeWidget.bindList(it)
        }

        binding.favoriteUnownedWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                null,
                { item ->
                    rating(item.rating)
                },
                R.menu.game_acquire,
                onAcquireMenuClick(),
            ))
        viewModel.favoriteUnownedItems.observe(viewLifecycleOwner) {
            binding.favoriteUnownedWidget.bindList(it)
        }

        binding.playedUnownedWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                null,
                { item ->
                    requireContext().getQuantityText(R.plurals.plays_suffix, item.numberOfPlays, item.numberOfPlays) to Color.WHITE
                },
                R.menu.game_acquire,
                onAcquireMenuClick(),
            ))
        viewModel.playedButUnownedItems.observe(viewLifecycleOwner) {
            binding.playedUnownedWidget.bindList(it)
        }

        binding.hawtUnownedWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                null,
                { item ->
                    rating(item.averageRating)
                },
                R.menu.game_acquire,
                onAcquireMenuClick(),
            ))
        viewModel.hawtUnownedItems.observe(viewLifecycleOwner) {
            binding.hawtUnownedWidget.bindList(it)
        }
    }

    private fun onAcquireMenuClick() = { item: CollectionItem, menuItem: MenuItem ->
        Boolean
        when (menuItem.itemId) {
            R.id.menu_acquire -> {
                buyGame(item)
                true
            }
            R.id.menu_view_game -> {
                GameActivity.start(requireContext(), item.gameId, item.gameName, item.thumbnailUrl, item.heroImageUrl)
                true
            }
            R.id.menu_view_item -> {
                GameCollectionItemActivity.start(
                    requireContext(),
                    item.internalId,
                    item.gameId,
                    item.gameName,
                    item.collectionId,
                    item.collectionName,
                    item.thumbnailUrl,
                    item.heroImageUrl,
                    item.gameYearPublished,
                    item.collectionYearPublished
                )
                true
            }
            else -> false
        }
    }

    private fun buyGame(item: CollectionItem) {
        val privateInfoDialogFragment = CollectionDetailPrivateInfoDialogFragment.newInstance(
            item.internalId,
            item.collectionName,
            item.pricePaidCurrency,
            item.pricePaid,
            item.quantity,
            item.acquisitionDate,
            item.acquiredFrom,
        )
        this.showAndSurvive(privateInfoDialogFragment)
    }

    private fun rating(rating: Double): Pair<String, Int> {
        val text = rating.asPersonalRating(context, ResourcesCompat.ID_NULL)
        val color = rating.toColor(BggColors.ratingColors)
        return text to color
    }
}
