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
import androidx.lifecycle.ViewModelProvider
import com.boardgamegeek.R
import com.boardgamegeek.databinding.DialogEditTextBinding
import com.boardgamegeek.extensions.requestFocus
import com.boardgamegeek.ui.viewmodel.BuddyViewModel
import org.jetbrains.anko.support.v4.act

class EditUsernameDialogFragment : DialogFragment() {
    private var _binding: DialogEditTextBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BuddyViewModel by lazy {
        ViewModelProvider(requireActivity()).get(BuddyViewModel::class.java)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        @SuppressLint("InflateParams")
        _binding = DialogEditTextBinding.inflate(LayoutInflater.from(context))

        val builder = AlertDialog.Builder(requireContext(), R.style.Theme_bgglight_Dialog_Alert)
                .setTitle(R.string.title_add_username)
                .setView(binding.root)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok) { _, _ ->
                    viewModel.addUsernameToPlayer(binding.editText.text.trim().toString())
                }

        return builder.create().apply {
            requestFocus(binding.editText)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.editText.inputType = binding.editText.inputType or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance(): EditUsernameDialogFragment {
            return EditUsernameDialogFragment()
        }
    }
}
