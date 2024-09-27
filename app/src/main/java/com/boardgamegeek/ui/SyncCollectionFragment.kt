package com.boardgamegeek.ui

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.allViews
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentSyncCollectionBinding
import com.boardgamegeek.extensions.formatDateTime
import com.boardgamegeek.extensions.getQuantityText
import com.boardgamegeek.extensions.setTextOrHide
import com.boardgamegeek.model.CollectionStatus
import com.boardgamegeek.ui.viewmodel.SyncViewModel
import com.boardgamegeek.ui.widget.CollectionStatusSync
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SyncCollectionFragment : Fragment() {
    private var _binding: FragmentSyncCollectionBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<SyncViewModel>()

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentSyncCollectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ownedStatusView.tag = CollectionStatus.Own
        binding.previouslyOwnedStatusView.tag = CollectionStatus.PreviouslyOwned
        binding.forTradeStatusView.tag = CollectionStatus.ForTrade
        binding.wantInTradeStatusView.tag = CollectionStatus.WantInTrade
        binding.wantToBuyStatusView.tag = CollectionStatus.WantToBuy
        binding.wishlistStatusView.tag = CollectionStatus.Wishlist
        binding.wantToPlayStatusView.tag = CollectionStatus.WantToPlay
        binding.preorderedStatusView.tag = CollectionStatus.Preordered
        binding.playedStatusView.tag = CollectionStatus.Played
        binding.ratedStatusView.tag = CollectionStatus.Rated
        binding.commentedStatusView.tag = CollectionStatus.Commented
        binding.hasPartsStatusView.tag = CollectionStatus.HasParts
        binding.wantPartsView.tag = CollectionStatus.WantParts

        binding.syncCollectionButton.setOnClickListener {
            viewModel.syncCollection()
        }
        binding.cancelCollectionButton.setOnClickListener {
            viewModel.cancelCollection()
        }
        binding.uploadCollectionButton.setOnClickListener {
            viewModel.uploadCollection()
        }
        viewModel.collectionCompleteCurrentTimestamp.observe(viewLifecycleOwner) {
            it?.let {
                binding.collectionCompleteCurrentSyncStatusContainer.isVisible = it > 0L
                binding.collectionCompleteCurrentSyncStatusView.text = it.asDateTime()
            }
        }
        viewModel.collectionCompleteTimestamp.observe(viewLifecycleOwner) {
            it?.let {
                binding.collectionCompleteContainer.isVisible = true
                binding.collectionCompleteSyncStatusView.text = it.asDateTime()
            }
        }
        viewModel.collectionPartialTimestamp.observe(viewLifecycleOwner) {
            it?.let {
                binding.collectionPartialContainer.isVisible = true
                binding.collectionPartialSyncStatusView.text = it.asDateTime()
            }
        }

        viewModel.numberOfUnsyncedGames.observe(viewLifecycleOwner) {
            binding.syncGameCount.text = requireContext().getQuantityText(R.plurals.games_pending_download, it, it)
        }

        viewModel.numberOfCollectionItemsToUpload.observe(viewLifecycleOwner) {
            binding.syncCollectionUpdate.text = requireContext().getQuantityText(R.plurals.items_pending_upload, it, it)
            binding.uploadCollectionButton.isEnabled = it > 0
        }

        binding.root.allViews.filterIsInstance<CollectionStatusSync>().forEach { v ->
            (v.tag as? CollectionStatus)?.let { status ->
                viewModel.collectionStatusCompleteTimestamp(status).observe(viewLifecycleOwner) {
                    v.setDefaultTimestamp(it)
                }
                viewModel.collectionStatusAccessoryCompleteTimestamp(status).observe(viewLifecycleOwner) {
                    v.setAccessoryTimestamp(it)
                }
                v.onSyncClick {
                    viewModel.syncCollection(status)
                }
                v.setEnableListener { enabled ->
                    viewModel.modifyCollectionStatus(status, enabled)
                }
            }
        }
        viewModel.syncCollectionStatuses.observe(viewLifecycleOwner) { statuses ->
            statuses?.let {
                binding.root.allViews.filterIsInstance<CollectionStatusSync>().forEach { view ->
                    (view.tag as? CollectionStatus)?.let { status ->
                        view.check(statuses.contains(status))
                    }
                }
            }
        }

        viewModel.collectionSyncProgress.observe(viewLifecycleOwner) {
            it?.let {
                binding.syncCollectionButton.isEnabled = (it.step == SyncViewModel.CollectionSyncProgressStep.NotSyncing)
                binding.cancelCollectionButton.isEnabled = (it.step != SyncViewModel.CollectionSyncProgressStep.NotSyncing)
                binding.progressBar.isVisible = (it.step != SyncViewModel.CollectionSyncProgressStep.NotSyncing)
                binding.syncCollectionStep.isVisible = (it.step != SyncViewModel.CollectionSyncProgressStep.NotSyncing)

                binding.root.allViews.filterIsInstance<CollectionStatusSync>().forEach { v -> v.setProgress(false, it.step != SyncViewModel.CollectionSyncProgressStep.NotSyncing) }
                binding.root.allViews.filterIsInstance<CollectionStatusSync>().find { v ->
                    (v.tag as? CollectionStatus) == it.status
                }?.setProgress(true, it.step != SyncViewModel.CollectionSyncProgressStep.NotSyncing)

                if (it.step == SyncViewModel.CollectionSyncProgressStep.NotSyncing) {
                    binding.syncCollectionStep.isVisible = false
                } else {
                    val subtype = getString(
                        when (it.subtype) {
                            SyncViewModel.CollectionSyncProgressSubtype.None -> R.string.items
                            SyncViewModel.CollectionSyncProgressSubtype.All -> R.string.games_expansions
                            SyncViewModel.CollectionSyncProgressSubtype.Accessory -> R.string.accessories
                        }
                    )
                    val statusDescription = getString(
                        when (it.status) {
                            CollectionStatus.Own -> R.string.collection_status_own
                            CollectionStatus.PreviouslyOwned -> R.string.collection_status_prev_owned
                            CollectionStatus.Preordered -> R.string.collection_status_preordered
                            CollectionStatus.ForTrade -> R.string.collection_status_for_trade
                            CollectionStatus.WantInTrade -> R.string.collection_status_want_in_trade
                            CollectionStatus.WantToBuy -> R.string.collection_status_want_to_buy
                            CollectionStatus.WantToPlay -> R.string.collection_status_want_to_play
                            CollectionStatus.Wishlist -> R.string.collection_status_wishlist
                            CollectionStatus.Played -> R.string.collection_status_played
                            CollectionStatus.Rated -> R.string.collection_status_rated
                            CollectionStatus.Commented -> R.string.collection_status_commented
                            CollectionStatus.HasParts -> R.string.collection_status_has_parts
                            CollectionStatus.WantParts -> R.string.collection_status_want_parts
                            CollectionStatus.Unknown -> R.string.unknown
                        }
                    )
                    val message = when (it.step) {
                        SyncViewModel.CollectionSyncProgressStep.CompleteCollection -> {
                            if (statusDescription.isBlank()) {
                                getString(R.string.sync_complete_collection, subtype)
                            } else {
                                getString(R.string.sync_complete_collection_status, statusDescription, subtype)
                            }
                        }
                        SyncViewModel.CollectionSyncProgressStep.PartialCollection -> getString(R.string.sync_partial_collection, subtype)
                        SyncViewModel.CollectionSyncProgressStep.StaleCollection -> getString(R.string.sync_stale_collection, subtype)
                        SyncViewModel.CollectionSyncProgressStep.DeleteCollection -> getString(R.string.sync_delete_collection, subtype)
                        SyncViewModel.CollectionSyncProgressStep.RemoveGames -> getString(R.string.sync_remove_games)
                        SyncViewModel.CollectionSyncProgressStep.StaleGames -> getString(R.string.sync_stale_games)
                        SyncViewModel.CollectionSyncProgressStep.NewGames -> getString(R.string.sync_new_games)
                        else -> ""
                    }
                    binding.syncCollectionStep.setTextOrHide(message)
                }
            }
        }
    }

    private fun Long.asDateTime() = this.formatDateTime(
        requireContext(),
        flags = DateUtils.FORMAT_ABBREV_ALL or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_YEAR
    )

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}