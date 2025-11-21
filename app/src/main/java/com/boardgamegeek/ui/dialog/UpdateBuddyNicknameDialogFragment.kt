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
import com.boardgamegeek.databinding.DialogEditNicknameBinding
import com.boardgamegeek.extensions.requestFocus
import com.boardgamegeek.extensions.setAndSelectExistingText
import com.boardgamegeek.ui.viewmodel.BuddyViewModel
import org.jetbrains.anko.support.v4.act

class UpdateBuddyNicknameDialogFragment : DialogFragment() {
    private var _binding: DialogEditNicknameBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BuddyViewModel by lazy {
        ViewModelProvider(this).get(BuddyViewModel::class.java)
    }

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)
        _binding = DialogEditNicknameBinding.inflate(LayoutInflater.from(context))

        return AlertDialog.Builder(requireContext(), R.style.Theme_bgglight_Dialog_Alert)
                .setView(binding.root)
                .setTitle(R.string.title_edit_nickname)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok) { _, _ ->
                    viewModel.updateNickName(binding.nicknameView.text.trim().toString(), binding.changePlaysCheckBox.isChecked)
                }
                .create().apply {
                    requestFocus(binding.nicknameView)
                }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            binding.nicknameView.setAndSelectExistingText(arguments?.getString("NICKNAME"))
            binding.nicknameView.inputType = binding.nicknameView.inputType or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance(nickname: String): UpdateBuddyNicknameDialogFragment {
            val fragment = UpdateBuddyNicknameDialogFragment()
            fragment.arguments = Bundle().apply {
                putString("NICKNAME", nickname)
            }
            return fragment
        }
    }
}
