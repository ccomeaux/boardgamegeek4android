package com.boardgamegeek.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.boardgamegeek.R
import com.boardgamegeek.databinding.DialogSaveViewBinding
import com.boardgamegeek.extensions.CollectionView
import com.boardgamegeek.extensions.requestFocus
import com.boardgamegeek.extensions.setAndSelectExistingText
import com.boardgamegeek.extensions.toast
import com.boardgamegeek.ui.viewmodel.CollectionViewViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent

class SaveViewDialogFragment : DialogFragment() {
    private var name: String = ""
    private var description: String? = null
    private var _binding: DialogSaveViewBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = DialogSaveViewBinding.inflate(LayoutInflater.from(context), null, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val viewModel = ViewModelProvider(requireActivity())[CollectionViewViewModel::class.java]

        arguments?.let {
            name = it.getString(KEY_NAME).orEmpty()
            description = it.getString(KEY_DESCRIPTION)
        }

        val firebaseAnalytics = FirebaseAnalytics.getInstance(requireContext())
        val builder = AlertDialog.Builder(requireContext(), R.style.Theme_bgglight_Dialog_Alert)
            .setTitle(R.string.title_save_view)
            .setView(binding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = binding.nameView.text?.trim()?.toString().orEmpty()
                val isDefault = binding.defaultViewCheckBox.isChecked
                if (viewModel.findViewId(name) > 0L) {
                    AlertDialog.Builder(requireContext())
                        .setTitle(R.string.title_collection_view_name_in_use)
                        .setMessage(R.string.msg_collection_view_name_in_use)
                        .setPositiveButton(R.string.update) { _, _ ->
                            requireContext().toast(getString(R.string.msg_collection_view_updated, name))
                            logAction(firebaseAnalytics, "Update", name)
                            viewModel.update(isDefault)
                        }
                        .setNegativeButton(R.string.create) { _, _ ->
                            requireContext().toast(getString(R.string.msg_collection_view_inserted, name))
                            logAction(firebaseAnalytics, "Insert", name)
                            viewModel.insert(name, isDefault)
                        }
                        .create()
                        .show()
                } else {
                    requireContext().toast(getString(R.string.msg_collection_view_inserted, name))
                    logAction(firebaseAnalytics, "Insert", name)
                    viewModel.insert(name, isDefault)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .setCancelable(true)

        return builder.create().apply {
            setOnShowListener {
                requestFocus(binding.nameView)
                getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = !binding.nameView.text.isNullOrBlank()
                binding.nameView.doAfterTextChanged {
                    getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = !binding.nameView.text.isNullOrBlank()
                }
            }
        }
    }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return binding.root
    }

    private fun logAction(firebaseAnalytics: FirebaseAnalytics, action: String, name: String) {
        firebaseAnalytics.logEvent("DataManipulation") {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "CollectionView")
            param("Action", action)
            param("Name", name)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val viewModel = ViewModelProvider(requireActivity())[CollectionViewViewModel::class.java]
        binding.nameView.setAndSelectExistingText(name)
        binding.defaultViewCheckBox.isChecked =
            viewModel.defaultViewId != CollectionView.DEFAULT_DEFAULT_ID && viewModel.findViewId(name) == viewModel.defaultViewId
        binding.descriptionView.text = description
    }

    companion object {
        private const val KEY_NAME = "title_id"
        private const val KEY_DESCRIPTION = "color_count"

        fun newInstance(name: String, description: String): SaveViewDialogFragment {
            return SaveViewDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(KEY_NAME, name)
                    putString(KEY_DESCRIPTION, description)
                }
            }
        }
    }
}
