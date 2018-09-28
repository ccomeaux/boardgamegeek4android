package com.boardgamegeek.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.boardgamegeek.R
import com.boardgamegeek.extensions.requestFocus
import com.boardgamegeek.extensions.setAndSelectExistingText
import kotlinx.android.synthetic.main.dialog_edit_nickname.*
import org.jetbrains.anko.support.v4.ctx

class UpdateBuddyNicknameDialogFragment : DialogFragment() {
    lateinit var layout: View
    private var listener: UpdateBuddyNicknameDialogListener? = null

    interface UpdateBuddyNicknameDialogListener {
        fun buddyNicknameUpdated(newNickname: String, shouldUpdatePlays: Boolean)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        listener = context as? UpdateBuddyNicknameDialogListener
        if (listener == null) throw ClassCastException("$context must implement UpdateBuddyNicknameDialogListener")
    }

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)
        layout = LayoutInflater.from(ctx).inflate(R.layout.dialog_edit_nickname, null)

        return AlertDialog.Builder(ctx, R.style.Theme_bgglight_Dialog_Alert)
                .setView(layout)
                .setTitle(R.string.title_edit_nickname)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok) { _, _ ->
                    listener?.buddyNicknameUpdated(nicknameView.text.trim().toString(), changePlaysCheckBox.isChecked)
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
