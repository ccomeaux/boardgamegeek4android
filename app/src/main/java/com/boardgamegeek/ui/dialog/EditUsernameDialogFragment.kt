package com.boardgamegeek.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.DialogEditTextBinding
import com.boardgamegeek.extensions.requestFocus
import com.boardgamegeek.ui.viewmodel.BuddyViewModel

class EditUsernameDialogFragment : DialogFragment() {
    private var _binding: DialogEditTextBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<BuddyViewModel>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogEditTextBinding.inflate(layoutInflater)

        val builder = AlertDialog.Builder(requireContext(), R.style.Theme_bgglight_Dialog_Alert)
            .setTitle(R.string.title_add_username)
            .setView(binding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { _, _ ->
                val text = binding.editText.text?.toString()
                viewModel.addUsernameToPlayer(text?.trim() ?: "")
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
        binding.editText.inputType = binding.editText.inputType or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        binding.editTextContainer.hint = getString(R.string.username)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
