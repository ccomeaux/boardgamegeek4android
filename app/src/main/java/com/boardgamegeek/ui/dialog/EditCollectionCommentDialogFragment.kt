package com.boardgamegeek.ui.dialog

import androidx.fragment.app.FragmentManager
import com.boardgamegeek.R
import com.boardgamegeek.extensions.TAG
import com.boardgamegeek.ui.viewmodel.GameCollectionItemViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditCollectionCommentDialogFragment : EditCollectionTextDialogFragment() {
    override val titleResId = R.string.comment

    override fun updateText(viewModel: GameCollectionItemViewModel, text: String) {
        viewModel.updateComment(text, originalText)
    }

    companion object {
        fun show(fragmentManager: FragmentManager, text: String) {
            EditCollectionCommentDialogFragment().apply {
                arguments = createBundle(text)
            }.show(fragmentManager, EditCollectionCommentDialogFragment.TAG)
        }
    }
}
