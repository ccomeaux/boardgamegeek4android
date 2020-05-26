package com.boardgamegeek.ui.dialog

import android.content.Context
import androidx.annotation.StringRes
import kotlinx.android.synthetic.main.dialog_edit_text.*
import org.jetbrains.anko.support.v4.withArguments

class EditTextDialogFragment : AbstractEditTextDialogFragment() {
    private var listener: EditTextDialogListener? = null

    interface EditTextDialogListener {
        fun onFinishEditDialog(text: String, originalText: String?)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? EditTextDialogListener
        if (listener == null) throw ClassCastException("$context must implement EditTextDialogListener")
    }

    override val titleResId
        get() = arguments?.getInt(KEY_TITLE_ID) ?: 0

    override val hintResId
        get() = arguments?.getInt(KEY_HINT_ID) ?: 0

    override val originalText
        get() = arguments?.getString(KEY_TEXT)

    override fun onPositiveButton() {
        val text = editText?.text?.toString()
        listener?.onFinishEditDialog(text?.trim() ?: "", originalText)
    }

    companion object {
        private const val KEY_TITLE_ID = "title_id"
        private const val KEY_HINT_ID = "hint_id"
        private const val KEY_TEXT = "text"

        @JvmStatic
        fun newInstance(@StringRes titleResId: Int, text: String?, @StringRes hintResId: Int = 0): EditTextDialogFragment {
            return EditTextDialogFragment().withArguments(
                    KEY_TITLE_ID to titleResId,
                    KEY_TEXT to text,
                    KEY_HINT_ID to hintResId
            )
        }
    }
}
