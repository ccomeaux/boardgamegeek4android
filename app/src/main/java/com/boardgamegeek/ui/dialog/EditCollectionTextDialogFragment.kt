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
import com.boardgamegeek.databinding.DialogEditTextBinding
import com.boardgamegeek.extensions.requestFocus
import com.boardgamegeek.extensions.setAndSelectExistingText

class EditCollectionTextDialogFragment : DialogFragment() {
    private var _binding: DialogEditTextBinding? = null
    private val binding get() = _binding!!
    private var listener: EditCollectionTextDialogListener? = null

    interface EditCollectionTextDialogListener {
        fun onEditCollectionText(text: String, textColumn: String, timestampColumn: String)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? EditCollectionTextDialogListener
        if (listener == null) throw ClassCastException("$context must implement EditTextDialogListener")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        @SuppressLint("InflateParams")
        _binding = DialogEditTextBinding.inflate(LayoutInflater.from(context))

        val builder = AlertDialog.Builder(requireContext(), R.style.Theme_bgglight_Dialog_Alert)
                .setTitle(arguments?.getString(KEY_TITLE))
                .setView(binding.root)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok) { _, _ ->
                    listener?.onEditCollectionText(
                            binding.editText.text.trim().toString(),
                            arguments?.getString(KEY_TEXT_COLUMN) ?: "",
                            arguments?.getString(KEY_TIMESTAMP_COLUMN) ?: "")
                }

        return builder.create().apply {
            requestFocus(binding.editText)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.editText.inputType = binding.editText.inputType or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        if (savedInstanceState == null) {
            binding.editText.setAndSelectExistingText(arguments?.getString(KEY_TEXT))
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
