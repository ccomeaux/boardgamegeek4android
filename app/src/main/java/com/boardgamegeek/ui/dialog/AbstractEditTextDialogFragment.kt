package com.boardgamegeek.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.boardgamegeek.R
import com.boardgamegeek.databinding.DialogEditTextBinding
import com.boardgamegeek.extensions.requestFocus
import com.boardgamegeek.extensions.setAndSelectExistingText

abstract class AbstractEditTextDialogFragment : DialogFragment() {
    private var _binding: DialogEditTextBinding? = null
    protected val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogEditTextBinding.inflate(layoutInflater)

        val builder = AlertDialog.Builder(requireContext(), R.style.Theme_bgglight_Dialog_Alert)
            .setView(binding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { _, _ ->
                onPositiveButton()
            }
        if (titleResId != 0) builder.setTitle(titleResId)
        return builder.create().apply {
            requestFocus(binding.editText)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (hintResId != 0) binding.editTextContainer.hint = getString(hintResId)
        binding.editText.inputType = binding.editText.inputType or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        if (savedInstanceState == null) {
            binding.editText.setAndSelectExistingText(originalText)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    open val titleResId
        get() = 0

    open val hintResId
        get() = 0

    open val originalText: String?
        get() = null

    protected abstract fun onPositiveButton()
}
