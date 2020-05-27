package com.boardgamegeek.ui.dialog

import com.boardgamegeek.R
import com.boardgamegeek.extensions.executeAsyncTask
import com.boardgamegeek.tasks.RenameLocationTask
import com.boardgamegeek.util.fabric.DataManipulationEvent
import kotlinx.android.synthetic.main.dialog_edit_text.*
import org.jetbrains.anko.support.v4.withArguments

class EditLocationNameDialogFragment : AbstractEditTextDialogFragment() {
    override val titleResId = R.string.title_edit_location

    override val hintResId = R.string.location_hint

    override val originalText
        get() = arguments?.getString(KEY_TEXT)

    override fun onPositiveButton() {
        val text = editText?.text?.toString()
        if (text != null && text.isNotBlank()) {
            DataManipulationEvent.log("Location", "Edit")
            RenameLocationTask(context, originalText, text).executeAsyncTask()
        }
    }

    companion object {
        private const val KEY_TEXT = "text"

        @JvmStatic
        fun newInstance(text: String?): EditLocationNameDialogFragment {
            return EditLocationNameDialogFragment().withArguments(
                    KEY_TEXT to text
            )
        }
    }
}
