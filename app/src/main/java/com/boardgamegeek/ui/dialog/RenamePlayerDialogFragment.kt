package com.boardgamegeek.ui.dialog

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.ui.viewmodel.BuddyViewModel
import kotlinx.android.synthetic.main.dialog_edit_text.*

class RenamePlayerDialogFragment : AbstractEditTextDialogFragment() {
    private val viewModel by activityViewModels<BuddyViewModel>()

    override val titleResId = R.string.title_edit_player

    override val hintResId = R.string.player_name

    override val originalText
        get() = arguments?.getString(KEY_TEXT)

    override fun onPositiveButton() {
        val text = editText?.text?.trim().toString()
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