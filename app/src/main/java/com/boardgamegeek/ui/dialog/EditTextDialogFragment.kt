package com.boardgamegeek.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.boardgamegeek.R
import com.boardgamegeek.extensions.requestFocus
import com.boardgamegeek.extensions.setAndSelectExistingText
import kotlinx.android.synthetic.main.dialog_edit_text.*
import org.jetbrains.anko.support.v4.act

class EditTextDialogFragment : DialogFragment() {
    private lateinit var layout: View
    private var listener: EditTextDialogListener? = null

    interface EditTextDialogListener {
        fun onFinishEditDialog(text: String, originalText: String?)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        listener = context as? EditTextDialogListener
        if (listener == null) throw ClassCastException("$context must implement EditTextDialogListener")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        @SuppressLint("InflateParams")
        layout = LayoutInflater.from(act).inflate(R.layout.dialog_edit_text, null)

        val builder = AlertDialog.Builder(act, R.style.Theme_bgglight_Dialog_Alert)
                .setTitle(arguments?.getInt(KEY_TITLE_ID) ?: 0)
                .setView(layout)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok) { _, _ ->
                    listener?.onFinishEditDialog(editText.text.trim().toString(), arguments?.getString(KEY_TEXT))
                }

        return builder.create().apply {
            requestFocus(editText)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        editText.inputType = editText.inputType or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        if (savedInstanceState == null) {
            editText.setAndSelectExistingText(arguments?.getString(KEY_TEXT))
        }
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
