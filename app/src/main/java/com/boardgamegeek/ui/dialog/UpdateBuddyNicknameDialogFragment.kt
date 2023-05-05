package com.boardgamegeek.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.DialogEditNicknameBinding
import com.boardgamegeek.extensions.createThemedBuilder
import com.boardgamegeek.extensions.requestFocus
import com.boardgamegeek.extensions.setAndSelectExistingText
import com.boardgamegeek.ui.viewmodel.BuddyViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UpdateBuddyNicknameDialogFragment : DialogFragment() {
    private var _binding: DialogEditNicknameBinding? = null
    private val binding get() = _binding!!

    private val viewModel by activityViewModels<BuddyViewModel>()

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)
        _binding = DialogEditNicknameBinding.inflate(layoutInflater)

        return requireContext().createThemedBuilder()
            .setView(binding.root)
            .setTitle(R.string.title_edit_nickname)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { _, _ ->
                val nickname = binding.nicknameView.text?.toString()
                viewModel.updateNickName(nickname?.trim().orEmpty(), binding.changePlaysCheckBox.isChecked)
            }
            .create().apply {
                requestFocus(binding.nicknameView)
            }
    }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            binding.nicknameView.setAndSelectExistingText(arguments?.getString(KEY_NICKNAME))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val KEY_NICKNAME = "NICKNAME"

        fun newInstance(nickname: String?) = UpdateBuddyNicknameDialogFragment().apply {
            arguments = bundleOf(KEY_NICKNAME to nickname)
        }
    }
}
