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
import com.boardgamegeek.extensions.CollectionViewPrefs
import com.boardgamegeek.extensions.createThemedBuilder
import com.boardgamegeek.extensions.requestFocus
import com.boardgamegeek.extensions.setAndSelectExistingText
import com.boardgamegeek.ui.viewmodel.CollectionViewViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SaveViewDialogFragment : DialogFragment() {
    private var _binding: DialogSaveViewBinding? = null
    private val binding get() = _binding!!
    private var name: String = ""
    private var description: String? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogSaveViewBinding.inflate(layoutInflater)
        val viewModel = ViewModelProvider(requireActivity())[CollectionViewViewModel::class.java]

        arguments?.let {
            name = it.getString(KEY_NAME).orEmpty()
            description = it.getString(KEY_DESCRIPTION)
        }

        val builder = requireContext().createThemedBuilder()
            .setTitle(R.string.title_save_view)
            .setView(binding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = binding.nameView.text?.trim()?.toString().orEmpty()
                val isDefault = binding.defaultViewCheckBox.isChecked
                if (viewModel.findViewId(name) > 0) {
                    requireContext().createThemedBuilder()
                        .setTitle(R.string.title_collection_view_name_in_use)
                        .setMessage(R.string.msg_collection_view_name_in_use)
                        .setPositiveButton(R.string.update) { _, _ ->
                            viewModel.update(name, isDefault)
                        }
                        .setNegativeButton(R.string.create) { _, _ ->
                            viewModel.insert(name, isDefault)
                        }
                        .create()
                        .show()
                } else {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val viewModel = ViewModelProvider(requireActivity())[CollectionViewViewModel::class.java]
        binding.nameView.setAndSelectExistingText(name)
        binding.defaultViewCheckBox.isChecked =
            viewModel.defaultViewId != CollectionViewPrefs.DEFAULT_DEFAULT_ID && viewModel.findViewId(name) == viewModel.defaultViewId
        binding.descriptionView.text = description
    }

    companion object {
        private const val KEY_NAME = "title_id"
        private const val KEY_DESCRIPTION = "color_count"

        fun newInstance(name: String, description: String) = SaveViewDialogFragment().apply {
            arguments = Bundle().apply {
                putString(KEY_NAME, name)
                putString(KEY_DESCRIPTION, description)
            }
        }
    }
}
