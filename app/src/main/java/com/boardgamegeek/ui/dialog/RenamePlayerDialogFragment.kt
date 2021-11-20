package com.boardgamegeek.ui.dialog

import androidx.core.os.bundleOf
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

        fun newInstance(text: String?): RenamePlayerDialogFragment {
            return RenamePlayerDialogFragment().apply {
                arguments = bundleOf(KEY_TEXT to text)
            }
        }
    }
}
