package com.boardgamegeek.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.boardgamegeek.R
import com.boardgamegeek.util.PreferencesUtils
import kotlinx.android.synthetic.main.dialog_play_stats_settings_include.*
import org.jetbrains.anko.support.v4.ctx

class PlayStatsIncludeSettingsDialogFragment : DialogFragment() {
    lateinit var layout: View

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        @SuppressLint("InflateParams")
        layout = LayoutInflater.from(ctx).inflate(R.layout.dialog_play_stats_settings_include, null)

        return AlertDialog.Builder(ctx, R.style.Theme_bgglight_Dialog_Alert)
                .setTitle(R.string.title_settings)
                .setView(layout)
                .setPositiveButton(R.string.ok) { _, _ ->
                    PreferencesUtils.putPlayStatsIncomplete(ctx, includeIncompleteGamesView.isChecked)
                    PreferencesUtils.putPlayStatsExpansions(ctx, includeExpansionsView.isChecked)
                    PreferencesUtils.putPlayStatsAccessories(ctx, includeAccessoriesView.isChecked)
                }
                .create()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        includeIncompleteGamesView.isChecked = PreferencesUtils.logPlayStatsIncomplete(ctx)
        includeExpansionsView.isChecked = PreferencesUtils.logPlayStatsExpansions(ctx)
        includeAccessoriesView.isChecked = PreferencesUtils.logPlayStatsAccessories(ctx)
    }

    companion object {
        fun newInstance(): PlayStatsIncludeSettingsDialogFragment {
            return PlayStatsIncludeSettingsDialogFragment()
        }
    }
}
