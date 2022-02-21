package com.boardgamegeek.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.boardgamegeek.R
import com.boardgamegeek.databinding.DialogEditTextBinding
import com.boardgamegeek.extensions.requestFocus
import com.boardgamegeek.extensions.setAndSelectExistingText
import com.boardgamegeek.ui.viewmodel.GameCollectionItemViewModel

class EditCollectionTextDialogFragment : DialogFragment() {
    private var _binding: DialogEditTextBinding? = null
    private val binding get() = _binding!!
    private var originalText: String? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogEditTextBinding.inflate(layoutInflater)

        val viewModel = ViewModelProvider(requireActivity())[GameCollectionItemViewModel::class.java]
        val builder = AlertDialog.Builder(requireContext(), R.style.Theme_bgglight_Dialog_Alert)
            .setTitle(arguments?.getString(KEY_TITLE))
            .setView(binding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { _, _ ->
                val text = binding.editText.text?.toString()
                viewModel.updateText(
                    text?.trim() ?: "",
                    arguments?.getString(KEY_TEXT_COLUMN) ?: "",
                    arguments?.getString(KEY_TIMESTAMP_COLUMN) ?: "",
                    originalText
                )
            }

        return builder.create().apply {
            requestFocus(binding.editText)
        }
    }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.editText.inputType = binding.editText.inputType or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        if (savedInstanceState == null) {
            originalText = arguments?.getString(KEY_TEXT)
            binding.editText.setAndSelectExistingText(originalText)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val KEY_TITLE = "title"
        private const val KEY_TEXT = "text"
        private const val KEY_TEXT_COLUMN = "text_column"
        private const val KEY_TIMESTAMP_COLUMN = "timestamp_column"

        fun newInstance(title: String, text: String?, textColumn: String, timestampColumn: String) =
            EditCollectionTextDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(KEY_TITLE, title)
                    putString(KEY_TEXT, text)
                    putString(KEY_TEXT_COLUMN, textColumn)
                    putString(KEY_TIMESTAMP_COLUMN, timestampColumn)
                }
            }
    }
}
