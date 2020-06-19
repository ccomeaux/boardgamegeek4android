package com.boardgamegeek.ui.dialog

import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.ui.viewmodel.PlaysViewModel
import com.boardgamegeek.util.fabric.DataManipulationEvent
import kotlinx.android.synthetic.main.dialog_edit_text.*
import org.jetbrains.anko.support.v4.withArguments

class EditLocationNameDialogFragment : AbstractEditTextDialogFragment() {
    private val viewModel by activityViewModels<PlaysViewModel>()

    override val titleResId = R.string.title_edit_location

    override val hintResId = R.string.location_hint

    override val originalText
        get() = arguments?.getString(KEY_TEXT)

    override fun onPositiveButton() {
        val text = editText?.text?.toString()
        if (text != null && text.isNotBlank()) {
            DataManipulationEvent.log("Location", "Edit")
            viewModel.renameLocation(originalText ?: "", text)
        }
    }

    companion object {
        private const val KEY_TEXT = "text"

        fun newInstance(text: String?): EditLocationNameDialogFragment {
            return EditLocationNameDialogFragment().withArguments(
                    KEY_TEXT to text
            )
        }
    }
}
