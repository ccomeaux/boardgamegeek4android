package com.boardgamegeek.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.extensions.requestFocus
import com.boardgamegeek.extensions.setAndSelectExistingText
import com.boardgamegeek.ui.viewmodel.BuddyViewModel
import kotlinx.android.synthetic.main.dialog_edit_nickname.*

class UpdateBuddyNicknameDialogFragment : DialogFragment() {
    lateinit var layout: View

    private val viewModel by activityViewModels<BuddyViewModel>()

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)
        layout = layoutInflater.inflate(R.layout.dialog_edit_nickname, null)

        return AlertDialog.Builder(requireContext(), R.style.Theme_bgglight_Dialog_Alert)
            .setView(layout)
            .setTitle(R.string.title_edit_nickname)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { _, _ ->
                val nickname = nicknameView.text?.toString()
                viewModel.updateNickName(nickname?.trim().orEmpty(), changePlaysCheckBox.isChecked)
            }
            .create().apply {
                requestFocus(nicknameView)
            }
    }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            nicknameView.setAndSelectExistingText(arguments?.getString(KEY_NICKNAME))
        }
    }

    companion object {
        private const val KEY_NICKNAME = "NICKNAME"

        fun newInstance(nickname: String?): UpdateBuddyNicknameDialogFragment {
            return UpdateBuddyNicknameDialogFragment().apply {
                arguments = bundleOf(KEY_NICKNAME to nickname)
            }
        }
    }
}
