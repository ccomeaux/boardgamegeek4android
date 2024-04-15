package com.boardgamegeek.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.extensions.*
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentSyncBinding
import com.boardgamegeek.ui.viewmodel.SyncViewModel
import com.boardgamegeek.work.SyncPlaysWorker
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SyncFragment : Fragment() {
    private var _binding: FragmentSyncBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<SyncViewModel>()

    private val prefs: SharedPreferences by lazy { requireContext().preferences() }

    private var oldestSyncDate = Long.MAX_VALUE
    private var newestSyncDate = 0L

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentSyncBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

         binding.syncPlaysSwitch2.setOnCheckedChangeListener { _, isChecked ->
             prefs[PREFERENCES_KEY_SYNC_PLAYS] = isChecked
         }
        binding.syncPlaysButton.setOnClickListener {
            SyncPlaysWorker.requestSync(requireContext())
        }

        viewModel.syncPlays.observe(viewLifecycleOwner) {
            it?.let { binding.syncPlaysSwitch2.isChecked = it }
        }
        viewModel.syncPlaysTimestamp.observe(viewLifecycleOwner) {
            it?.let {
                binding.lastPlaySyncView.text = "Plays sync disabled at ${it.asDateTime()}"
            }
        }
        viewModel.oldestSyncDate.observe(viewLifecycleOwner) {
            oldestSyncDate = it ?: Long.MAX_VALUE
            bindStatusMessage()
        }
        viewModel.newestSyncDate.observe(viewLifecycleOwner) {
            newestSyncDate = it ?: 0L
            bindStatusMessage()
        }
    }

    private fun bindStatusMessage() {
        binding.playSyncStatusView.text = when {
            oldestSyncDate == Long.MAX_VALUE && newestSyncDate <= 0L -> getString(R.string.plays_sync_status_none)
            oldestSyncDate <= 0L -> String.format(getString(R.string.plays_sync_status_new), newestSyncDate.asDate())
            newestSyncDate <= 0L -> String.format(getString(R.string.plays_sync_status_old), oldestSyncDate.asDate())
            else -> String.format(
                getString(R.string.plays_sync_status_range),
                oldestSyncDate.asDate(),
                newestSyncDate.asDate()
            )
        }
    }

    private fun Long.asDate() = this.formatDateTime(requireContext(), flags = DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_ALL)

    private fun Long.asDateTime() = this.formatDateTime(
        requireContext(),
        flags = DateUtils.FORMAT_ABBREV_ALL or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME
    )

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
