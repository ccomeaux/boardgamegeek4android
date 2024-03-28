package com.boardgamegeek.ui.dialog

import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.ui.viewmodel.PlaysViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditLocationNameDialogFragment : AbstractEditTextDialogFragment() {
    private val viewModel by activityViewModels<PlaysViewModel>()

    override val titleResId = R.string.title_edit_location

    override val hintResId = R.string.location_hint

    override val originalText
        get() = arguments?.getString(KEY_TEXT)

    override fun onPositiveButton() {
        val text = binding.editText.text?.toString()
        if (!text.isNullOrBlank()) {
            FirebaseAnalytics.getInstance(requireContext()).logEvent("DataManipulation") {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Location")
                param("Action", "Edit")
            }
            viewModel.renameLocation(originalText.orEmpty(), text)
        }
    }

    companion object {
        private const val KEY_TEXT = "text"

        fun newInstance(text: String?) = EditLocationNameDialogFragment().apply {
            arguments = bundleOf(KEY_TEXT to text)
        }
    }
}
