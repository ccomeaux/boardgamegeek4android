package com.boardgamegeek.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.appcompat.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.boardgamegeek.R
import com.boardgamegeek.databinding.DialogPlayStatsSettingsIncludeBinding
import com.boardgamegeek.util.PreferencesUtils

class PlayStatsIncludeSettingsDialogFragment : DialogFragment() {
    private var _binding: DialogPlayStatsSettingsIncludeBinding? = null
    private val binding get() = _binding!!
    lateinit var layout: View

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogPlayStatsSettingsIncludeBinding.inflate(LayoutInflater.from(context))
        layout = binding.root

        return AlertDialog.Builder(requireContext(), R.style.Theme_bgglight_Dialog_Alert)
                .setTitle(R.string.title_settings)
                .setView(layout)
                .setPositiveButton(R.string.ok) { _, _ ->
                    PreferencesUtils.putPlayStatsIncomplete(context, binding.includeIncompleteGamesView.isChecked)
                    PreferencesUtils.putPlayStatsExpansions(context, binding.includeExpansionsView.isChecked)
                    PreferencesUtils.putPlayStatsAccessories(context, binding.includeAccessoriesView.isChecked)
                }
                .create()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.includeIncompleteGamesView.isChecked = PreferencesUtils.logPlayStatsIncomplete(context)
        binding.includeExpansionsView.isChecked = PreferencesUtils.logPlayStatsExpansions(context)
        binding.includeAccessoriesView.isChecked = PreferencesUtils.logPlayStatsAccessories(context)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): PlayStatsIncludeSettingsDialogFragment {
            return PlayStatsIncludeSettingsDialogFragment()
        }
    }
}
