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
        layout = LayoutInflater.from(context).inflate(R.layout.dialog_edit_nickname, null)

        return AlertDialog.Builder(requireContext(), R.style.Theme_bgglight_Dialog_Alert)
                .setView(layout)
                .setTitle(R.string.title_edit_nickname)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok) { _, _ ->
                    viewModel.updateNickName(nicknameView.text.trim().toString(), changePlaysCheckBox.isChecked)
                }
                .create().apply {
                    requestFocus(nicknameView)
                }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            nicknameView.setAndSelectExistingText(arguments?.getString("NICKNAME"))
            nicknameView.inputType = nicknameView.inputType or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        }
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
