package com.boardgamegeek.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.boardgamegeek.R
import com.boardgamegeek.databinding.DialogEditTextBinding
import com.boardgamegeek.extensions.requestFocus
import com.boardgamegeek.extensions.setAndSelectExistingText
import com.boardgamegeek.ui.viewmodel.GameCollectionItemViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
abstract class EditCollectionTextDialogFragment : DialogFragment() {
    private var _binding: DialogEditTextBinding? = null
    private val binding get() = _binding!!
    protected var originalText: String? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogEditTextBinding.inflate(layoutInflater)

        val viewModel = ViewModelProvider(requireActivity())[GameCollectionItemViewModel::class.java]
        val builder = AlertDialog.Builder(requireContext(), R.style.Theme_bgglight_Dialog_Alert)
            .setTitle(titleResId)
            .setView(binding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { _, _ ->
                updateText(viewModel, binding.editText.text?.toString()?.trim().orEmpty())
            }

        return builder.create().apply {
            requestFocus(binding.editText)
        }
    }

    protected abstract val titleResId: Int

    protected abstract fun updateText(viewModel: GameCollectionItemViewModel, text: String)

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
        protected const val KEY_TEXT = "text"

        fun createBundle(text: String?) = bundleOf(KEY_TEXT to text)
    }
}
