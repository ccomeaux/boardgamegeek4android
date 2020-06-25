package com.boardgamegeek.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.CollectionViews
import com.boardgamegeek.ui.viewmodel.CollectionViewViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import kotlinx.android.synthetic.main.dialog_save_view.*
import org.jetbrains.anko.support.v4.defaultSharedPreferences

class SaveViewDialogFragment : DialogFragment() {
    lateinit var layout: View
    private var name: String = ""
    private var description: String? = null
    private val firebaseAnalytics by lazy { FirebaseAnalytics.getInstance(requireContext()) }

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        layout = LayoutInflater.from(context).inflate(R.layout.dialog_save_view, null)
        val viewModel = ViewModelProvider(requireActivity()).get(CollectionViewViewModel::class.java)
        val toast = Toast.makeText(requireContext(), R.string.msg_saved, Toast.LENGTH_SHORT) // TODO improve message

        arguments?.let {
            name = it.getString(KEY_NAME) ?: ""
            description = it.getString(KEY_DESCRIPTION)
        }

        val builder = AlertDialog.Builder(requireContext(), R.style.Theme_bgglight_Dialog_Alert)
                .setTitle(R.string.title_save_view)
                .setView(layout)
                .setPositiveButton(R.string.save) { _, _ ->
                    val name = nameView.text?.trim()?.toString() ?: ""
                    val isDefault = defaultViewCheckBox.isChecked
                    val viewId = findViewId(name)
                    if (viewId > 0) {
                        AlertDialog.Builder(requireContext())
                                .setTitle(R.string.title_collection_view_name_in_use)
                                .setMessage(R.string.msg_collection_view_name_in_use)
                                .setPositiveButton(R.string.update) { _, _ ->
                                    toast.show()
                                    logAction("Update", name)
                                    viewModel.update(isDefault)
                                }
                                .setNegativeButton(R.string.create) { _, _ ->
                                    toast.show()
                                    logAction("Insert", name)
                                    viewModel.insert(name, isDefault)
                                }
                                .create()
                                .show()
                    } else {
                        toast.show()
                        logAction("Insert", name)
                        viewModel.insert(name, isDefault)
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .setCancelable(true)
        return builder.create().apply {
            requestFocus(nameView)
            setOnShowListener { enableSaveButton(this, nameView) }
        }
    }

    private fun logAction(action: String, name: String) {
        firebaseAnalytics.logEvent("DataManipulation") {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "CollectionView")
            param("Action", action)
            param("Color", name)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        nameView.setAndSelectExistingText(name)
        val viewDefaultId = defaultSharedPreferences[CollectionView.PREFERENCES_KEY_DEFAULT_ID, CollectionView.DEFAULT_DEFAULT_ID]
                ?: CollectionView.DEFAULT_DEFAULT_ID
        defaultViewCheckBox.isChecked = viewDefaultId != CollectionView.DEFAULT_DEFAULT_ID && findViewId(name) == viewDefaultId
        descriptionView.text = description
    }

    private fun findViewId(name: String): Long {
        return if (name.isBlank())
            BggContract.INVALID_ID.toLong()
        else
            requireContext().contentResolver.queryLong(
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
        fun newInstance(name: String, description: String): SaveViewDialogFragment {
            return SaveViewDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(KEY_NAME, name)
                    putString(KEY_DESCRIPTION, description)
                }
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
