package com.boardgamegeek.pref

import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceDialogFragmentCompat
import com.boardgamegeek.ui.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfirmDialogFragment : PreferenceDialogFragmentCompat() {
    private val viewModel by activityViewModels<SettingsViewModel>()

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            when (arguments?.getString(ARG_KEY)) {
                "clear" -> viewModel.clearAllData()
                "collection" -> viewModel.resetCollectionItems()
                "plays" -> viewModel.resetPlays()
                "buddies" -> viewModel.resetUsers()
            }
        }
    }

    companion object {
        fun newInstance(key: String): ConfirmDialogFragment {
            return ConfirmDialogFragment().apply {
                arguments = bundleOf(ARG_KEY to key)
            }
        }
    }
}
