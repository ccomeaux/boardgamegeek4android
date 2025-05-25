package com.boardgamegeek.ui

import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentCollectionDivestBinding
import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.CollectionStatus
import com.boardgamegeek.ui.dialog.CollectionDetailsConditionDialogFragment
import com.boardgamegeek.ui.viewmodel.CollectionDetailsViewModel
import com.boardgamegeek.ui.widget.CollectionShelf
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class CollectionDivestFragment : Fragment() {
    private var _binding: FragmentCollectionDivestBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<CollectionDetailsViewModel>()
    private var ownCount: Int = 0
    private var previouslyOwnedCount: Int? = null
    private var forTradeCount: Int? = null

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentCollectionDivestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.container.layoutTransition.setAnimateParentHierarchy(false)

        binding.forTradeWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                null,
                { item: CollectionItem ->
                    rating(item.geekRating)
                },
                R.menu.collection_shelf_for_trade,
                onMenuClick(),
            )
        )

        binding.forTradeWithoutConditionWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                null,
                { item: CollectionItem ->
                    rating(item.geekRating)
                },
                R.menu.collection_shelf_divest_for_trade_without_condition,
                onMenuClick(),
            )
        )

        binding.previouslyOwnedWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                null,
                { item: CollectionItem ->
                    rating(item.geekRating)
                },
                R.menu.collection_shelf,
                onMenuClick(),
            )
        )
        val dateFormat = DateFormat.getDateFormat(context)
        binding.whyOwnWidget.setAdapter(
            CollectionShelf.CollectionItemAdapter(
                null,
                bindBadge = { item ->
                    (item.lastPlayDate?.let { dateFormat.format(it) } ?: "") to Color.WHITE
                },
                R.menu.collection_shelf_offer_trade,
                onMenuClick(),
            )
        )

        viewModel.own.observe(viewLifecycleOwner) {
            ownCount = it.second
            showMessage()
        }

        viewModel.syncCollectionStatuses.observe(viewLifecycleOwner) { set ->
            set?.let { statuses ->
                if (statuses.contains(CollectionStatus.ForTrade)) {
                    binding.forTradeWidget.isVisible = true
                    viewModel.forTrade.observe(viewLifecycleOwner) {
                        forTradeCount = it.second
                        showMessage()
                        binding.forTradeWidget.bindList(it.first)
                        binding.forTradeWidget.setCount(it.second)
                    }
                    binding.forTradeWithoutConditionWidget.isVisible = true
                    viewModel.forTradeWithoutCondition.observe(viewLifecycleOwner) {
                        binding.forTradeWithoutConditionWidget.bindList(it.first)
                        binding.forTradeWithoutConditionWidget.setCount(it.second)
                    }
                }
                if (statuses.contains(CollectionStatus.PreviouslyOwned)) {
                    binding.previouslyOwnedWidget.isVisible = true
                    viewModel.previouslyOwned.observe(viewLifecycleOwner) {
                        previouslyOwnedCount = it.second
                        showMessage()
                        binding.previouslyOwnedWidget.bindList(it.first)
                        binding.previouslyOwnedWidget.setCount(it.second)
                    }

                }
            }
        }

        viewModel.whyOwnItems.observe(viewLifecycleOwner) {
            binding.whyOwnWidget.bindList(it.first)
            binding.whyOwnWidget.setCount(it.second)
        }
    }

    private fun showMessage() {
        if (ownCount > 0) {
            previouslyOwnedCount?.let { previouslyOwned ->
                forTradeCount?.let { forTrade ->
                    val previouslyOwnedRatio = previouslyOwned.toDouble() / ownCount
                    val forTradeRatio = forTrade.toDouble() / ownCount
                    binding.divestSummaryView.text =
                        getString(R.string.msg_collection_details_divest, previouslyOwnedRatio.asPercentage(), forTradeRatio.asPercentage())
                } ?: {
                    val previouslyOwnedRatio = previouslyOwned.toDouble() / ownCount
                    binding.divestSummaryView.text =
                        getString(R.string.msg_collection_details_divest_previously_owned, previouslyOwnedRatio.asPercentage())
                }
            } ?: forTradeCount?.let { forTrade ->
                val forTradeRatio = forTrade.toDouble() / ownCount
                binding.divestSummaryView.text = getString(R.string.msg_collection_details_divest_for_trade, forTradeRatio.asPercentage())
            }
        }
    }

    private fun onMenuClick() = { item: CollectionItem, menuItem: MenuItem ->
        Boolean
        when (menuItem.itemId) {
            R.id.menu_remove_for_trade -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(item.collectionName)
                    .setMessage(getString(R.string.msg_remove_status, getString(R.string.collection_status_for_trade)))
                    .setCancelable(true)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.remove) { _: DialogInterface?, _: Int ->
                        viewModel.removeStatus(item.internalId, CollectionStatus.ForTrade)
                    }
                    .create()
                    .show()
                true
            }
            R.id.menu_add_condition_text -> {
                CollectionDetailsConditionDialogFragment.show(parentFragmentManager, item.gameName, item.internalId, item.conditionText)
                true
            }
            R.id.menu_offer_trade -> {
                viewModel.addStatus(item.internalId, CollectionStatus.ForTrade)
                true
            }
            R.id.menu_trade -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(item.collectionName)
                    .setMessage(R.string.msg_confirm_trade)
                    .setCancelable(true)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.title_trade) { _: DialogInterface?, _: Int ->
                        viewModel.markAsTraded(item.internalId)
                    }
                    .create()
                    .show()
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
            else -> false
        }
    }

    private fun rating(rating: Double): Pair<String, Int> {
        val text = rating.asPersonalRating(context, ResourcesCompat.ID_NULL)
        val color = rating.toColor(BggColors.ratingColors)
        return text to color
    }
}
