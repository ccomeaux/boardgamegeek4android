package com.boardgamegeek.ui.dialog

import android.app.Dialog
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
    private var root: ViewGroup? = null
    private var listener: UpdateBuddyNicknameDialogListener? = null
    private var nickname: String? = null

    interface UpdateBuddyNicknameDialogListener {
        fun onFinishEditDialog(newNickname: String, shouldUpdatePlays: Boolean)
    }

    private fun initialize(
            root: ViewGroup?,
            listener: UpdateBuddyNicknameDialogListener) {
        this.root = root
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        layout = LayoutInflater.from(ctx).inflate(R.layout.dialog_edit_nickname, root, false)

        return AlertDialog.Builder(ctx, R.style.Theme_bgglight_Dialog_Alert)
                .setView(layout)
                .setTitle(R.string.title_edit_nickname)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok) { _, _ ->
                    listener?.onFinishEditDialog(nicknameView.text.trim().toString(), changePlaysCheckBox.isChecked)
                }
                .create().apply {
                    requestFocus(nicknameView)
                }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nicknameView.setAndSelectExistingText(nickname)
        nicknameView.inputType = nicknameView.inputType or InputType.TYPE_TEXT_FLAG_CAP_WORDS
    }

    fun setNickname(nickname: String) {
        this.nickname = nickname
    }

    companion object {
        @JvmStatic
        fun newInstance(
                root: ViewGroup?,
                listener: UpdateBuddyNicknameDialogListener
        ): UpdateBuddyNicknameDialogFragment {
            val fragment = UpdateBuddyNicknameDialogFragment()
            fragment.initialize(root, listener)
            return fragment
        }
    }
}
