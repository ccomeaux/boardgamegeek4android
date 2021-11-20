package com.boardgamegeek.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.boardgamegeek.R
import com.boardgamegeek.extensions.PlayStats.LOG_PLAY_STATS_ACCESSORIES
import com.boardgamegeek.extensions.PlayStats.LOG_PLAY_STATS_EXPANSIONS
import com.boardgamegeek.extensions.PlayStats.LOG_PLAY_STATS_INCOMPLETE
import com.boardgamegeek.extensions.get
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.extensions.set
import kotlinx.android.synthetic.main.dialog_play_stats_settings_include.*

class PlayStatsIncludeSettingsDialogFragment : DialogFragment() {
    lateinit var layout: View

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        @SuppressLint("InflateParams")
        layout = LayoutInflater.from(context).inflate(R.layout.dialog_play_stats_settings_include, null)
        val prefs = requireContext().preferences()
        return AlertDialog.Builder(requireContext(), R.style.Theme_bgglight_Dialog_Alert)
            .setTitle(R.string.title_settings)
            .setView(layout)
            .setPositiveButton(R.string.ok) { _, _ ->
                prefs[LOG_PLAY_STATS_INCOMPLETE] = includeIncompleteGamesView.isChecked
                prefs[LOG_PLAY_STATS_EXPANSIONS] = includeExpansionsView.isChecked
                prefs[LOG_PLAY_STATS_ACCESSORIES] = includeAccessoriesView.isChecked
            }
            .create()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val prefs = requireContext().preferences()
        includeIncompleteGamesView.isChecked = prefs[LOG_PLAY_STATS_INCOMPLETE, false] ?: false
        includeExpansionsView.isChecked = prefs[LOG_PLAY_STATS_EXPANSIONS, false] ?: false
        includeAccessoriesView.isChecked = prefs[LOG_PLAY_STATS_ACCESSORIES, false] ?: false
    }
}
