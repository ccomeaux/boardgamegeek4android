package com.boardgamegeek.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.boardgamegeek.R
import com.boardgamegeek.databinding.DialogPlayStatsSettingsIncludeBinding
import com.boardgamegeek.extensions.PlayStats.LOG_PLAY_STATS_ACCESSORIES
import com.boardgamegeek.extensions.PlayStats.LOG_PLAY_STATS_EXPANSIONS
import com.boardgamegeek.extensions.PlayStats.LOG_PLAY_STATS_INCOMPLETE
import com.boardgamegeek.extensions.createThemedBuilder
import com.boardgamegeek.extensions.get
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.extensions.set

class PlayStatsIncludeSettingsDialogFragment : DialogFragment() {
    private var _binding: DialogPlayStatsSettingsIncludeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        @SuppressLint("InflateParams")
        _binding = DialogPlayStatsSettingsIncludeBinding.inflate(layoutInflater, null, false)
        val prefs = requireContext().preferences()
        return requireContext().createThemedBuilder()
            .setTitle(R.string.title_settings)
            .setView(binding.root)
            .setPositiveButton(R.string.ok) { _, _ ->
                prefs[LOG_PLAY_STATS_INCOMPLETE] = binding.includeIncompleteGamesView.isChecked
                prefs[LOG_PLAY_STATS_EXPANSIONS] = binding.includeExpansionsView.isChecked
                prefs[LOG_PLAY_STATS_ACCESSORIES] = binding.includeAccessoriesView.isChecked
            }
            .create()
    }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val prefs = requireContext().preferences()
        binding.includeIncompleteGamesView.isChecked = prefs[LOG_PLAY_STATS_INCOMPLETE, false] ?: false
        binding.includeExpansionsView.isChecked = prefs[LOG_PLAY_STATS_EXPANSIONS, false] ?: false
        binding.includeAccessoriesView.isChecked = prefs[LOG_PLAY_STATS_ACCESSORIES, false] ?: false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
