package com.boardgamegeek.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.boardgamegeek.R
import com.boardgamegeek.extensions.queryLong
import com.boardgamegeek.extensions.requestFocus
import com.boardgamegeek.extensions.setAndSelectExistingText
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.CollectionViews
import com.boardgamegeek.util.PreferencesUtils
import kotlinx.android.synthetic.main.dialog_save_view.*
import org.jetbrains.anko.support.v4.ctx

class SaveViewDialogFragment : DialogFragment() {
    lateinit var layout: View
    private var root: ViewGroup? = null
    private var listener: OnViewSavedListener? = null
    private var name: String = ""
    private var description: String? = null

    interface OnViewSavedListener {
        fun onInsertRequested(name: String, isDefault: Boolean)

        fun onUpdateRequested(name: String, isDefault: Boolean, viewId: Long)
    }

    fun initialize(root: ViewGroup?, listener: OnViewSavedListener, name: String, description: String) {
        this.root = root
        this.listener = listener
        arguments = Bundle().apply {
            putString(KEY_NAME, name)
            putString(KEY_DESCRIPTION, description)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        layout = LayoutInflater.from(ctx).inflate(R.layout.dialog_save_view, root, false)

        arguments?.let {
            name = it.getString(KEY_NAME)
            description = it.getString(KEY_DESCRIPTION)
        }

        val builder = AlertDialog.Builder(ctx, R.style.Theme_bgglight_Dialog_Alert)
                .setTitle(R.string.title_save_view)
                .setView(layout)
                .setPositiveButton(R.string.save) { _, _ ->
                    val name = nameView.text.trim().toString()
                    val isDefault = defaultViewCheckBox.isChecked
                    val viewId = findViewId(name)
                    if (viewId > 0) {
                        AlertDialog.Builder(ctx)
                                .setTitle(R.string.title_collection_view_name_in_use)
                                .setMessage(R.string.msg_collection_view_name_in_use)
                                .setPositiveButton(R.string.update) { _, _ -> listener?.onUpdateRequested(name, isDefault, viewId) }
                                .setNegativeButton(R.string.create) { _, _ -> listener?.onInsertRequested(name, isDefault) }
                                .create()
                                .show()
                    } else {
                        listener?.onInsertRequested(name, isDefault)
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .setCancelable(true)
        return builder.create().apply {
            requestFocus(nameView)
            setOnShowListener { enableSaveButton(this, nameView) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        nameView.setAndSelectExistingText(name)
        val viewDefaultId = PreferencesUtils.getViewDefaultId(ctx)
        defaultViewCheckBox.isChecked = viewDefaultId != -1L && findViewId(name) == viewDefaultId
        descriptionView.text = description
    }

    private fun findViewId(name: String): Long {
        return if (name.isBlank())
            BggContract.INVALID_ID.toLong()
        else
            ctx.contentResolver.queryLong(
                    CollectionViews.CONTENT_URI,
                    CollectionViews._ID,
                    0L,
                    "${CollectionViews.NAME}=?",
                    arrayOf(name))
    }

    companion object {
        private const val KEY_NAME = "title_id"
        private const val KEY_DESCRIPTION = "color_count"

        @JvmStatic
        fun newInstance(
                root: ViewGroup?,
                listener: OnViewSavedListener,
                name: String,
                description: String
        ): SaveViewDialogFragment {
            return SaveViewDialogFragment().apply {
                initialize(root, listener, name, description)
            }
        }

        private fun enableSaveButton(dialog: AlertDialog, nameView: EditText) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = !nameView.text.isNullOrBlank()
            nameView.addTextChangedListener(object : TextWatcher {
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

                override fun afterTextChanged(s: Editable) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = !nameView.text.isNullOrBlank()
                }
            })
        }
    }
}
