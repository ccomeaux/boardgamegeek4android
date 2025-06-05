package com.boardgamegeek.ui

import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder

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

        binding.container.layoutTransition.setAnimateParentHierarchy(false)

        viewModel.collectionAcquireStats.observe(viewLifecycleOwner) {
            if (it.incomingCount > 0) {
                binding.acquireSummaryView.text = getString(R.string.msg_collection_details_acquire, it.incomingCount, it.desireRate.asPercentage())
                binding.acquireSummaryView.isVisible = true
            } else {
                binding.acquireSummaryView.isVisible = false
            }
        }

        viewModel.syncCollectionStatuses.observe(viewLifecycleOwner) {
            it?.let {
                binding.preorderedWidget.isVisible = it.contains(CollectionStatus.Preordered)
                binding.wishlistWidget.isVisible = it.contains(CollectionStatus.Wishlist)
                binding.wantToBuyWidget.isVisible = it.contains(CollectionStatus.WantToBuy)
            }
        }

        binding.preorderedWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                R.menu.collection_shelf_preordered,
                onAcquireMenuClick()
            ) { item: CollectionItem ->
                item.acquisitionDate.formatDateTime(context, flags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_ALL) to Color.WHITE
            }
        )
        viewModel.preordered.observe(viewLifecycleOwner) {
            binding.preorderedWidget.bindList(it.first)
            binding.preorderedWidget.setCount(it.second)
        }

        binding.wishlistWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                R.menu.collection_shelf_wishlist,
                onAcquireMenuClick(),
            ) { item: CollectionItem ->
                item.wishListPriority.asWishListPriority(context) to
                        item.wishListPriority.toDouble().toColor(BggColors.fiveStageColors)
            }
        )
        viewModel.wishlist.observe(viewLifecycleOwner) {
            binding.wishlistWidget.bindList(it.first)
            binding.wishlistWidget.setCount(it.second)
        }

        binding.wantToBuyWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                R.menu.collection_shelf_want_to_buy,
                onAcquireMenuClick(),
            ) { rating(it.averageRating) }
        )
        viewModel.wantToBuy.observe(viewLifecycleOwner) {
            binding.wantToBuyWidget.bindList(it.first)
            binding.wantToBuyWidget.setCount(it.second)
        }

        binding.wantInTradeWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                R.menu.collection_shelf_want_in_trade,
                onAcquireMenuClick(),
            ) { rating(it.averageRating) }
        )
        viewModel.wantInTrade.observe(viewLifecycleOwner) {
            binding.wantInTradeWidget.bindList(it.first)
            binding.wantInTradeWidget.setCount(it.second)
        }

        binding.favoriteUnownedWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                R.menu.collection_shelf_acquire,
                onAcquireMenuClick(),
            ) { rating(it.rating) }
        )
        viewModel.favoriteUnownedItems.observe(viewLifecycleOwner) {
            binding.favoriteUnownedWidget.bindList(it)
        }

        binding.playedUnownedWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                R.menu.collection_shelf_acquire,
                onAcquireMenuClick(),
            ) { requireContext().getQuantityText(R.plurals.plays_suffix, it.numberOfPlays, it.numberOfPlays) to Color.WHITE }
        )
        viewModel.playedButUnownedItems.observe(viewLifecycleOwner) {
            binding.playedUnownedWidget.bindList(it)
        }

        binding.hawtUnownedWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                R.menu.collection_shelf_acquire,
                onAcquireMenuClick(),
            ) { rating(it.averageRating) }
        )
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
                GameCollectionItemActivity.start(requireContext(), item)
                true
            }
            R.id.menu_remove_preordered -> {
                confirmRemoveStatus(item, R.string.collection_status_preordered, CollectionStatus.Preordered)
                true
            }
            R.id.menu_remove_wishlist -> {
                confirmRemoveStatus(item, R.string.collection_status_wishlist, CollectionStatus.Wishlist)
                true
            }
            R.id.menu_remove_want_to_buy -> {
                confirmRemoveStatus(item, R.string.collection_status_want_to_buy, CollectionStatus.WantToBuy)
                true
            }
            R.id.menu_remove_want_in_trade -> {
                confirmRemoveStatus(item, R.string.collection_status_want_in_trade, CollectionStatus.WantInTrade)
                true
            }
            else -> false
        }
    }

    private fun confirmRemoveStatus(item: CollectionItem, @StringRes statusResId: Int, status: CollectionStatus) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(item.collectionName)
            .setMessage(getString(R.string.msg_remove_status, getString(statusResId)))
            .setCancelable(true)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.remove) { _: DialogInterface?, _: Int ->
                viewModel.removeStatus(item.internalId, status)
            }
            .create()
            .show()
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
