package com.boardgamegeek.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.boardgamegeek.R
import com.boardgamegeek.extensions.requestFocus
import kotlinx.android.synthetic.main.dialog_edit_text.*
import org.jetbrains.anko.support.v4.act

class EditUsernameDialogFragment : DialogFragment() {
    private lateinit var layout: View
    private var listener: EditUsernameDialogListener? = null

    interface EditUsernameDialogListener {
        fun onFinishAddUsername(username: String)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        listener = context as? EditUsernameDialogListener
        if (listener == null) throw ClassCastException("$context must implement EditTextDialogListener")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        @SuppressLint("InflateParams")
        layout = LayoutInflater.from(act).inflate(R.layout.dialog_edit_text, null)

        val builder = AlertDialog.Builder(act, R.style.Theme_bgglight_Dialog_Alert)
                .setTitle(R.string.title_add_username)
                .setView(layout)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok) { _, _ ->
                    listener?.onFinishAddUsername(editText.text.trim().toString())
                }

        return builder.create().apply {
            requestFocus(editText)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        editText.inputType = editText.inputType or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
    }

    companion object {
        @JvmStatic
        fun newInstance(): EditUsernameDialogFragment {
            return EditUsernameDialogFragment()
        }
    }
}
