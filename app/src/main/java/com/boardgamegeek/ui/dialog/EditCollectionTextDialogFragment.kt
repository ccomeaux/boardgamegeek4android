package com.boardgamegeek.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.appcompat.app.AlertDialog
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.boardgamegeek.R
import com.boardgamegeek.extensions.requestFocus
import com.boardgamegeek.extensions.setAndSelectExistingText
import kotlinx.android.synthetic.main.dialog_edit_text.*

class EditCollectionTextDialogFragment : DialogFragment() {
    private lateinit var layout: View
    private var listener: EditCollectionTextDialogListener? = null

    interface EditCollectionTextDialogListener {
        fun onEditCollectionText(text: String, textColumn: String, timestampColumn: String)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        listener = context as? EditCollectionTextDialogListener
        if (listener == null) throw ClassCastException("$context must implement EditTextDialogListener")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        @SuppressLint("InflateParams")
        layout = LayoutInflater.from(context).inflate(R.layout.dialog_edit_text, null)

        val builder = AlertDialog.Builder(requireContext(), R.style.Theme_bgglight_Dialog_Alert)
                .setTitle(arguments?.getString(KEY_TITLE))
                .setView(layout)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok) { _, _ ->
                    listener?.onEditCollectionText(
                            editText.text.trim().toString(),
                            arguments?.getString(KEY_TEXT_COLUMN) ?: "",
                            arguments?.getString(KEY_TIMESTAMP_COLUMN) ?: "")
                }

        return builder.create().apply {
            requestFocus(editText)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        editText.inputType = editText.inputType or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        if (savedInstanceState == null) {
            editText.setAndSelectExistingText(arguments?.getString(KEY_TEXT))
        }
    }

    companion object {
        private const val KEY_TITLE = "title"
        private const val KEY_TEXT = "text"
        private const val KEY_TEXT_COLUMN = "text_column"
        private const val KEY_TIMESTAMP_COLUMN = "timestamp_column"

        @JvmStatic
        fun newInstance(title: String, text: String?, textColumn: String, timestampColumn: String): EditCollectionTextDialogFragment {
            return EditCollectionTextDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(KEY_TITLE, title)
                    putString(KEY_TEXT, text)
                    putString(KEY_TEXT_COLUMN, textColumn)
                    putString(KEY_TIMESTAMP_COLUMN, timestampColumn)
                }
            }
        }
    }
}
