package com.boardgamegeek.ui.dialog

import android.content.Context
import android.os.Bundle
import androidx.annotation.StringRes
import kotlinx.android.synthetic.main.dialog_edit_text.*

class EditTextDialogFragment : AbstractEditTextDialogFragment() {
    private var listener: EditTextDialogListener? = null

    interface EditTextDialogListener {
        fun onFinishEditDialog(text: String, originalText: String?)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        listener = context as? EditTextDialogListener
        if (listener == null) throw ClassCastException("$context must implement EditTextDialogListener")
    }

    override val titleResId
        get() = arguments?.getInt(KEY_TITLE_ID) ?: 0

    override val originalText
        get() = arguments?.getString(KEY_TEXT)

    override fun onPositiveButton() {
        listener?.onFinishEditDialog(editText.text.trim().toString(), originalText)
    }

    companion object {
        private const val KEY_TITLE_ID = "title_id"
        private const val KEY_TEXT = "text"

        @JvmStatic
        fun newInstance(@StringRes titleResId: Int, text: String?): EditTextDialogFragment {
            return EditTextDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(KEY_TITLE_ID, titleResId)
                    putString(KEY_TEXT, text)
                }
            }
        }
    }
}
