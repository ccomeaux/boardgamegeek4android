package com.boardgamegeek.ui.dialog

import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import com.boardgamegeek.R
import com.boardgamegeek.ui.viewmodel.BuddyViewModel
import kotlinx.android.synthetic.main.dialog_edit_text.*
import org.jetbrains.anko.support.v4.act

class RenamePlayerDialogFragment : AbstractEditTextDialogFragment() {
    private val viewModel: BuddyViewModel by lazy {
        ViewModelProviders.of(act).get(BuddyViewModel::class.java)
    }

    override val titleResId
        get() = R.string.title_edit_player

    override val originalText
        get() = arguments?.getString(KEY_TEXT)

    override fun onPositiveButton() {
        val text = editText.text.trim().toString()
        viewModel.renamePlayer(text)
    }

    companion object {
        private const val KEY_TEXT = "text"

        @JvmStatic
        fun newInstance(text: String?): RenamePlayerDialogFragment {
            return RenamePlayerDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(KEY_TEXT, text)
                }
            }
        }
    }
}