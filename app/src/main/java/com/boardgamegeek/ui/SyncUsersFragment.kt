package com.boardgamegeek.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentSyncUsersBinding
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.viewmodel.SyncViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SyncUsersFragment : Fragment() {
    private var _binding: FragmentSyncUsersBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<SyncViewModel>()

    private val prefs: SharedPreferences by lazy { requireContext().preferences() }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentSyncUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.containerView.layoutTransition.setAnimateParentHierarchy(false)

        binding.syncBuddiesSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs[PREFERENCES_KEY_SYNC_BUDDIES] = isChecked
            binding.syncBuddiesButton.isVisible = isChecked
            binding.cancelBuddiesButton.isVisible = isChecked
        }

        binding.syncBuddiesButton.setOnClickListener {
            viewModel.syncBuddies()
        }
        binding.cancelBuddiesButton.setOnClickListener {
            viewModel.cancelBuddies()
        }

        viewModel.syncBuddies.observe(viewLifecycleOwner) {
            it?.let {
                binding.syncBuddiesSwitch.isChecked = it
            }
        }
        viewModel.buddySyncDate.observe(viewLifecycleOwner) {
            it?.let {
                binding.playBuddiesDateView.setTextOrHide(
                    when {
                        it <= 0L -> getString(R.string.sync_buddies_date_zero)
                        else -> getString(R.string.sync_buddies_date, it.asDateTime())
                    }
                )
            }
        }
        viewModel.userSyncState.observe(viewLifecycleOwner) {
            it?.let {
                binding.userCountView.setTextOrHide(
                    requireContext().getQuantityText(
                        R.plurals.users_synced_total,
                        it.count,
                        it.count,
                        it.oldestUpdatedBuddyTimestamp.asDateTime(),
                    )
                )
                binding.unupdatedUserCountView.setTextOrHide(
                    requireContext().getQuantityText(
                        R.plurals.users_unupdated_total,
                        it.numberOfUnupdatedBuddies,
                        it.numberOfUnupdatedBuddies,
                    ),
                    it.numberOfUnupdatedBuddies > 0
                )
            }
        }
        viewModel.userProgress.observe(viewLifecycleOwner) {
            it?.let {
                when (it.step) {
                    SyncViewModel.UserSyncProgressStep.BuddyList -> {
                        updateSyncProgress(R.string.sync_user_step_list, it.progress, it.max)
                    }
                    SyncViewModel.UserSyncProgressStep.StaleUsers -> {
                        updateSyncProgress(R.string.sync_user_step_stale, it.progress, it.max)
                    }
                    SyncViewModel.UserSyncProgressStep.UnupdatedUsers -> {
                        updateSyncProgress(R.string.sync_user_step_unupdated, it.progress, it.max)
                    }
                    SyncViewModel.UserSyncProgressStep.NotSyncing -> {
                        binding.stepSyncingView.isVisible = false
                        binding.currentUsernameSyncingView.isVisible = false
                        binding.progressBar.isVisible = false
                        binding.syncBuddiesButton.isEnabled = true
                        binding.cancelBuddiesButton.isEnabled = false
                    }
                }
                binding.currentUsernameSyncingView.setTextOrHide(
                    getString(R.string.sync_notification_user, it.username),
                    !it.username.isNullOrBlank()
                )
            }
        }
    }

    private fun updateSyncProgress(@StringRes resId: Int, progress: Int, max: Int) {
        binding.stepSyncingView.setTextOrHide(getString(resId))
        binding.syncBuddiesButton.isEnabled = false
        binding.cancelBuddiesButton.isEnabled = true
        binding.progressBar.setProgressOrIndeterminate(progress, max)
    }

    private fun Long?.asDateTime(): CharSequence {
        return this?.formatDateTime(
            requireContext(),
            flags = DateUtils.FORMAT_ABBREV_ALL or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME
        ) ?: getString(R.string.never)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
