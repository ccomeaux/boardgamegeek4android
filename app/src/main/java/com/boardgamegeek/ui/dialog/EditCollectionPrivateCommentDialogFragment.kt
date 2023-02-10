package com.boardgamegeek.ui.dialog

import androidx.fragment.app.FragmentManager
import com.boardgamegeek.R
import com.boardgamegeek.extensions.TAG
import com.boardgamegeek.ui.viewmodel.GameCollectionItemViewModel

class EditCollectionPrivateCommentDialogFragment : EditCollectionTextDialogFragment() {
    override val titleResId = R.string.private_comment

    override fun updateText(viewModel: GameCollectionItemViewModel, text: String) {
        viewModel.updatePrivateComment(text, originalText)
    }

    companion object {
        fun show(fragmentManager: FragmentManager, text: String) {
            EditCollectionPrivateCommentDialogFragment().apply {
                arguments = createBundle(text)
            }.show(fragmentManager, EditCollectionPrivateCommentDialogFragment.TAG)
        }
    }
}
