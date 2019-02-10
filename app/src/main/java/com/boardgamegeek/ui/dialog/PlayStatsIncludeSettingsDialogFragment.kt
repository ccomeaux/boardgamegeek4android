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
import com.boardgamegeek.util.PreferencesUtils
import kotlinx.android.synthetic.main.dialog_play_stats_settings_include.*

class PlayStatsIncludeSettingsDialogFragment : DialogFragment() {
    lateinit var layout: View

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        @SuppressLint("InflateParams")
        layout = LayoutInflater.from(context).inflate(R.layout.dialog_play_stats_settings_include, null)

        return AlertDialog.Builder(requireContext(), R.style.Theme_bgglight_Dialog_Alert)
                .setTitle(R.string.title_settings)
                .setView(layout)
                .setPositiveButton(R.string.ok) { _, _ ->
                    PreferencesUtils.putPlayStatsIncomplete(context, includeIncompleteGamesView.isChecked)
                    PreferencesUtils.putPlayStatsExpansions(context, includeExpansionsView.isChecked)
                    PreferencesUtils.putPlayStatsAccessories(context, includeAccessoriesView.isChecked)
                }
                .create()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        includeIncompleteGamesView.isChecked = PreferencesUtils.logPlayStatsIncomplete(context)
        includeExpansionsView.isChecked = PreferencesUtils.logPlayStatsExpansions(context)
        includeAccessoriesView.isChecked = PreferencesUtils.logPlayStatsAccessories(context)
    }

    companion object {
        fun newInstance(): PlayStatsIncludeSettingsDialogFragment {
            return PlayStatsIncludeSettingsDialogFragment()
        }
    }
}
