package com.boardgamegeek.ui

import android.content.SharedPreferences
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
import com.boardgamegeek.databinding.FragmentSyncPlaysBinding
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.viewmodel.SyncViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SyncPlaysFragment : Fragment() {
    private var _binding: FragmentSyncPlaysBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<SyncViewModel>()

    private val prefs: SharedPreferences by lazy { requireContext().preferences() }

    private var playsUpdateCount = 0
    private var playsDeleteCount = 0

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentSyncPlaysBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.containerView.layoutTransition.setAnimateParentHierarchy(false)

        binding.syncPlaysSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs[PREFERENCES_KEY_SYNC_PLAYS] = isChecked
            binding.buttonBar.isVisible = isChecked
        }
        binding.syncPlaysButton.setOnClickListener {
            viewModel.syncPlays()
        }
        binding.cancelPlaysButton.setOnClickListener {
            viewModel.cancelPlays()
        }
        binding.uploadPlaysButton.setOnClickListener {
            viewModel.uploadPlays()
        }

        viewModel.syncPlays.observe(viewLifecycleOwner) {
            it?.let {
                binding.syncPlaysSwitch.isChecked = it
                binding.playSyncStatusView.isVisible = it
            }
        }
        viewModel.playSyncState.observe(viewLifecycleOwner) {
            it?.let {
                val (oldestSyncDate, newestSyncDate, size) = it
                binding.playSyncStatusView.text = requireContext().getQuantityText(
                    when {
                        oldestSyncDate == Long.MAX_VALUE && newestSyncDate <= 0L -> R.plurals.plays_sync_status_none
                        oldestSyncDate <= 0L -> R.plurals.plays_sync_status_new
                        newestSyncDate <= 0L -> R.plurals.plays_sync_status_old
                        else -> R.plurals.plays_sync_status_range
                    },
                    size,
                    size,
                    oldestSyncDate.asDate(),
                    newestSyncDate.asDate(),
                )
            }
        }

        viewModel.playSyncProgress.observe(viewLifecycleOwner) {
            it?.let { // test if sync is enabled first!
                binding.syncPlaysButton.isEnabled = (it.step == SyncViewModel.PlaySyncProgressStep.NotSyncing)
                binding.cancelPlaysButton.isEnabled = (it.step != SyncViewModel.PlaySyncProgressStep.NotSyncing)
                binding.progressBar.isVisible = (it.step != SyncViewModel.PlaySyncProgressStep.NotSyncing)
                when (it.step) {
                    SyncViewModel.PlaySyncProgressStep.NotSyncing -> {
                        binding.syncPlaysDateRange.isVisible = false
                        binding.syncPlaysStep.isVisible = false
                    }
                    SyncViewModel.PlaySyncProgressStep.Old -> {
                        binding.syncPlaysStep.setTextOrHide(getString(R.string.sync_plays_step_old).appendPage(it.page))
                        binding.syncPlaysDateRange.setTextOrHide(getSyncDateDescription(it))
                    }
                    SyncViewModel.PlaySyncProgressStep.New -> {
                        binding.syncPlaysStep.setTextOrHide(getString(R.string.sync_plays_step_new).appendPage(it.page))
                        binding.syncPlaysDateRange.setTextOrHide(getSyncDateDescription(it))
                    }
                    SyncViewModel.PlaySyncProgressStep.Stats -> {
                        binding.syncPlaysDateRange.isVisible = false
                        binding.syncPlaysStep.setTextOrHide(R.string.sync_plays_step_stats)
                    }
                }
                val actionResId = when (it.action) {
                    SyncViewModel.PlaySyncProgressAction.None -> ResourcesCompat.ID_NULL
                    SyncViewModel.PlaySyncProgressAction.Waiting -> R.string.sync_plays_action_waiting
                    SyncViewModel.PlaySyncProgressAction.Downloading -> R.string.sync_plays_action_downloading
                    SyncViewModel.PlaySyncProgressAction.Saving -> R.string.sync_plays_action_saving
                    SyncViewModel.PlaySyncProgressAction.Deleting -> R.string.sync_plays_action_deleting
                }
                binding.syncPlaysAction.setTextOrHide(actionResId)
            }
        }

        viewModel.numberOfPlaysToBeUpdated.observe(viewLifecycleOwner) {
            it?.let {
                playsUpdateCount = it
                binding.syncPlaysUpdate.setTextOrHide(requireContext().getQuantityText(R.plurals.plays_pending_update, it, it))
                enableUploadButton()
            }
        }
        viewModel.numberOfPlaysToBeDeleted.observe(viewLifecycleOwner) {
            it?.let {
                playsDeleteCount = it
                binding.syncPlaysDelete.setTextOrHide(requireContext().getQuantityText(R.plurals.plays_pending_deletion, it, it))
                enableUploadButton()
            }
        }
    }

    private fun String.appendPage(page: Int): String {
        val contentText = when {
            page > 1 -> getString(R.string.sync_notification_page_suffix, this, page)
            else -> this
        }
        return contentText
    }

    private fun getSyncDateDescription(it: SyncViewModel.PlaySyncProgress) = when {
        it.minDate == 0L && it.maxDate == 0L -> getString(R.string.sync_notification_plays_all)
        it.minDate == 0L -> getString(R.string.sync_notification_plays_old, it.maxDate.asDate())
        it.maxDate == 0L -> getString(R.string.sync_notification_plays_new, it.minDate.asDate())
        else -> getString(R.string.sync_notification_plays_between, it.minDate.asDate(), it.maxDate.asDate())
    }

    private fun enableUploadButton() {
        binding.uploadPlaysButton.isEnabled = (playsUpdateCount + playsUpdateCount) > 0
    }

    private fun Long.asDate() = this.formatDateTime(requireContext(), flags = DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_ALL)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
