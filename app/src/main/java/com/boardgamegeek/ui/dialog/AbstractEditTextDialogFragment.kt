package com.boardgamegeek.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.boardgamegeek.R
import com.boardgamegeek.extensions.requestFocus
import com.boardgamegeek.extensions.setAndSelectExistingText
import kotlinx.android.synthetic.main.dialog_edit_text.*

abstract class AbstractEditTextDialogFragment : DialogFragment() {
    private lateinit var layout: View

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        @SuppressLint("InflateParams")
        layout = LayoutInflater.from(context).inflate(R.layout.dialog_edit_text, null)

        val builder = AlertDialog.Builder(requireContext(), R.style.Theme_bgglight_Dialog_Alert)
                .setView(layout)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok) { _, _ ->
                    onPositiveButton()
                }
        if (titleResId != 0) builder.setTitle(titleResId)
        return builder.create().apply {
            requestFocus(editText)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (hintResId != 0) editTextContainer.hint = getString(hintResId)
        editText.inputType = editText.inputType or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        if (savedInstanceState == null) {
            editText.setAndSelectExistingText(originalText)
        }
    }

    open val titleResId
        get() = 0

    open val hintResId
        get() = 0

    open val originalText: String?
        get() = null

    protected abstract fun onPositiveButton()
}
